package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueService;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.apiserver.util.Constants.NOT_AUTHORIZED;
import static ogc.rs.apiserver.util.Constants.USER_NOT_AUTHORIZED;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.UUID_REGEX;

public class StacCollectionOnboardingAuthZHandler implements Handler<RoutingContext> {

    private final DatabaseService databaseService;
    private static final Logger LOGGER = LogManager.getLogger(StacCollectionOnboardingAuthZHandler.class);
    CatalogueService catalogueService;

    public StacCollectionOnboardingAuthZHandler(Vertx vertx, JsonObject config) {

        this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
        catalogueService = new CatalogueService(vertx, config);
    }

    /**
     * Handles the routing context to authorize access to STAC Collection Onboarding APIs.
     * The token should be open and either provider or delegate
     *
     * @param routingContext the routing context of the request
     */

  @Override
  public void handle(RoutingContext routingContext) {
    AuthInfo user = routingContext.get(USER_KEY);
    LOGGER.debug("STAC Onboarding Authorization: " + routingContext.data());

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

      if (user.getRole() != AuthInfo.RoleEnum.provider
          && user.getRole() != AuthInfo.RoleEnum.delegate) {
        routingContext.fail(
            new OgcException(401, NOT_AUTHORIZED, "Role Not Provider or delegate"));
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

        if (user.getRole() != AuthInfo.RoleEnum.provider
            && user.getRole() != AuthInfo.RoleEnum.delegate) {
          routingContext.fail(
              new OgcException(401, NOT_AUTHORIZED, "Role Not Provider or delegate"));
          return;
        }
          checkCollectionOwnership(collectionId, user, routingContext, validatedCollectionId, collections.size());
      }
    }
    }


    private void validateCollectionOwnership(String collectionId, AuthInfo user, RoutingContext routingContext) {
        catalogueService
                .getCatItemOwnerUserId(collectionId)
                .onSuccess(
                        success -> {
                            if (user.getRole() == AuthInfo.RoleEnum.provider && !user.isRsToken()) {
                                routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "open token should be used"));
                                return;
                            }
                            if (user.getRole() == AuthInfo.RoleEnum.provider &&
                                    !(success.getString("ownerUserId").trim().equals(user.getUserId().toString()))) {
                                routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Item belongs to different provider"));
                                return;
                            }
                            if (user.getRole() == AuthInfo.RoleEnum.delegate && !success.getString("ownerUserId").equals(user.getDelegatorUserId().toString())) {
                                routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Item belongs to different provider"));
                                return;
                            }
                            routingContext.data().put("ownerUserId", success.getString("ownerUserId"));
                            routingContext.data().put("accessPolicy", success.getString("accessPolicy"));
                            routingContext.data().put("role", user.getRole().toString());
                            routingContext.next();
                        })
                .onFailure(failed -> {
                    LOGGER.debug("cat item not found ");
                    if (failed instanceof OgcException) {
                        routingContext.put("response", ((OgcException) failed).getJson().toString());
                        routingContext.put("statusCode", ((OgcException) failed).getStatusCode());
                    } else {
                        OgcException ogcException =
                                new OgcException(500, "Internal Server Error", "Internal Server Error");
                        routingContext.put("response", ogcException.getJson().toString());
                        routingContext.put("statusCode", ogcException.getStatusCode());
                    }
                    routingContext.next();
                });
    }

    private void checkCollectionOwnership(String collectionId, AuthInfo user, RoutingContext routingContext,ArrayList<String> validatedcollecrionId ,int totalCollections) {
        catalogueService.getCatItemOwnerUserId(collectionId)
                .onSuccess(success -> {
                    if (user.getRole() == AuthInfo.RoleEnum.provider && !user.isRsToken()) {
                        routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Open token should be used"));
                        return;
                    }
                    if (user.getRole() == AuthInfo.RoleEnum.provider &&
                            !(success.getString("ownerUserId").trim().equals(user.getUserId().toString()))) {
                        routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Item belongs to different provider"));
                        return;
                    }
                    if (user.getRole() == AuthInfo.RoleEnum.delegate &&
                            !success.getString("ownerUserId").equals(user.getDelegatorUserId().toString())) {
                        routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Item belongs to different provider"));
                        return;
                    }

                    routingContext.data().put("ownerUserId", success.getString("ownerUserId"));
                    routingContext.data().put("accessPolicy", success.getString("accessPolicy"));
                    routingContext.data().put("role", user.getRole().toString());

                    // Move to next handler only after all validations succeed
                    validatedcollecrionId.add(collectionId);

                    if (validatedcollecrionId.size() == totalCollections) {
                        LOGGER.debug("All the collections are validated");
                        routingContext.next();
                        return;
                    }
                })
                .onFailure(failed -> {
                    routingContext.fail(new OgcException(500, "Internal Server Error", "Validation failed for collection: " + collectionId));
                });
    }
}
