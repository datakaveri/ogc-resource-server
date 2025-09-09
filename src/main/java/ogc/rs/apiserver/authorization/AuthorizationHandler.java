package ogc.rs.apiserver.authorization;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.authentication.client.AclClient;
import ogc.rs.apiserver.authorization.model.Asset;
import ogc.rs.apiserver.authorization.util.AccessPolicy;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//TODO: Hereee delete the class as it is not used anywhere
/**
 * Utility class to check resource access by making a POST request to a control panel has_access endpoint.
 */
public class AuthorizationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(AuthorizationHandler.class);
  private final CatalogueService catalogueService;
  private final AclClient aclClient;

  /**
   * Constructs the AuthorizationHandler with required services.
   *
   * @param catalogueService service to fetch asset metadata
   * @param aclClient        client to check access permissions
   */
  public AuthorizationHandler(CatalogueService catalogueService, AclClient aclClient) {
    this.catalogueService = catalogueService;
    this.aclClient = aclClient;
  }

  @Override
  public void handle(RoutingContext context) {
    LOGGER.debug("Starting access verification process");

    String itemId = RoutingContextHelper.getId(context);
    String bearerToken = context.request().getHeader("Authorization");
    Asset asset = RoutingContextHelper.getAsset(context);

    if (itemId == null || bearerToken == null) {
      context.fail(new OgcException(400, "Bad Request", "Missing itemId or bearerToken"));
      return;
    }
    /*asset information is not set earlier in any authz handler, so calling the catalogue server to get the info*/
    if (asset == null) {
      catalogueService.getCatalogueAsset(itemId).onSuccess(catAsset -> {
        if (catAsset == null) {
          context.fail(new OgcException(404, "Not Found", "Item Not Found"));
          return;
        }
        /* set asset information in routing context helper */
        RoutingContextHelper.setAsset(context, catAsset);
      }).onFailure(err -> {
        LOGGER.error("Failed to fetch item metadata: {}", err.getMessage());
        context.fail(new OgcException(500, "Internal Server Error", "Error fetching item metadata"));
      });
    }
        /* check if has access api is called */
        boolean hasAccessApiCalled  = RoutingContextHelper.getHasAccessApiCalled(context);
        Asset catAsset = RoutingContextHelper.getAsset(context);
        AccessPolicy accessPolicyValue = AccessPolicy.fromValue(catAsset.getAccessPolicy());
        LOGGER.debug("Fetched access policy: {}", accessPolicyValue);
        /*Set access policy of the resource item in routing context helper*/
        RoutingContextHelper.setAccessPolicy(context, accessPolicyValue);
        /*Access Policy of the resource is open, no more authorization is required*/
        if (accessPolicyValue == AccessPolicy.OPEN) {
          LOGGER.debug("Item accessPolicy is OPEN. Skipping access check.");
          context.next();
        } else if(!hasAccessApiCalled) {
          LOGGER.debug("Item accessPolicy is {}. Performing access check.", accessPolicyValue);
          /*Access Policy of the resource is not open, the consumer's access to the resource is validated
           * by calling controlplane to check if there is any policy present for the current resource*/

          aclClient.checkAccess(itemId, bearerToken)
              .onSuccess(accessGranted -> {
                if (accessGranted) {
                  context.next();
                  LOGGER.debug("Access was granted for itemId: {}", itemId);
                } else {
                  context.fail(
                      new OgcException(403, "Forbidden", "User not authorized to access the resource : " + itemId));
                }
              })
              .onFailure(err -> {
                LOGGER.error("Access verification failed: {}", err.getMessage());
                context.fail(new OgcException(500, "Internal Server Error", "Error during access verification"));
              });
        }
  }

}
