package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Instant;
import java.util.UUID;
import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.UUID_REGEX;

/**
 * Handler for authorizing access to STAC items based on the provided token and collection access permissions.
 * Determines whether a pre-signed URL should be generated based on authentication and authorization rules.
 */
public class StacItemByIdAuthZHandler implements Handler<RoutingContext> {

    Vertx vertx;
    public static final String SHOULD_CREATE_KEY = "shouldCreate"; // Key used to store authorization result in the RoutingContext
    private static final int TOKEN_EXPIRY_THRESHOLD_SECONDS = 10; // token expiry threshold
    private final DatabaseService databaseService;
    private static final Logger LOGGER = LogManager.getLogger(StacItemByIdAuthZHandler.class);

    /**
     * Constructs the handler with Vert.x and initializes the database service.
     *
     * @param vertx The Vert.x instance.
     */
    public StacItemByIdAuthZHandler(Vertx vertx) {
        this.vertx = vertx;
        this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    }

    /**
     * Handles the authorization logic for accessing STAC items.
     *
     * @param routingContext The routing context containing request details.
     */
    @Override
    public void handle(RoutingContext routingContext) {

        if (System.getProperty("disable.auth") != null) {
            routingContext.put(SHOULD_CREATE_KEY, false);
            routingContext.next();
            return;
        }

        LOGGER.debug("STAC Item By Asset Id Authorization");
        String collectionId = routingContext.pathParam("collectionId");

        // Validate collectionId format
        if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
            routingContext.fail(new OgcException(404, "Not Found", "Collection Not Found"));
            return;
        }

        // Retrieve user authentication info from context
        AuthInfo user = routingContext.get(USER_KEY);

        if (user == null) {
            LOGGER.debug("No token provided or token expired, proceeding without authentication...");
            routingContext.put(SHOULD_CREATE_KEY, false);
            routingContext.next();
            return;
        }

        // Token Expiry Check
        long expiry = user.getExpiry();
        long currentTime = Instant.now().getEpochSecond();
        if (expiry - currentTime <= TOKEN_EXPIRY_THRESHOLD_SECONDS) {
            LOGGER.warn("Token expiry is less than {} seconds away. Disabling pre-signed URL generation.", TOKEN_EXPIRY_THRESHOLD_SECONDS);
            routingContext.put(SHOULD_CREATE_KEY, false);
            routingContext.next();
            return;
        }

        // Validate resource ID if not an RS token
        UUID iid = user.getResourceId();
        if (!user.isRsToken() && !collectionId.equals(iid.toString())) {
            LOGGER.error("Resource IDs don't match! id- {}, jwtId- {}", collectionId, iid);
            routingContext.put(SHOULD_CREATE_KEY, false);
            routingContext.next();
            return;
        }

        // Check collection access permissions
        databaseService.getAccess(collectionId)
                .onSuccess(isOpenResource -> {
                    user.setResourceId(UUID.fromString(collectionId));

                    if (isOpenResource && user.isRsToken()) {
                        LOGGER.debug("Resource is open, access granted.");
                        routingContext.put(SHOULD_CREATE_KEY, true);
                    } else if (!isOpenResource && user.isRsToken()) {
                        LOGGER.debug("Resource is secure, but passed an open token.");
                        routingContext.put(SHOULD_CREATE_KEY, false);
                    } else {
                        handleSecureResource(routingContext, user, isOpenResource);
                    }
                    routingContext.next();
                })
                .onFailure(failure -> {
                    LOGGER.error("Failed to retrieve collection access: {}", failure.getMessage());
                    routingContext.put(SHOULD_CREATE_KEY, false);
                    routingContext.next();
                });
    }

    /**
     * Handles authorization logic for secure resources, based on user roles and access constraints.
     *
     * @param routingContext The routing context.
     * @param user           The authenticated user information.
     * @param isOpenResource Whether the resource is publicly accessible.
     */
    private void handleSecureResource(RoutingContext routingContext, AuthInfo user, boolean isOpenResource) {
        if (!isOpenResource) {
            LOGGER.debug("Not an open resource, it's a secure resource.");

            if (user.getRole() == AuthInfo.RoleEnum.provider) {
                routingContext.put(SHOULD_CREATE_KEY, true);
            } else if (user.getRole() == AuthInfo.RoleEnum.consumer) {
                JsonArray access = user.getConstraints() != null ? user.getConstraints().getJsonArray("access") : null;

                if (access == null || !access.contains("api")) {
                    LOGGER.debug("Invalid consumer token. Constraints not present.");
                    routingContext.put(SHOULD_CREATE_KEY, false);
                } else {
                    routingContext.put(SHOULD_CREATE_KEY, true);
                }
            } else {
                LOGGER.debug("Role not recognized: {}", user.getRole());
                routingContext.put(SHOULD_CREATE_KEY, false);
            }
        } else {
            LOGGER.debug("Resource is open but token is secure for role: {}", user.getRole());
            routingContext.put(SHOULD_CREATE_KEY, false);
        }
    }
}
