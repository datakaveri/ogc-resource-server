package ogc.rs.apiserver.authentication.handler;

import static ogc.rs.apiserver.util.Constants.NOT_AUTHORIZED;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.Base64;
import ogc.rs.apiserver.authentication.client.JwksResolver;
import ogc.rs.apiserver.authentication.util.BearerTokenExtractor;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiIssuerJwtAuthHandler implements AuthenticationHandler {
  private static final Logger LOGGER = LogManager.getLogger(MultiIssuerJwtAuthHandler.class);

  private final JwksResolver jwksResolver;

  public MultiIssuerJwtAuthHandler(JwksResolver resolver) {
    this.jwksResolver = resolver;
  }

  private static String extractIssuer(String token) {
    String[] parts = token.split("\\.");
    if (parts.length < 2) throw new IllegalArgumentException("Malformed JWT");
    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
    return new JsonObject(payload).getString("iss");
  }

  private static String extractKid(String token) {
    String[] parts = token.split("\\.");
    if (parts.length < 2) throw new IllegalArgumentException("Malformed JWT");
    String header = new String(Base64.getUrlDecoder().decode(parts[0]));
    return new JsonObject(header).getString("kid");
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
    String kid;
    try {
      issuer = extractIssuer(token);
      kid = extractKid(token);
    } catch (Exception e) {
      LOGGER.error("Failed to extract token claims: {}", e.getMessage());
      ctx.fail(new OgcException(401, NOT_AUTHORIZED, "Invalid token format"));
      return;
    }

    jwksResolver
        .resolve(issuer, kid)
        .compose(jwtAuth -> jwtAuth.authenticate(new TokenCredentials(token)))
        .onSuccess(
            user -> {
              LOGGER.debug("Authentication successful for issuer: {}, kid: {}", issuer, kid);
              ctx.setUser(user);
              ctx.next();
            })
        .onFailure(
            err -> {
              LOGGER.error(
                  "Authentication failed for issuer {}, kid {}: {}", issuer, kid, err.getMessage());
              ctx.fail(new OgcException(401, NOT_AUTHORIZED, "Invalid token format"));
            });
  }
}
