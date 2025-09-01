package ogc.rs.apiserver.handlers;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.apiserver.util.Constants.NOT_AUTHORIZED;
import static ogc.rs.apiserver.util.Constants.USER_NOT_AUTHORIZED;
import static ogc.rs.common.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import java.util.UUID;
import ogc.rs.apiserver.authentication.util.BearerTokenExtractor;
import ogc.rs.apiserver.authorization.CheckResourceAccess;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OgcFeaturesAuthZHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(OgcFeaturesAuthZHandler.class);
  private final DatabaseService databaseService;
  private final CheckResourceAccess resourceAccessHandler;
  Vertx vertx;

  public OgcFeaturesAuthZHandler(Vertx vertx, CheckResourceAccess resourceAccessHandler) {
    this.vertx = vertx;
    this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    this.resourceAccessHandler = resourceAccessHandler;
  }

  /**
   * Handles the routing context to authorize access to OGC Features APIs.
   *
   * @param routingContext the routing context of the request
   */
  @Override
  public void handle(RoutingContext routingContext) {

    if (System.getProperty("disable.auth") != null) {
      routingContext.next();
      return;
    }

    LOGGER.debug("OGC Features Authorization");
    String collectionId = routingContext.normalizedPath().split("/")[2];
    if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
      routingContext.fail(new OgcException(404, "Not Found", "Collection Not Found"));
      return;
    }
    AuthInfo user = routingContext.get(USER_KEY);
//    UUID iid = user.getResourceId();
//    TODO: Revisit this check
//    if (!user.isRsToken() && !collectionId.equals(iid.toString())) {
//      LOGGER.error("Resource Ids don't match! id- {}, jwtId- {}", collectionId, iid);
//      routingContext.fail(
//          new OgcException(
//              401, "Not Authorized", "User is not authorised. Please contact DX AAA "));
//      return;
//    }
    databaseService
        .getAccess(collectionId)
        .onSuccess(
            isOpen -> {

              user.setResourceId(UUID.fromString(collectionId));
              RoutingContextHelper.addCollectionId(routingContext, collectionId);

              if (isOpen && user.isRsToken()) {
                authorizeUser(routingContext);
              } else {
                if (user.getRole() == AuthInfo.RoleEnum.consumer) {
                  handleConsumerAccess(routingContext, user);
                } else {
                  authorizeUser(routingContext);
                }
              }
            })
        .onFailure(
            fail -> {
              LOGGER.error("Collection not present in table: {}", fail.getMessage());
              routingContext.fail(fail);
            });
  }

  private void authorizeUser(RoutingContext routingContext) {
    LOGGER.debug("Authorization info: {}", routingContext.data().values());
    routingContext.next();
  }

  private void handleConsumerAccess(
      RoutingContext routingContext, AuthInfo user) {
    checkResourceAccess(routingContext);
    JsonArray access =
        user.getConstraints() != null ? user.getConstraints().getJsonArray("access") : null;
    if (access == null || !access.contains("api")) {
      LOGGER.debug("invalid constraints value");
      routingContext.fail(
          new OgcException(
              401, "Not Authorized", "User is not authorised. Please contact DX AAA "));

    } else {
      authorizeUser(routingContext);
    }
  }


  private void checkResourceAccess(RoutingContext routingContext) {
    //Call the control panel to check if the user has access to the resource
    String token = BearerTokenExtractor.extract(routingContext);
    Future<Boolean> hasAccessFuture =
        resourceAccessHandler.checkAccess(RoutingContextHelper.getCollectionId(routingContext), token);

    hasAccessFuture.onSuccess(hasAccess -> {
      if (hasAccess) {
        LOGGER.debug("User has access to the resource.");

      } else {
        LOGGER.debug("User does not have access to the resource.");
        routingContext.fail(new OgcException(403, NOT_AUTHORIZED, USER_NOT_AUTHORIZED));
      }
    }).onFailure(err -> {
      LOGGER.error("Error checking access: {}", err.getMessage());
      routingContext.fail(new OgcException(500, INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR));

    });
  }

}
