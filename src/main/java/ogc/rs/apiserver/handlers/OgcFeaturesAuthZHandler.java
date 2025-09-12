package ogc.rs.apiserver.handlers;

import static ogc.rs.apiserver.util.Constants.HEADER_AUTHORIZATION;
import static ogc.rs.common.Constants.UUID_REGEX;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.authentication.client.AclClient;
import ogc.rs.apiserver.authentication.util.DxUser;
import ogc.rs.apiserver.authorization.model.DxRole;
import ogc.rs.apiserver.authorization.util.AccessPolicy;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OgcFeaturesAuthZHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(OgcFeaturesAuthZHandler.class);
  private final CatalogueInterface catalogueService;
  private final AclClient aclClient;

  public OgcFeaturesAuthZHandler(CatalogueInterface catalogueService, AclClient aclClient) {
    this.catalogueService = catalogueService;
    this.aclClient = aclClient;
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

    LOGGER.debug("Inside OgcFeaturesAuthZHandler");
    String collectionId = routingContext.normalizedPath().split("/")[2];
    if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
      routingContext.fail(new OgcException(404, "Not Found", "Collection Not Found"));
      return;
    }
    DxUser user = RoutingContextHelper.fromPrincipal(routingContext);
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
        String bearerToken = routingContext.request().getHeader(HEADER_AUTHORIZATION);

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
  }
}
