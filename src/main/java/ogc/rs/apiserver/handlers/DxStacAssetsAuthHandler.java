package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.User;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.authenticator.Constants.*;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;

public class DxStacAssetsAuthHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(DxStacAssetsAuthHandler.class);
  private final DatabaseService databaseService;

  public DxStacAssetsAuthHandler(Vertx vertx) {
    this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
  }

  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("STAC Assets Authorization");

    User user = routingContext.get(USER_KEY);
    UUID userId = user.getResourceId();
    String assetId = routingContext.pathParam("assetId");

    databaseService
        .getAssets(assetId)
        .onSuccess(
            asset -> {
              if (asset.isEmpty()) {
                LOGGER.debug("Asset not found.");
                routingContext.fail(new OgcException(404, NOT_FOUND, ASSET_NOT_FOUND));
                return;
              }

              LOGGER.debug("Asset found: {}", asset);

              if (!user.isRsToken() && !asset.getString("stac_collections_id").equals(userId)) {
                LOGGER.error("Collection associated with asset is not the same as in token.");
                routingContext.fail(new OgcException(401, NOT_AUTHORIZED, INVALID_COLLECTION_ID));
                return;
              }

              LOGGER.debug("Collection ID in token validated.");

              databaseService
                  .getAccess(asset.getString("stac_collections_id"))
                  .onSuccess(
                      isOpenResource -> {
                        if (isOpenResource && user.isRsToken()) {
                          LOGGER.debug("Resource is open, access granted.");
                          routingContext.put("isAuthorised", true);
                          routingContext.next();
                        } else {
                          handleSecureResource(routingContext, user, isOpenResource);
                        }
                      })
                  .onFailure(
                      failure -> {
                        LOGGER.error(
                            "Failed to retrieve collection access: {}", failure.getMessage());
                        routingContext.fail(failure);
                      });
            })
        .onFailure(
            failure -> {
              LOGGER.error("Asset retrieval failed: {}", failure.getMessage());
              routingContext.fail(failure);
            });
  }

  private void handleSecureResource(
      RoutingContext routingContext, User user, boolean isOpenResource) {
    if (!isOpenResource) {
      LOGGER.debug("Not an open resource, it's a secure resource.");

      if (user.getRole() == User.RoleEnum.provider) {
        routingContext.put("isAuthorised", true);
        routingContext.next();
      } else if (user.getRole() == User.RoleEnum.consumer) {
        JsonArray access =
            user.getConstraints() != null ? user.getConstraints().getJsonArray("access") : null;

        if (access == null || !access.contains("api")) {
          routingContext.fail(new OgcException(401, NOT_AUTHORIZED, USER_NOT_AUTHORIZED));
        } else {
          routingContext.put("isAuthorised", true);
          routingContext.next();
        }
      } else {
        LOGGER.debug("Role not recognized: {}", user.getRole());
        routingContext.fail(
            new OgcException(401, NOT_AUTHORIZED, NOT_PROVIDER_OR_CONSUMER_TOKEN + user.getRole()));
      }
    } else {
      LOGGER.debug("Resource is open but token is secure for role: {}", user.getRole());
      routingContext.fail(
          new OgcException(401, NOT_AUTHORIZED, RESOURCE_OPEN_TOKEN_SECURE + user.getRole()));
    }
  }
}
