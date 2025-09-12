package ogc.rs.apiserver.handlers;

import static ogc.rs.apiserver.util.Constants.NOT_AUTHORIZED;
import static ogc.rs.common.Constants.UUID_REGEX;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import ogc.rs.apiserver.authentication.util.DxUser;
import ogc.rs.apiserver.authorization.model.DxRole;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StacCollectionOnboardingAuthZHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(StacCollectionOnboardingAuthZHandler.class);
  CatalogueInterface catalogueService;

  public StacCollectionOnboardingAuthZHandler(CatalogueInterface catalogueService) {

    this.catalogueService = catalogueService;
  }

  /**
   * Handles the routing context to authorize access to STAC Collection Onboarding APIs.
   * The token should be open and either provider  or delegate
   * The authorization checks include:
   * 1. Validates that the user has the 'provider' role.
   * 2. Validates that the user is the owner of the collection(s) being onboarded.
   *
   * @param routingContext the routing context of the request
   */

  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("STAC Collection Onboarding Authorization");
    DxUser user = RoutingContextHelper.fromPrincipal(routingContext);
    JsonObject requestBody = routingContext.body().asJsonObject();
    JsonArray collections = requestBody.getJsonArray("collections");

    if (collections == null) {
      // Single collection case
      String collectionId = requestBody.getString("id");

      if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
        routingContext.fail(
            new OgcException(400, "Invalid UUID", "Invalid Collection Id: " + collectionId));
        return;
      }

      validateCollectionOwnership(collectionId, user, routingContext);
    } else {
      LOGGER.debug("Processing all collections...");

      ArrayList<String> validatedCollectionId = new ArrayList<>();

      for (int i = 0; i < collections.size(); i++) {
        String collectionId = collections.getJsonObject(i).getString("id");

        if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
          routingContext.fail(
              new OgcException(400, "Invalid UUID", "Invalid Collection Id: " + collectionId));
          return; // Stop processing immediately
        }

        checkCollectionOwnership(collectionId, user, routingContext, validatedCollectionId, collections.size());
      }
    }
  }

  private void validateCollectionOwnership(String collectionId, DxUser user, RoutingContext routingContext) {
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
      /* Is the user having provider role and is resource owner*/
      LOGGER.debug("User roles: {}, isProviderTheResourceOwner: {}", user.getRoles(), isProviderTheResourceOwner);
      if (!user.getRoles().contains(DxRole.PROVIDER.getRole()) || !isProviderTheResourceOwner) {
        routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Ownership check failed"));
        return;
      }
      routingContext.next();
    }).onFailure(err -> {
      LOGGER.error("Failed to fetch item metadata: {}", err.getMessage());
      routingContext.fail(new OgcException(500, "Internal Server Error", "Error fetching item metadata"));
    });
  }


  private void checkCollectionOwnership(String collectionId, DxUser user, RoutingContext routingContext,
                                        ArrayList<String> validatedCollectionId, int totalCollections) {

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
      /* Is the user having provider role and is resource owner*/
      if (!user.getRoles().contains(DxRole.PROVIDER.getRole()) && !isProviderTheResourceOwner) {
        routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Ownership check failed"));
        return;
      }
      // Move to next handler only after all validations succeed
      validatedCollectionId.add(collectionId);
      if (validatedCollectionId.size() == totalCollections) {
        LOGGER.debug("All the collections are validated");
        routingContext.next();
      }

    }).onFailure(err -> {
      LOGGER.error("Failed to fetch item metadata: {}", err.getMessage());
      routingContext.fail(new OgcException(500, "Internal Server Error", "Error fetching item metadata"));
    });

  }

}
