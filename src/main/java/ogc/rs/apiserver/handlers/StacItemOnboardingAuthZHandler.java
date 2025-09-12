package ogc.rs.apiserver.handlers;


import static ogc.rs.apiserver.util.Constants.NOT_AUTHORIZED;
import static ogc.rs.common.Constants.UUID_REGEX;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import ogc.rs.apiserver.authentication.util.DxUser;
import ogc.rs.apiserver.authorization.model.DxRole;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StacItemOnboardingAuthZHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(StacItemOnboardingAuthZHandler.class);
  private final CatalogueInterface catalogueService;

  public StacItemOnboardingAuthZHandler(CatalogueInterface catalogueService) {
    this.catalogueService = catalogueService;
  }

  /**
   * Handles the routing context to authorize access to STAC Item Onboarding APIs.
   * The token should be open, and it should either be a provider or a provider-delegate
   *
   * @param routingContext the routing context of the request
   */

  @Override
  public void handle(RoutingContext routingContext) {
    String collectionId;

    if (System.getProperty("disable.auth") != null) {
      routingContext.next();
      return;
    }

    DxUser user = RoutingContextHelper.fromPrincipal(routingContext);
    List<String> roles = user.getRoles();

    LOGGER.debug("Inside StacItemOnboardingAuthZHandler");

    collectionId = routingContext.normalizedPath().split("/")[3];

    if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
      LOGGER.debug("collectionId   " + collectionId);
      routingContext.fail(new OgcException(400, "Not Found", "Invalid Collection Id"));
      return;
    }
    /* if the role is not provider, 401 is returned back */
    if (!roles.contains(DxRole.PROVIDER.getRole())) {
      routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Only provider or provider delegate is authorized" +
          " to perform this action."));
      return;
    }
    /*Calling catalogue to get information about resourceId / collectionId */
    catalogueService.getCatalogueAsset(collectionId).onSuccess(catAsset -> {
      if (catAsset == null) {
        routingContext.fail(new OgcException(404, "Not Found", "Item Not Found"));
        return;
      }
      /* set asset information in routing context helper */
      RoutingContextHelper.setAsset(routingContext, catAsset);
      String ownerUserId = catAsset.getProviderId();
      boolean isProviderTheResourceOwner = ownerUserId.equals(user.getSub().toString());
      /* Is the user having provider role the resource owner*/
      LOGGER.debug("User roles: {}, isProviderTheResourceOwner: {}", user.getRoles(), isProviderTheResourceOwner);
      if (!isProviderTheResourceOwner) {
        routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Ownership check failed"));
        return;
      }
      routingContext.next();
    }).onFailure(err -> {
      LOGGER.error("Failed to fetch item metadata: {}", err.getMessage());
      routingContext.fail(new OgcException(500, "Internal Server Error", "Error fetching item metadata"));
    });
  }
}

