package ogc.rs.apiserver.handlers;


import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueService;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.apiserver.util.Constants.NOT_AUTHORIZED;
import static ogc.rs.apiserver.util.Constants.USER_NOT_AUTHORIZED;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.UUID_REGEX;

public class StacItemOnboardingAuthZHandler implements Handler<RoutingContext> {

  private final DatabaseService databaseService;
  private static final Logger LOGGER = LogManager.getLogger(StacItemOnboardingAuthZHandler.class);

  public StacItemOnboardingAuthZHandler(Vertx vertx) {

    this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
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
    AuthInfo user = routingContext.get(USER_KEY);

    LOGGER.debug("STAC Item Onboarding Authorization" + routingContext.data());

    collectionId = routingContext.normalizedPath().split("/")[3];

    if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
      LOGGER.debug("collectionId   " + collectionId);
      routingContext.fail(new OgcException(400, "Not Found", "Invalid Collection Id"));
      return;
    }
    if (user.getRole() != AuthInfo.RoleEnum.provider
        && user.getRole() != AuthInfo.RoleEnum.delegate || user.getDelegatorRole() != AuthInfo.RoleEnum.provider) {
          routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Only provider or provider delegate is authorized" +
              " to perform this action."));
          return;
    }

    databaseService.getAccessDetails(collectionId)
        .onSuccess(success -> {
          if (user.getRole() == AuthInfo.RoleEnum.provider
              && !user.getUserId().toString().equalsIgnoreCase(success.getString("role_id"))) {
            LOGGER.debug("Provider does not have access to the collection");
            routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Not authorized to access this collection"));
            return;
          }
          if (user.getRole() == AuthInfo.RoleEnum.delegate
              && !user.getDelegatorUserId().toString().equalsIgnoreCase(success.getString("role_id"))) {
            LOGGER.debug("Provider delegate does not have access to the collection");
            routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Not authorized to access this collection"));
            return;
          }
          if (!user.isRsToken()) {
            routingContext.fail(new OgcException(401, NOT_AUTHORIZED, "Not authorized. Please use an open token " +
              "to perform this action."));
            return;
          }
          routingContext.data().put("ownerUserId", success.getString("role_id"));
          routingContext.data().put("role", user.getRole().toString());
          routingContext.next();
        })
        .onFailure(failed -> {
          if (failed instanceof OgcException) {
            routingContext.put("response", ((OgcException) failed).getJson().toString());
            routingContext.put("statusCode", ((OgcException) failed).getStatusCode());
          } else {
            OgcException ogcException =
                new OgcException(500, "Internal Server Error", "Internal Server Error");
            routingContext.put("response", ogcException.getJson().toString());
            routingContext.put("statusCode", ogcException.getStatusCode());
          }
          routingContext.fail(failed);
        });
  }
}

