package ogc.rs.apiserver.authentication.handler;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.List;
import java.util.stream.Collectors;
import ogc.rs.apiserver.authentication.util.BearerTokenExtractor;
import ogc.rs.apiserver.authentication.util.TokenIssuer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AAAJwtAuthHandler implements AuthenticationHandler {
  private static final Logger LOGGER = LogManager.getLogger(AAAJwtAuthHandler.class);
  private JWTAuth jwtAuth;
  private final String certUrl;
  private final WebClient client;

  public AAAJwtAuthHandler(String certUrl, String issuer, Vertx vertx) {
    this.certUrl = certUrl;
    this.client =
        WebClient.create(
            vertx, new WebClientOptions().setSsl(certUrl.startsWith("https")).setTrustAll(true));
    refresh(vertx, new JsonObject().put("issuer", issuer).put("jwtIgnoreExpiry", false))
        .onSuccess(handler -> {
          this.jwtAuth = handler;
        }).onFailure(err -> {
          LOGGER.error("Failed to initialize JWTAuth: {}", err.getMessage());
          err.printStackTrace();
        });
  }

  @Override
  public void handle(RoutingContext ctx) {
    LOGGER.debug("Handling authentication for AAA JWT");
    String token = BearerTokenExtractor.extract(ctx);
    if (token == null || token.isBlank()) {
      LOGGER.warn("Missing or invalid Authorization header");
      ctx.put("auth_error", "Missing Bearer token in Authorization header");
      ctx.put("auth_failed", true);
      return;
    }

    jwtAuth.authenticate(new JsonObject().put("token", token))
        .onComplete(ar -> {
          if (ar.succeeded()) {
            LOGGER.debug("Authentication successful for AAA JWT");
            ctx.setUser(ar.result());
            ctx.put("auth_failed", false);
            ctx.next();
          } else {
            LOGGER.warn("Auth failed: {}", ar.cause().getMessage());
            // do NOT call ctx.fail(ar.cause());
            ctx.put("auth_failed", true);
            ctx.put("auth_error", ar.cause().getMessage());
          }
        });
  }

  public Future<JsonObject> fetchJwkKeys() {
    LOGGER.info("Fetching JWKs from {}", certUrl);
    return client
        .requestAbs(HttpMethod.GET, certUrl)
        .send()
        .compose(
            resp -> {
              if (resp.statusCode() == 200 && resp.bodyAsJsonObject().containsKey("keys")) {
                return Future.succeededFuture(resp.bodyAsJsonObject());
              } else {
                return Future.failedFuture("Invalid JWKs response: " + resp.statusCode());
              }
            })
        .recover(
            err -> {
              LOGGER.error("Failed to fetch JWKs: {}", err.getMessage());
              err.printStackTrace();
              return Future.failedFuture(err);
            });
  }

  private Future<JWTAuth> refresh(Vertx vertx, JsonObject config) {
    return this.fetchJwkKeys().compose(jwk -> {
      List<JsonObject> keys = jwk.getJsonArray("keys").stream()
          .map(obj -> (JsonObject) obj)
          .collect(Collectors.toList());
      String iss = config.getString("issuer");

      LOGGER.debug("Using issuer: {}, {}", TokenIssuer.CONTROL_PANE, iss);
      JWTAuthOptions options = new JWTAuthOptions()
          .setJwks(keys)
          .setJWTOptions(new JWTOptions()
              .setLeeway(30)
              .setIgnoreExpiration(config.getBoolean("jwtIgnoreExpiry", false))
              .setIssuer(iss));
      //TODO need to set aud as well
      //        .setAudience(List.of(config.getString("aud"))));

      jwtAuth = JWTAuth.create(vertx, options);
      LOGGER.info("JWTAuth initialized/refreshed successfully.");
      return Future.succeededFuture(jwtAuth);
    });
  }
}
