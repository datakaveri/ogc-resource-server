package ogc.rs.apiserver.authentication.handler;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;

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
import ogc.rs.apiserver.util.AuthInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KeycloakJwtAuthHandler implements AuthenticationHandler {
  private static final Logger LOGGER = LogManager.getLogger(KeycloakJwtAuthHandler.class);
  private JWTAuth jwtAuth;
  private final String certUrl;
  private final WebClient client;

  public KeycloakJwtAuthHandler(String certUrl, String issuer, Vertx vertx) {
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

      LOGGER.debug("Using issuer: {}, {}", TokenIssuer.KEYCLOAK, iss);
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

  @Override
  public void handle(RoutingContext ctx) {
    LOGGER.debug("Handling authentication for Keycloak JWT");
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
            LOGGER.debug("auth successful for Keycloak JWT");
            /* Creating auth info here and setting it in the routing context*/
            ctx.put(USER_KEY, AuthInfo.map(ar.result().attributes().getJsonObject("accessToken")));
            ctx.setUser(ar.result());
            ctx.put("auth_failed", false);
            ctx.next();
          } else {
            LOGGER.warn("Auth failed: {}", ar.cause().getMessage());
            ctx.put("auth_error", ar.cause().getMessage());
            ctx.put("auth_failed", true);
          }
        });
  }

}