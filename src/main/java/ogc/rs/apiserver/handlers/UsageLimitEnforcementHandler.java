package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.apiserver.util.Constants.*;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;

/**
 * This HTTP request handler enforces data and API usage limits per user and collection.
 * <p>
 * Limits are extracted from the user's JWT token and can include either:
 * - Data usage limit (e.g., "100:mb")
 * - API hit limit (e.g., 100)
 * <p>
 * The limits are applied from the policy issued-at timestamp (iat) defined in the token.
 */
public class UsageLimitEnforcementHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(UsageLimitEnforcementHandler.class);
    Vertx vertx;
    private final DatabaseService databaseService;

    /**
     * Constructs the handler with a Vert.x instance and initializes a proxy for DatabaseService.
     *
     * @param vertx Vertx instance used to create service proxies.
     */
    public UsageLimitEnforcementHandler(Vertx vertx) {
        this.vertx = vertx;
        this.databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    }

    /**
     * Intercepts each request and checks usage limits based on the user's token.
     * If the user has exceeded their allowed quota, the request is denied with 429 status.
     *
     * @param routingContext Context of the current HTTP request.
     */
    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.debug("Usage Limit Enforcement Handler invoked");

        AuthInfo user = routingContext.get(USER_KEY);
        String userId = user.getRole() == AuthInfo.RoleEnum.delegate
                ? user.getDelegatorUserId().toString()
                : user.getUserId().toString();
        String collectionId = user.getResourceId().toString();
        String apiPath = routingContext.normalizedPath();

        JsonObject limits = user.getConstraints().getJsonObject("limits");
        if (limits == null) {
            routingContext.next();
            return;
        }

        long policyIssuedAt = limits.getLong("iat", 0L);
        LOGGER.info("policyIssuedAt: {}: ", policyIssuedAt);

        boolean constraintHandled = false;

        for (String key : limits.fieldNames()) {
            switch (key) {
                case "dataUsage":
                    String dataUsageLimit = limits.getString("dataUsage");
                    long limitInBytes = convertToBytes(dataUsageLimit);

                    databaseService.getTotalDataUsage(userId, apiPath, collectionId, policyIssuedAt)
                            .onSuccess(dataUsage -> {
                                if (dataUsage > limitInBytes) {
                                    routingContext.fail(new OgcException(429, TOO_MANY_REQUESTS, DATA_USAGE_LIMIT_EXCEEDED));
                                } else {
                                    routingContext.next();
                                }
                            }).onFailure(fail -> {
                                LOGGER.error("Error checking data usage: {}", fail.getMessage());
                                routingContext.fail(fail);
                            });

                    constraintHandled = true;
                    break;

                case "apiHits":
                    long apiHitsLimit = limits.getLong("apiHits");

                    databaseService.getTotalApiHits(userId, apiPath, collectionId, policyIssuedAt)
                            .onSuccess(apiHits -> {
                                if (apiHits > apiHitsLimit) {
                                    routingContext.fail(new OgcException(429, TOO_MANY_REQUESTS, API_CALLS_LIMIT_EXCEEDED));
                                } else {
                                    routingContext.next();
                                }
                            }).onFailure(fail -> {
                                LOGGER.error("Error checking API hits: {}", fail.getMessage());
                                routingContext.fail(fail);
                            });

                    constraintHandled = true;
                    break;

                default:
                    LOGGER.warn("Unknown usage constraint key: {}", key);
                    break;
            }

            if (constraintHandled) {
                // Stop after handling the first supported constraint.
                return;
            }
        }

        // If no known constraint keys were handled
        routingContext.next();
    }

    /**
    * Converts a human-readable data limit string (e.g., "100:mb") into bytes.
    *
    * @param limit A string in the format "<value>:<unit>", e.g., "500:mb"
    * @return Limit in bytes
    * @throws OgcException if the unit is invalid or unsupported
    */
    private long convertToBytes(String limit) {
        String[] parts = limit.split(":");
        long value = Long.parseLong(parts[0]);
        String unit = parts[1].toLowerCase();

        switch (unit) {
            case "kb":
                return value * 1024L;
            case "mb":
                return value * 1024L * 1024;
            case "gb":
                return value * 1024L * 1024 * 1024;
            case "tb":
                return value * 1024L * 1024 * 1024 * 1024;
            default:
                throw new OgcException(400, "Bad Request", "Invalid data usage unit: " + unit);
        }
    }
}
