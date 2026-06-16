package ogc.rs.apiserver.handlers;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.apiserver.util.AuthInfo.RoleEnum;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.UUID_REGEX;
import static ogc.rs.processes.featureCollectionOnboarding.Constants.CAT_RESPONSE_FAILURE;
import static ogc.rs.processes.featureCollectionOnboarding.Constants.ITEM_NOT_PRESENT_ERROR;
import static ogc.rs.processes.featureCollectionOnboarding.Constants.RESOURCE_OWNERSHIP_ERROR;

/**
 * Authorization for OGC Maps (collection map resource).
 */
public class OgcMapsAuthZHandler implements Handler<RoutingContext> {

  private static final String CATALOGUE_NON_2XX_RESPONSE = "Catalogue returned non-2XX response";

  private final JsonObject config;
  private final DatabaseService databaseService;
  private final WebClient catItemWebClient;

  private static final Logger LOGGER = LogManager.getLogger(OgcMapsAuthZHandler.class);

  public OgcMapsAuthZHandler(Vertx vertx, JsonObject config) {
    this.config = config;
    this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    this.catItemWebClient =
        WebClient.create(
            vertx,
            new WebClientOptions().setSsl(true).setTrustAll(false).setVerifyHost(true));
  }

  /**
   * Handles the routing context to authorize access to OGC Maps APIs.
   *
   * @param routingContext the routing context of the request
   */
  @Override
  public void handle(RoutingContext routingContext) {

    if (System.getProperty("disable.auth") != null) {
      LOGGER.info(
          "OgcMapsAuthZHandler: disable.auth set, skipping auth path={}",
          routingContext.normalizedPath());
      routingContext.next();
      return;
    }

    LOGGER.info("OgcMapsAuthZHandler: checking authorization path={}", routingContext.normalizedPath());
    String collectionId = routingContext.normalizedPath().split("/")[2];
    if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
      routingContext.fail(new OgcException(404, "Not Found", "Collection Not Found"));
      return;
    }
    AuthInfo user = routingContext.get(USER_KEY);
    UUID iid = user.getResourceId();
    if (!user.isRsToken() && !collectionId.equals(iid.toString())) {
      LOGGER.error("Resource Ids don't match! id- {}, jwtId- {}", collectionId, iid);
      routingContext.fail(
          new OgcException(
              401, "Not Authorized", "User is not authorised. Please contact DX AAA "));
      return;
    }

    databaseService
        .getAccess(collectionId)
        .onSuccess(
            isOpen -> {

              user.setResourceId(UUID.fromString(collectionId));

              LOGGER.info(
                  "OgcMapsAuthZHandler: getAccess succeeded collectionId={} isOpen={} role={} isRsToken={}",
                  collectionId,
                  isOpen,
                  user.getRole(),
                  user.isRsToken());

              if (isOpen && user.isRsToken()) {
                authorizeProviderOrDelegate(routingContext, user, collectionId);
              } else {
                if (user.getRole() == AuthInfo.RoleEnum.consumer) {
                  handleConsumerAccess(routingContext, collectionId);
                } else {
                  authorizeProviderOrDelegate(routingContext, user, collectionId);
                }
              }
            })
        .onFailure(
            fail -> {
              LOGGER.error("Collection not present in table: {}", fail.getMessage());
              routingContext.fail(fail);
            });
  }

  private void authorizeProviderOrDelegate(
      RoutingContext routingContext, AuthInfo user, String collectionId) {
    catalogueItemExists(collectionId)
        .compose(catalogueItem -> verifyCatalogueOwnership(user, catalogueItem))
        .onSuccess(
            v -> {
              LOGGER.info(
                  "OgcMapsAuthZHandler: authorization passed, continuing to handler collectionId={}",
                  collectionId);
              LOGGER.debug("Authorization info: {}", routingContext.data().values());
              routingContext.next();
            })
        .onFailure(routingContext::fail);
  }

  private void handleConsumerAccess(RoutingContext routingContext, String collectionId) {
    AuthInfo user = routingContext.get(USER_KEY);
    JsonArray access =
        user.getConstraints() != null ? user.getConstraints().getJsonArray("access") : null;
    if (access == null || !access.contains("api")) {
      LOGGER.debug("invalid constraints value");
      routingContext.fail(
          new OgcException(
              401, "Not Authorized", "User is not authorised. Please contact DX AAA "));

    } else {
      LOGGER.info(
          "OgcMapsAuthZHandler: consumer constraints validated (api) collectionId={}",
          collectionId);
      catalogueItemExists(collectionId)
          .onSuccess(
              v -> {
                LOGGER.info(
                    "OgcMapsAuthZHandler: consumer authorization passed collectionId={}",
                    collectionId);
                routingContext.next();
              })
          .onFailure(routingContext::fail);
    }
  }

  /**
   * Verifies the collection item exists in the DX catalogue.
   *
   * @param collectionId collection resource id
   * @return catalogue item JSON when present
   */
  private Future<JsonObject> catalogueItemExists(String collectionId) {
    Promise<JsonObject> promise = Promise.promise();
    String catHost = config.getString("catServerHost");
    int catPort = config.getInteger("catServerPort");
    String catItemsUri = config.getString("catRequestItemsUri");

    catItemWebClient
        .get(catPort, catHost, catItemsUri)
        .addQueryParam("id", collectionId)
        .send()
        .onSuccess(
            responseFromCat -> {
              if (responseFromCat.statusCode() != 200) {
                LOGGER.error(
                    "{} status={} collectionId={}",
                    CATALOGUE_NON_2XX_RESPONSE,
                    responseFromCat.statusCode(),
                    collectionId);
                promise.fail(new OgcException(503, "Service Unavailable", CATALOGUE_NON_2XX_RESPONSE));
                return;
              }
              JsonObject body;
              try {
                body = responseFromCat.bodyAsJsonObject();
              } catch (Exception e) {
                LOGGER.error(
                    "catalogueItemExists: invalid JSON from catalogue collectionId={}",
                    collectionId,
                    e);
                promise.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
                return;
              }
              JsonArray results = body.getJsonArray("results");
              if (results == null || results.isEmpty()) {
                LOGGER.error("{} collectionId={}", ITEM_NOT_PRESENT_ERROR, collectionId);
                promise.fail(new OgcException(404, "Not Found", "Not Found"));
                return;
              }
              LOGGER.info("catalogueItemExists: ok collectionId={}", collectionId);
              promise.complete(results.getJsonObject(0));
            })
        .onFailure(
            failureResponseFromCat -> {
              LOGGER.error(CAT_RESPONSE_FAILURE + failureResponseFromCat.getMessage());
              promise.fail(
                  new OgcException(503, "Service Unavailable", "Service Unavailable"));
            });

    return promise.future();
  }

  /**
   * Enforces catalogue ownership for provider and delegate roles.
   *
   * @param user authenticated user
   * @param catalogueItem catalogue item returned by {@link #catalogueItemExists(String)}
   */
  private Future<Void> verifyCatalogueOwnership(AuthInfo user, JsonObject catalogueItem) {
    Promise<Void> promise = Promise.promise();
    String owner = catalogueItem.getString("ownerUserId");

    if (user.getRole() == RoleEnum.provider && !user.isRsToken()) {
      promise.fail(
          new OgcException(
              401, "Not Authorized", "User is not authorised. Please contact DX AAA "));
      return promise.future();
    }

    if (user.getRole() == RoleEnum.provider) {
      if (owner == null || !owner.equals(user.getUserId().toString())) {
        LOGGER.error(RESOURCE_OWNERSHIP_ERROR);
        promise.fail(new OgcException(403, "Forbidden", RESOURCE_OWNERSHIP_ERROR));
        return promise.future();
      }
    } else if (user.getRole() == RoleEnum.delegate) {
      if (user.getDelegatorUserId() == null
          || owner == null
          || !owner.equals(user.getDelegatorUserId().toString())) {
        LOGGER.error(
            "verifyCatalogueOwnership: delegate ownership mismatch ownerUserId present={}",
            owner != null);
        promise.fail(new OgcException(403, "Forbidden", "Forbidden"));
        return promise.future();
      }
    }

    LOGGER.info(
        "verifyCatalogueOwnership: ok role={} ownerUserId present={}",
        user.getRole(),
        owner != null);
    promise.complete();
    return promise.future();
  }
}
