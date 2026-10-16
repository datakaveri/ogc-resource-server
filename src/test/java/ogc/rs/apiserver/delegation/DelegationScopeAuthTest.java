package ogc.rs.apiserver.delegation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import ogc.rs.apiserver.authentication.client.JwksResolver;
import ogc.rs.apiserver.authentication.handler.MultiIssuerJwtAuthHandler;
import ogc.rs.apiserver.authentication.util.DxUser;
import ogc.rs.apiserver.authorization.handler.AuthorizationHandler;
import ogc.rs.apiserver.authorization.model.Scopes;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.delegation.grpc.ResolveDelegationResponse;
import ogc.rs.apiserver.handlers.FailureHandler;
import ogc.rs.apiserver.util.OgcException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * OGC auth chain with a {@code did} header: MultiIssuerJwtAuthHandler (real JWT verification +
 * role→scope enrichment) → delegation → AuthorizationHandler.forScopes → RoutingContextHelper.
 * Only the gRPC call is stubbed; the response→user mapping is GrpcDelegationResolver's own.
 */
@ExtendWith(VertxExtension.class)
class DelegationScopeAuthTest {

  private static final String ISSUER = "test-issuer";
  private static final String DELEGATEE = "11111111-1111-1111-1111-111111111111";
  private static final String DELEGATOR = "22222222-2222-2222-2222-222222222222";

  private static Vertx vertx;
  private static WebClient client;
  private static JWTAuth jwtAuth;
  private static int port;

  @BeforeAll
  static void setUp(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    client = WebClient.create(vertx);
    jwtAuth =
        JWTAuth.create(
            vertx,
            new JWTAuthOptions()
                .addPubSecKey(
                    new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("test-secret-test-secret"))
                .setJWTOptions(new JWTOptions().setIssuer(ISSUER)));

    JwksResolver jwks =
        new JwksResolver(vertx, new JsonObject()) {
          @Override
          public Future<JWTAuth> resolve(String issuer) {
            return Future.succeededFuture(jwtAuth);
          }
        };

    // stands in for the gRPC call; controlplane returns the delegator with capped scopes
    DelegationResolver delegations =
        (did, delegatee) -> {
          if (!DELEGATOR.equals(did)) {
            return Future.failedFuture(new OgcException(403, "Forbidden", "No active delegation"));
          }
          ResolveDelegationResponse response =
              ResolveDelegationResponse.newBuilder()
                  .setSuccess(true)
                  .setSub(DELEGATOR)
                  .addRoles("provider")
                  .addScopes(Scopes.OWN_ASSET_MANAGEMENT)
                  .setDelegateeSub(delegatee.principal().getString("sub"))
                  .build();
          return Future.succeededFuture(
              GrpcDelegationResolver.toUser(
                  GrpcDelegationResolver.toPrincipal(response), delegatee));
        };

    Router router = Router.router(vertx);
    router
        .get("/provider-only")
        .handler(new MultiIssuerJwtAuthHandler(jwks, delegations))
        .handler(AuthorizationHandler.forScopes(Scopes.OWN_ASSET_MANAGEMENT))
        .handler(
            ctx -> {
              DxUser user = RoutingContextHelper.fromPrincipal(ctx);
              ctx.json(
                  new JsonObject()
                      .put("sub", user.getSub().toString())
                      .put("subject", ctx.user().subject())
                      .put("scopes", new JsonArray(user.getScopes()))
                      .put("exp", user.getTokenExpiry())
                      .put("delegatee", ctx.user().principal().getString("delegatee_sub")));
            })
        .failureHandler(new FailureHandler());

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(0)
        .onComplete(
            testContext.succeeding(
                server -> {
                  port = server.actualPort();
                  testContext.completeNow();
                }));
  }

  @AfterAll
  static void tearDown(VertxTestContext testContext) {
    vertx.close().onComplete(testContext.succeedingThenComplete());
  }

  /** Consumer JWT for the delegatee, signed with the test key. */
  private static String consumerToken() {
    return jwtAuth.generateToken(
        new JsonObject()
            .put("sub", DELEGATEE)
            .put("realm_access", new JsonObject().put("roles", new JsonArray().add("consumer"))),
        new JWTOptions().setIssuer(ISSUER).setExpiresInMinutes(30));
  }

  private HttpRequest<Buffer> get() {
    return client.get(port, "localhost", "/provider-only");
  }

  @Test
  void noTokenIs401(VertxTestContext testContext) {
    get()
        .send()
        .onComplete(
            testContext.succeeding(
                resp ->
                    testContext.verify(
                        () -> {
                          assertEquals(401, resp.statusCode());
                          testContext.completeNow();
                        })));
  }

  @Test
  void consumerWithoutDelegationLacksProviderScope(VertxTestContext testContext) {
    get()
        .putHeader("Authorization", "Bearer " + consumerToken())
        .send()
        .onComplete(
            testContext.succeeding(
                resp ->
                    testContext.verify(
                        () -> {
                          assertEquals(401, resp.statusCode());
                          testContext.completeNow();
                        })));
  }

  @Test
  void delegationActsAsDelegatorWithDelegatedScopes(VertxTestContext testContext) {
    get()
        .putHeader("Authorization", "Bearer " + consumerToken())
        .putHeader("did", DELEGATOR)
        .send()
        .onComplete(
            testContext.succeeding(
                resp ->
                    testContext.verify(
                        () -> {
                          assertEquals(200, resp.statusCode(), resp.bodyAsString());
                          JsonObject body = resp.bodyAsJsonObject();
                          assertEquals(DELEGATOR, body.getString("sub"));
                          assertEquals(DELEGATOR, body.getString("subject"));
                          assertEquals(DELEGATEE, body.getString("delegatee"));
                          assertEquals(
                              new JsonArray().add(Scopes.OWN_ASSET_MANAGEMENT),
                              body.getJsonArray("scopes"));
                          // caller's token expiry is carried over to the delegator user
                          long now = System.currentTimeMillis() / 1000;
                          long exp = body.getLong("exp");
                          assertEquals(true, exp > now && exp <= now + 30 * 60 + 5);
                          testContext.completeNow();
                        })));
  }

  @Test
  void unknownDelegationIs403(VertxTestContext testContext) {
    get()
        .putHeader("Authorization", "Bearer " + consumerToken())
        .putHeader("did", "33333333-3333-3333-3333-333333333333")
        .send()
        .onComplete(
            testContext.succeeding(
                resp ->
                    testContext.verify(
                        () -> {
                          assertEquals(403, resp.statusCode());
                          testContext.completeNow();
                        })));
  }
}
