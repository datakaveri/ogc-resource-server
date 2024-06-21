package ogc.rs.apiserver.handlers;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.AuthInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

import static ogc.rs.apiserver.util.Constants.HEADER_TOKEN;
import static ogc.rs.apiserver.util.Constants.AUTH_CERTIFICATE_PATH;

public class DxTokenAuthenticationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(DxTokenAuthenticationHandler.class);
  public static final String USER_KEY = "userKey";

  Vertx vertx;
  private WebClient webClient;
  private static JWTAuth jwtAuth;

  static WebClient createWebClient(Vertx vertx, JsonObject config) {
    return createWebClient(vertx, config, false);
  }

  static WebClient createWebClient(Vertx vertxObj, JsonObject config, boolean testing) {
    WebClientOptions webClientOptions = new WebClientOptions();
    if (testing) {
      webClientOptions.setTrustAll(true).setVerifyHost(false);
    }
    webClientOptions.setSsl(true);
    return WebClient.create(vertxObj, webClientOptions);
  }

  public DxTokenAuthenticationHandler(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    getJwtPublicKey(vertx, config)
        .onSuccess(
            handler -> {
              String cert = handler;

              JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
              jwtAuthOptions.addPubSecKey(
                  new PubSecKeyOptions().setAlgorithm("ES256").setBuffer(cert));
              /*
               * Default jwtIgnoreExpiry is false. If set through config, then that value is taken
               */
              boolean jwtIgnoreExpiry =
                  config.getBoolean("jwtIgnoreExpiry") != null
                      && config.getBoolean("jwtIgnoreExpiry");
              if (jwtIgnoreExpiry) {
                jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
                LOGGER.warn(
                    "JWT ignore expiration set to true, "
                        + "do not set IgnoreExpiration in production!!");
              }
              jwtAuthOptions
                  .getJWTOptions()
                  .setAudience(Collections.singletonList(config.getString("audience")));
              jwtAuthOptions.getJWTOptions().setIssuer(config.getString("issuer"));
              jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
            })
        .onFailure(
            handler -> {
              LOGGER.error("failed to get JWT public key from auth server");
              LOGGER.error("Authentication verticle deployment failed.");
            });
  }

  private Future<String> getJwtPublicKey(Vertx vertx, JsonObject config) {
    Promise<String> promise = Promise.promise();
    if (System.getProperty("fake-token") != null) {
      LOGGER.fatal("processing fake token property");
      String authCert = "src/test/resources/public.pem";
      FileSystem fileSystem = vertx.fileSystem();
      Buffer buffer = fileSystem.readFileBlocking(authCert);
      promise.complete(buffer.toString());
    } else {
      webClient = createWebClient(vertx, config);
      String authCert = config.getString("dxAuthBasePath") + AUTH_CERTIFICATE_PATH;
      webClient
          .get(443, config.getString("authServerHost"), authCert)
          .send(
              handler -> {
                if (handler.succeeded()) {
                  JsonObject json = handler.result().bodyAsJsonObject();
                  promise.complete(json.getString("cert"));
                } else {
                  promise.fail("fail to get JWT public key");
                }
              });
    }
    return promise.future();
  }

  @Override
  public void handle(RoutingContext routingContext) {
    String token = routingContext.request().headers().get(HEADER_TOKEN);
    if (token == null) {
      routingContext.fail(new OgcException(401, "Token not found", "Token not found"));
      return;
    }
    jwtAuth.authenticate(
        new JsonObject().put("token", token),
        res -> {
          if (res.succeeded()) {
            LOGGER.debug("Authentication Successful");
            JsonObject tokenDetails = res.result().principal();
            AuthInfo user = AuthInfo.createUser(tokenDetails);
            routingContext.put(USER_KEY, user);
            LOGGER.debug("the user key: " + routingContext.get(USER_KEY).toString());
            routingContext.next();
          } else {
            LOGGER.debug("Authentication not successful" + res.cause());
            routingContext.fail(new OgcException(401, "Invalid Token", res.cause().getMessage()));
          }
        });
  }
}
