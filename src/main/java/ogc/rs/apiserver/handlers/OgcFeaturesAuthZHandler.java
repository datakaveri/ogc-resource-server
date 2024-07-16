package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.apiserver.util.Constants.HEADER_TOKEN;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.UUID_REGEX;

public class OgcFeaturesAuthZHandler implements Handler<RoutingContext> {

  Vertx vertx;

  private final DatabaseService databaseService;

  private static final Logger LOGGER = LogManager.getLogger(OgcFeaturesAuthZHandler.class);

  public OgcFeaturesAuthZHandler(Vertx vertx) {
    this.vertx = vertx;
    this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
  }

  /**
   * Handles the routing context to authorize access to OGC Features APIs.
   *
   * @param routingContext the routing context of the request
   */
  @Override
  public void handle(RoutingContext routingContext) {
    
    if(System.getProperty("disable.auth") != null) {
      routingContext.next();
      return;
    }
    
    LOGGER.debug("OGC Features Authorization");
    String id = routingContext.normalizedPath().split("/")[2];
    String token = routingContext.request().getHeader("token");
    JsonObject authInfo = new JsonObject().put(HEADER_TOKEN, token).put("id", id);
    if (id == null || !id.matches(UUID_REGEX)) {
      routingContext.fail(new OgcException(404, "Not Found", "Collection Not Found"));
      return;
    }
    AuthInfo user = routingContext.get(USER_KEY);
    UUID iid = user.getResourceId();
    if (!user.isRsToken() && !id.equals(iid.toString())) {
      LOGGER.error("Resource Ids don't match! id- {}, jwtId- {}", id, iid);
      routingContext.fail(
          new OgcException(
              401, "Not Authorized", "User is not authorised. Please contact IUDX AAA "));
      return;
    }
    databaseService
        .getAccess(id)
        .onSuccess(
            isOpen -> {
              if (isOpen && user.isRsToken()) {
                authorizeUser(routingContext, authInfo, user);
              } else {
                if (user.getRole() == AuthInfo.RoleEnum.consumer) {
                  handleConsumerAccess(routingContext, authInfo, user);
                } else {
                  authorizeUser(routingContext, authInfo, user);
                }
              }
            })
        .onFailure(
            fail -> {
              LOGGER.error("Collection not present in table: {}", fail.getMessage());
              routingContext.fail(fail);
            });
  }

  private void authorizeUser(RoutingContext routingContext, JsonObject authInfo, AuthInfo user) {
    authInfo
        .put("iid", user.getResourceId().toString())
        .put("userId", user.getUserId().toString())
        .put("role", user.getRole().toString());
    routingContext.data().put("authInfo", authInfo);
    LOGGER.debug("Authorization info: {}", routingContext.data().values());
    routingContext.next();
  }

  private void handleConsumerAccess(
      RoutingContext routingContext, JsonObject authInfo, AuthInfo user) {
    JsonArray access =
        user.getConstraints() != null ? user.getConstraints().getJsonArray("access") : null;
    if (access == null || !access.contains("api")) {
      LOGGER.debug("invalid constraints value");
      routingContext.fail(
          new OgcException(
              401, "Not Authorized", "User is not authorised. Please contact IUDX AAA "));

    } else {
      authorizeUser(routingContext, authInfo, user);
    }
  }
}
