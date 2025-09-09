package ogc.rs.apiserver.handlers;

import static ogc.rs.common.Constants.UUID_REGEX;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import ogc.rs.apiserver.authentication.client.AclClient;
import ogc.rs.apiserver.authentication.util.DxUser;
import ogc.rs.apiserver.authorization.model.DxRole;
import ogc.rs.apiserver.authorization.util.AccessPolicy;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handler for authorizing access to STAC items based on the provided token and collection access permissions.
 * Determines whether a pre-signed URL should be generated based on authentication and authorization rules.
 */
public class StacItemByIdAuthZHandler implements Handler<RoutingContext> {

  public static final String SHOULD_CREATE_KEY = "shouldCreate";
  // Key used to store authorization result in the RoutingContext
  private static final int TOKEN_EXPIRY_THRESHOLD_SECONDS = 10; // token expiry threshold
  private static final Logger LOGGER = LogManager.getLogger(StacItemByIdAuthZHandler.class);
  private final CatalogueService catalogueService;
  private final AclClient aclClient;
  Vertx vertx;

  /**
   * Constructs the handler with Vert.x and initializes the database service.
   *
   * @param catalogueService catalogueService service to fetch asset metadata
   * @param aclClient        client to check access permissions
   */
  public StacItemByIdAuthZHandler(CatalogueService catalogueService, AclClient aclClient) {
    this.catalogueService = catalogueService;
    this.aclClient = aclClient;
  }

  @Override
  public void handle(RoutingContext routingContext) {

    if (System.getProperty("disable.auth") != null) {
      routingContext.put(SHOULD_CREATE_KEY, false);
      routingContext.next();
      return;
    }

    LOGGER.debug("Inside StacItemByIdAuthZHandler");
    String collectionId = routingContext.pathParam("collectionId");

    if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
      routingContext.fail(new OgcException(404, "Not Found", "Collection Not Found"));
      return;
    }
    DxUser user = RoutingContextHelper.fromPrincipal(routingContext);

    //TODO: check if this is required
    if (user == null) {
      LOGGER.debug("No token provided or token expired, proceeding without authentication...");
      routingContext.put(SHOULD_CREATE_KEY, false);
      routingContext.next();
      return;
    }

    long expiry = user.getTokenExpiry();
    long currentTime = Instant.now().getEpochSecond();
    if (expiry - currentTime <= TOKEN_EXPIRY_THRESHOLD_SECONDS) {
      LOGGER.warn("Token expiry is less than {} seconds away. Disabling pre-signed URL generation.",
          TOKEN_EXPIRY_THRESHOLD_SECONDS);
      routingContext.put(SHOULD_CREATE_KEY, false);
      routingContext.next();
      return;
    }

    catalogueService.getCatalogueAsset(collectionId).onSuccess(catAsset -> {
      if (catAsset == null) {
        routingContext.fail(new OgcException(404, "Not Found", "Item Not Found"));
        return;
      }
      RoutingContextHelper.setAsset(routingContext, catAsset);
      String accessPolicy = catAsset.getAccessPolicy();
      LOGGER.debug("Access policy for item is: {}", accessPolicy);
      if (AccessPolicy.fromValue(accessPolicy) == AccessPolicy.OPEN) {
        LOGGER.debug("Access policy is open, access granted.");
        routingContext.put(SHOULD_CREATE_KEY, true);
        routingContext.next();
        return;
      }
      LOGGER.debug("Not an open resource, it's a {} resource.", accessPolicy);
      if (user.getRoles().contains(DxRole.PROVIDER.toString())) {
        routingContext.put(SHOULD_CREATE_KEY, true);
        routingContext.next();
      } else if (user.getRoles().contains(DxRole.CONSUMER.toString())) {
        String bearerToken = routingContext.request().getHeader("Authorization");
        aclClient.checkAccess(collectionId, bearerToken)
            .onSuccess(accessGranted -> {
              if (accessGranted) {
                routingContext.put(SHOULD_CREATE_KEY, true);
                LOGGER.debug("Access was granted for itemId: {}", collectionId);
                routingContext.next();
              } else {
                LOGGER.debug("Invalid consumer token. Constraints not present.");
                routingContext.put(SHOULD_CREATE_KEY, false);
                routingContext.next();
              }
            })
            .onFailure(err -> {
              LOGGER.error("Access verification failed: {}", err.getMessage());
              routingContext.put(SHOULD_CREATE_KEY, false);
              routingContext.next();
            });
      }
    }).onFailure(err -> {
      LOGGER.error("Failed to retrieve collection access: {}", err.getMessage());
      routingContext.put(SHOULD_CREATE_KEY, false);
      routingContext.next();
    });
  }
}