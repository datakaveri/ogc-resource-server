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
import java.util.UUID;
import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.UUID_REGEX;

public class StacItemByIdAuthZHandler implements Handler<RoutingContext> {

    Vertx vertx;

    private final DatabaseService databaseService;

    private static final Logger LOGGER = LogManager.getLogger(StacItemByIdAuthZHandler.class);

    public StacItemByIdAuthZHandler(Vertx vertx) {
        this.vertx = vertx;
        this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    }

    @Override
    public void handle(RoutingContext routingContext){

        if(System.getProperty("disable.auth") != null) {
            routingContext.put("shouldCreate", false);
            routingContext.next();
            return;
        }

        LOGGER.debug("STAC Item By Asset Id Authorization");

        String collectionId = routingContext.pathParam("collectionId");
        LOGGER.debug("Extracted collectionId: {}", collectionId);

        if (collectionId == null || !collectionId.matches(UUID_REGEX)) {
            routingContext.fail(new OgcException(404, "Not Found", "Collection Not Found"));
            return;
        }

        AuthInfo user = routingContext.get(USER_KEY);

        if (user == null) {
            LOGGER.debug("No token provided, proceeding without authentication.");
            routingContext.put("shouldCreate", false);
            routingContext.next();
            return;
        }

        UUID iid = user.getResourceId();
        if (!user.isRsToken() && !collectionId.equals(iid.toString())) {
            LOGGER.error("Resource Ids don't match! id- {}, jwtId- {}", collectionId, iid);
            routingContext.put("shouldCreate", false);
            routingContext.next();
            return;
        }

        databaseService
                .getAccess(collectionId)
                .onSuccess(
                        isOpenResource -> {
                            user.setResourceId(UUID.fromString(collectionId));

                            if (isOpenResource && user.isRsToken()) {
                                LOGGER.debug("Resource is open, access granted.");
                                routingContext.put("shouldCreate", true);
                            }
                            else if (!isOpenResource && user.isRsToken()) {
                                LOGGER.debug("Resource is secure, but passed an open token..");
                                routingContext.put("shouldCreate", false);
                            } else {
                                handleSecureResource(routingContext, user, isOpenResource);
                            }
                            routingContext.next();
                        })
                .onFailure(
                        failure -> {
                            LOGGER.error("Failed to retrieve collection access: {}", failure.getMessage());
                            routingContext.put("shouldCreate", false);
                            routingContext.next();
                        });
    }

    private void handleSecureResource(RoutingContext routingContext, AuthInfo user, boolean isOpenResource) {
        if (!isOpenResource) {
            LOGGER.debug("Not an open resource, it's a secure resource.");

            if (user.getRole() == AuthInfo.RoleEnum.provider) {
                routingContext.put("shouldCreate", true);
            } else if (user.getRole() == AuthInfo.RoleEnum.consumer) {
                JsonArray access =
                        user.getConstraints() != null ? user.getConstraints().getJsonArray("access") : null;

                if (access == null || !access.contains("api")) {
                    LOGGER.debug("Invalid consumer token. Constraints not present.");
                    routingContext.put("shouldCreate", false);
                } else {
                    routingContext.put("shouldCreate", true);
                }
            } else {
                LOGGER.debug("Role not recognized: {}", user.getRole());
                routingContext.put("shouldCreate", false);
            }
        } else {
            LOGGER.debug("Resource is open but token is secure for role: {}", user.getRole());
            routingContext.put("shouldCreate", false);
        }
    }
}
