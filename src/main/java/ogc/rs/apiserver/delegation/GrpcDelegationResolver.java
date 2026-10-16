package ogc.rs.apiserver.delegation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import ogc.rs.apiserver.delegation.grpc.AppIdVerificationServiceGrpc;
import ogc.rs.apiserver.delegation.grpc.ResolveDelegationRequest;
import ogc.rs.apiserver.delegation.grpc.ResolveDelegationResponse;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Calls dx-controlplane's {@code ResolveDelegation} gRPC (same RPC dx-dataplane-rs uses via
 * dx-common). Controlplane checks the delegation is active, not expired and the delegator is
 * enabled, and returns the delegator with the delegation's capped scopes. Results are cached per
 * (delegator, delegatee) for {@value #CACHE_TTL_MINUTES} minutes, as in dx-common.
 *
 * <p>Plaintext channel; TLS is handled at the service-mesh level.
 */
public class GrpcDelegationResolver implements DelegationResolver {

  private static final Logger LOGGER = LogManager.getLogger(GrpcDelegationResolver.class);
  private static final int CACHE_MAX_SIZE = 1000;
  private static final int CACHE_TTL_MINUTES = 2;
  private static final Metadata.Key<String> AUTHORIZATION_KEY =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  private final ManagedChannel channel;
  private final AppIdVerificationServiceGrpc.AppIdVerificationServiceStub asyncStub;
  private final KeycloakServiceTokenProvider tokenProvider;
  private final Cache<String, JsonObject> delegatorCache =
      CacheBuilder.newBuilder()
          .maximumSize(CACHE_MAX_SIZE)
          .expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
          .build();

  public GrpcDelegationResolver(
      String host, int port, KeycloakServiceTokenProvider tokenProvider) {
    InetAddress resolved;
    try {
      // Docker Swarm DNS names can contain underscores which java.net.URI rejects.
      resolved = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Cannot resolve controlplane gRPC host: " + host, e);
    }
    this.channel =
        NettyChannelBuilder.forAddress(new InetSocketAddress(resolved, port))
            .usePlaintext()
            .keepAliveTime(30, TimeUnit.SECONDS)
            .build();
    this.asyncStub = AppIdVerificationServiceGrpc.newStub(channel);
    this.tokenProvider = tokenProvider;
  }

  @Override
  public Future<User> resolve(String delegatorSub, User delegatee) {
    String delegateeSub = delegatee.principal().getString("sub");
    String cacheKey = delegatorSub + ":" + delegateeSub;

    JsonObject cached = delegatorCache.getIfPresent(cacheKey);
    if (cached != null) {
      return Future.succeededFuture(toUser(cached, delegatee));
    }

    return tokenProvider
        .getServiceToken()
        .compose(token -> call(delegatorSub, delegateeSub, token))
        .compose(
            response -> {
              if (!response.getSuccess()) {
                LOGGER.debug(
                    "ResolveDelegation rejected delegatorSub={} delegateeSub={}: {}",
                    delegatorSub, delegateeSub, response.getErrorCode());
                return Future.failedFuture(
                    new OgcException(403, "Forbidden", "No active delegation"));
              }
              JsonObject delegator = toPrincipal(response);
              delegatorCache.put(cacheKey, delegator);
              return Future.succeededFuture(toUser(delegator, delegatee));
            });
  }

  private Future<ResolveDelegationResponse> call(
      String delegatorSub, String delegateeSub, String serviceToken) {
    Promise<ResolveDelegationResponse> promise = Promise.promise();
    asyncStub
        .withCallCredentials(bearer(serviceToken))
        .resolveDelegation(
            ResolveDelegationRequest.newBuilder()
                .setDelegatorSub(delegatorSub)
                .setDelegateeSub(delegateeSub)
                .build(),
            new StreamObserver<ResolveDelegationResponse>() {
              @Override
              public void onNext(ResolveDelegationResponse response) {
                promise.complete(response);
              }

              @Override
              public void onError(Throwable t) {
                LOGGER.error(
                    "gRPC ResolveDelegation failed delegatorSub={} delegateeSub={}: {}",
                    delegatorSub, delegateeSub, t.getMessage());
                promise.fail(t);
              }

              @Override
              public void onCompleted() {}
            });
    return promise.future();
  }

  /** Delegator principal in the same claim shape a Keycloak JWT gives OGC's handlers. */
  static JsonObject toPrincipal(ResolveDelegationResponse r) {
    return new JsonObject()
        .put("sub", r.getSub())
        .put("organisation_id", r.getOrganisationId().isBlank() ? null : r.getOrganisationId())
        .put("organisation_name", r.getOrganisationName().isBlank() ? null : r.getOrganisationName())
        .put("realm_access", new JsonObject().put("roles", new JsonArray(r.getRolesList())))
        .put("scopes", new JsonArray(r.getScopesList()))
        .put("email_verified", r.getEmailVerified())
        .put("kyc_verified", r.getKycVerified())
        .put("name", r.getName())
        .put("preferred_username", r.getPreferredUsername())
        .put("given_name", r.getGivenName())
        .put("family_name", r.getFamilyName())
        .put("email", r.getEmail());
  }

  /**
   * New user for the delegator. Keeps the caller's {@code iss}/{@code exp} (the request is still
   * bounded by the caller's token) and records the caller as {@code delegatee_sub}.
   */
  static User toUser(JsonObject delegator, User delegatee) {
    Long exp = delegatee.attributes().getLong("exp", delegatee.principal().getLong("exp"));
    JsonObject principal =
        delegator
            .copy()
            .put("delegatee_sub", delegatee.principal().getString("sub"))
            .put("iss", delegatee.principal().getString("iss"))
            .put("exp", exp);
    // only exp is carried over: the caller's rootClaim/accessToken attributes would make
    // User.subject() return the delegatee instead of the delegator
    return User.create(principal, new JsonObject().put("exp", exp));
  }

  private static CallCredentials bearer(String token) {
    return new CallCredentials() {
      @Override
      public void applyRequestMetadata(
          RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION_KEY, "Bearer " + token);
        applier.apply(headers);
      }
    };
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }
}
