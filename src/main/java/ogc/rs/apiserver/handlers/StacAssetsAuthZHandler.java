package ogc.rs.apiserver.handlers;

import static ogc.rs.apiserver.util.Constants.ASSET_NOT_FOUND;
import static ogc.rs.apiserver.util.Constants.NOT_FOUND;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.authentication.client.AclClient;
import ogc.rs.apiserver.authentication.util.DxUser;
import ogc.rs.apiserver.authorization.model.DxRole;
import ogc.rs.apiserver.authorization.util.AccessPolicy;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueInterface;
import ogc.rs.catalogue.CatalogueService;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StacAssetsAuthZHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(StacAssetsAuthZHandler.class);
  private final DatabaseService databaseService;
  private final AclClient aclClient;
  private final CatalogueInterface catalogueService;

  public StacAssetsAuthZHandler(Vertx vertx, CatalogueInterface catalogueService, AclClient aclClient) {
    this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    this.aclClient = aclClient;
    this.catalogueService = catalogueService;
  }

  /**
   * Handles the routing context to authorize access to STAC asset APIs.
   *
   * @param routingContext the routing context of the request
   */
  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("Inside StacAssetsAuthZHandler");

    DxUser user = RoutingContextHelper.fromPrincipal(routingContext);
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
                    ? asset.getString("stac_collections_id")
                    : asset.containsKey("collection_id")
                    ? asset.getString("collection_id")
                    : asset.getString("collections_id");

                LOGGER.debug("Collection ID in token validated.");

                /*Access policy is fetched for the given collection ID or resource ID*/
                /* If access policy is open allow access */
                /* Else */
                /*Calling catalogue to get information about resourceId / collectionId */
                catalogueService.getCatalogueAsset(collectionId).onSuccess(catAsset -> {
                  if (catAsset == null) {
                    routingContext.fail(new OgcException(404, "Not Found", "Item Not Found"));
                    return;
                  }
                  /* set asset information in routing context helper */
                  RoutingContextHelper.setAsset(routingContext, catAsset);
                  String accessPolicy = catAsset.getAccessPolicy();
                  LOGGER.debug("Access policy for item is: {}", accessPolicy);
                  if (AccessPolicy.fromValue(accessPolicy) == AccessPolicy.OPEN) {
                    LOGGER.debug("Access policy is open, access granted.");
                    routingContext.next();
                    return;
                  }
                  /* If access policy is not open, check if user has access */
                  LOGGER.debug("Item accessPolicy is {}. Performing access check.", accessPolicy);
                  /* If the role is provider go to the next handler */
                  if (user.getRoles().contains(DxRole.PROVIDER.toString())) {
                    routingContext.next();
                  } else {
                    /* Call control panel's has access endpoint to check if the consumer
                     * has access to the given collection ID or resource */
                    String bearerToken = routingContext.request().getHeader("Authorization");

                    aclClient.checkAccess(collectionId, bearerToken)
                        .onSuccess(accessGranted -> {
                          if (accessGranted) {
                            routingContext.next();
                            LOGGER.debug("Access was granted for itemId: {}", collectionId);
                          } else {
                            routingContext.fail(
                                new OgcException(403, "Forbidden",
                                    "User not authorized to access the resource : " + collectionId));
                          }
                        })
                        .onFailure(err -> {
                          LOGGER.error("Access verification failed: {}", err.getMessage());
                          routingContext.fail(
                              new OgcException(500, "Internal Server Error", "Error during access verification"));
                        });

                  }
                }).onFailure(err -> {
                  LOGGER.error("Failed to fetch item metadata: {}", err.getMessage());
                  routingContext.fail(new OgcException(500, "Internal Server Error", "Error fetching item metadata"));
                });
              } catch (Exception e) {
                LOGGER.error("Something went wrong here! {}", e.getMessage());
                routingContext.fail(e.getCause());
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error("Asset retrieval failed: {}", failure.getMessage());
              routingContext.fail(failure);
            });
  }
}