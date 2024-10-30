package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.apiserver.util.Constants.*;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;

public class StacAssetsAuthZHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(StacAssetsAuthZHandler.class);
  private final DatabaseService databaseService;

  public StacAssetsAuthZHandler(Vertx vertx) {
    this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
  }

  /**
   * Handles the routing context to authorize access to STAC asset APIs.
   *
   * @param routingContext the routing context of the request
   */
  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("STAC Assets Authorization");

    AuthInfo user = routingContext.get(USER_KEY);
    UUID resourceId = user.getResourceId();
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
            try {
                String collectionId = asset.containsKey("stac_collections_id")
                  ? asset.getString("stac_collections_id") : asset.getString("collection_id");
                if (!user.isRsToken()
                    && !collectionId.equals(resourceId.toString())) {
                  LOGGER.error("Collection associated with asset is not the same as in token.");
                  routingContext.fail(new OgcException(401, NOT_AUTHORIZED, INVALID_COLLECTION_ID));
                  return;
                }

                LOGGER.debug("Collection ID in token validated.");


                databaseService
                    .getAccess(collectionId)
                    .onSuccess(
                        isOpenResource -> {
                          user.setResourceId(UUID.fromString(collectionId));

                          if (isOpenResource && user.isRsToken()) {
                            LOGGER.debug("Resource is open, access granted.");
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
              } catch (Exception e) {
                LOGGER.error("Something went wrong here! {}",e.getMessage());
                routingContext.fail(e.getCause());
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error("Asset retrieval failed: {}", failure.getMessage());
              routingContext.fail(failure);
            });
  }

  private void handleSecureResource(
      RoutingContext routingContext, AuthInfo user, boolean isOpenResource) {
    if (!isOpenResource) {
      LOGGER.debug("Not an open resource, it's a secure resource.");

      if (user.getRole() == AuthInfo.RoleEnum.provider) {
        routingContext.next();
      } else if (user.getRole() == AuthInfo.RoleEnum.consumer) {
        JsonArray access =
            user.getConstraints() != null ? user.getConstraints().getJsonArray("access") : null;

        if (access == null || !access.contains("api")) {
          LOGGER.debug("Invalid consumer token. Constrains not present.");
          routingContext.fail(new OgcException(401, NOT_AUTHORIZED, USER_NOT_AUTHORIZED));
        } else {
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
