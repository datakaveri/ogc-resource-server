package ogc.rs.apiserver.authentication.handler;

import static ogc.rs.apiserver.util.Constants.NOT_AUTHORIZED;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ogc.rs.apiserver.authentication.client.JwksResolver;
import ogc.rs.apiserver.authentication.util.BearerTokenExtractor;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiIssuerJwtAuthHandler implements AuthenticationHandler {
  private static final Logger LOGGER = LogManager.getLogger(MultiIssuerJwtAuthHandler.class);

  private final Map<String, JWTAuth> authProviders;
  private final JwksResolver jwksResolver;

  public MultiIssuerJwtAuthHandler(JwksResolver resolver) {
    this.jwksResolver = resolver;
    this.authProviders = new ConcurrentHashMap<>();
  }

  private static String extractIssuer(String token) {
    String[] parts = token.split("\\.");
    if (parts.length < 2) throw new IllegalArgumentException("Malformed JWT");
    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
    return new JsonObject(payload).getString("iss");
  }

  @Override
  public void handle(RoutingContext ctx) {
    String token = BearerTokenExtractor.extract(ctx);
    RoutingContextHelper.addToken(ctx, token);
    if (token == null || token.isBlank()) {
      LOGGER.warn("Missing or invalid Authorization header");
      ctx.fail(new OgcException(401, NOT_AUTHORIZED, "Missing bearer token"));
      return;
    }

    String issuer;
    try {
      issuer = extractIssuer(token);
    } catch (Exception e) {
      LOGGER.error("Failed to extract issuer: {}", e.getMessage());
      ctx.fail(new OgcException(401, NOT_AUTHORIZED, "Invalid token format"));
      return;
    }

    getOrCreateAuth(issuer)
        .compose(jwtAuth -> jwtAuth.authenticate(new JsonObject().put("token", token)))
        .onSuccess(
            user -> {
              LOGGER.debug("Authentication successful for issuer: {}", issuer);
              ctx.setUser(user);
              ctx.next();
            })
        .onFailure(
            err -> {
              LOGGER.error("Authentication failed for issuer {}: {}", issuer, err.getMessage());
              ctx.fail(new OgcException(401, NOT_AUTHORIZED, "Invalid token format"));
            });
  }

  private Future<JWTAuth> getOrCreateAuth(String issuer) {
    LOGGER.debug("Looking up JWTAuth for issuer: " + issuer);
    if (authProviders.containsKey(issuer)) {
      return Future.succeededFuture(authProviders.get(issuer));
    }
    return jwksResolver
        .resolve(issuer)
        .map(
            jwtAuth -> {
              System.out.println("Cached JWTAuth for issuer: " + issuer);
              authProviders.put(issuer, jwtAuth);
              return jwtAuth;
            });
  }
}