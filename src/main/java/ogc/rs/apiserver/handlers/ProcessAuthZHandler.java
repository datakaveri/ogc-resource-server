package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.AuthInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.regex.Matcher;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.apiserver.util.Constants.*;
import static ogc.rs.processes.auditLogsIngestion.Constants.AUDIT_LOGS_INGESTION_TITLE;
import static ogc.rs.processes.echo.Constants.ECHO_PROCESS_TITLE;
import static ogc.rs.processes.userDatasetUsageCheck.Constants.USER_DATASET_USAGE_TITLE;

public class ProcessAuthZHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(ProcessAuthZHandler.class);
    // Check if fake token is enabled (set via JVM option -Dfake-token=true)
    private static final boolean FAKE_TOKEN_ENABLED =
            Boolean.parseBoolean(System.getProperty("fake-token", "false"));

    private final PgPool pool;

    /**
     * Constructor for ProcessAuthZHandler
     *
     * @param vertx Vertx instance
     * @param config Configuration containing database credentials
     */
    public ProcessAuthZHandler(Vertx vertx, JsonObject config) {
        String databaseIp = config.getString("databaseHost");
        int databasePort = config.getInteger("databasePort");
        String databaseName = config.getString("databaseName");
        String databaseUserName = config.getString("databaseUser");
        String databasePassword = config.getString("databasePassword");
        int poolSize = config.getInteger("poolSize");

        PgConnectOptions connectOptions =
                new PgConnectOptions()
                        .setPort(databasePort)
                        .setHost(databaseIp)
                        .setDatabase(databaseName)
                        .setUser(databaseUserName)
                        .setPassword(databasePassword)
                        .setReconnectAttempts(5)
                        .setReconnectInterval(1000L)
                        .setTcpKeepAlive(true);

        PoolOptions poolOptions = new PoolOptions().setMaxSize(poolSize);
        this.pool = PgPool.pool(vertx, connectOptions, poolOptions);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.debug("Process Authorization");
        AuthInfo user = routingContext.get(USER_KEY);
        UUID iid = user.getResourceId();
        String requestPath = routingContext.normalizedPath();

        // Extract processId from the request path
        Matcher matcher = PROCESS_ID_PATTERN.matcher(requestPath);
        if (!matcher.find()) {
            LOGGER.debug("Path does not match process execution pattern");
            routingContext.fail(
                    new OgcException(400, "Invalid Request", "Invalid process execution path"));
            return;
        }
        String processIdStr = matcher.group(1);
        UUID processId;
        try {
            processId = UUID.fromString(processIdStr);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid process ID format: {}", processIdStr);
            routingContext.fail(
                    new OgcException(400, "Invalid Process ID", "The provided process ID is not valid"));
            return;
        }

        // Query database to get process title
        String PROCESS_TITLE_QUERY = "SELECT title FROM processes_table WHERE id = $1";
        pool
                .preparedQuery(PROCESS_TITLE_QUERY)
                .execute(
                        Tuple.of(processId),
                        ar -> {
                            if (ar.failed()) {
                                LOGGER.error("Failed to PROCESS_TITLE_QUERY process title: {}", ar.cause().getMessage());
                                routingContext.fail(
                                        new OgcException(
                                                500, "Database Error", "Failed to retrieve process information"));
                                return;
                            }

                            if (ar.result().size() == 0) {
                                LOGGER.error("Process not found with ID: {}", processId);
                                routingContext.fail(
                                        new OgcException(404, "Not Found", "The requested process does not exist"));
                                return;
                            }

                            Row row = ar.result().iterator().next();
                            String processTitle = row.getString("title");
                            LOGGER.debug("Process title for ID {}: {}", processId, processTitle);

                            // Check if this is the echo process - block unless fake-token enabled
                            if (ECHO_PROCESS_TITLE.equals(processTitle)) {
                                if (!FAKE_TOKEN_ENABLED) {
                                    LOGGER.error("Attempted to run Echo process without fake-token=true");
                                    routingContext.fail(
                                            new OgcException(
                                                    403,
                                                    "ForbiddenProcess",
                                                    "The echo process is only available for testing purposes with fake-token enabled"));
                                    return;
                                }
                            }

                            // Perform authorization based on process title and user role
                            performAuthorization(routingContext, user, iid, processTitle);
                        });
    }

    private void performAuthorization(
            RoutingContext routingContext, AuthInfo user, UUID iid, String processTitle) {

        // Check if this is AuditLogsIngestion process - ONLY RS admin allowed
        if (AUDIT_LOGS_INGESTION_TITLE.equals(processTitle)) {
            if (user.getRole() == AuthInfo.RoleEnum.admin && user.isRsToken()) {
                LOGGER.debug("RS admin authorized for AuditLogsIngestion process");
                JsonObject results = new JsonObject();
                results.put("userId", user.getUserId());
                results.put("role", user.getRole());
                routingContext.data().put("authInfo", results);
                routingContext.next();
            } else {
                LOGGER.error(
                        "Only RS admin is authorized to execute the AuditLogsIngestion process. " +
                                "Unauthorized access to AuditLogsIngestion. Role: {}, isRsToken: {}",
                        user.getRole(),
                        user.isRsToken());
                routingContext.fail(
                        new OgcException(
                                401,
                                "Not Authorized",
                                "Only RS admin is authorized to execute the AuditLogsIngestion process. Please contact DX AAA"));
            }
            return;
        }

        // Check if this is UserDatasetUsageCheck process - ONLY COS admin allowed
        if (USER_DATASET_USAGE_TITLE.equals(processTitle)) {
            if (user.getRole() == AuthInfo.RoleEnum.cos_admin) {
                LOGGER.debug("COS admin authorized for UserDatasetUsageCheck process");
                JsonObject results = new JsonObject();
                results.put("userId", user.getUserId());
                results.put("role", user.getRole());
                routingContext.data().put("authInfo", results);
                routingContext.next();
            } else {
                LOGGER.error("Only COS admin is authorized to execute the UserDatasetUsageCheck process." +
                                " But, user with role {} attempted to execute this process: {}",
                        user.getRole(),
                        processTitle);
                routingContext.fail(
                        new OgcException(
                                401,
                                "Not Authorized",
                                "Only COS admin is authorized to execute the UserDatasetUsageCheck process. Please contact DX AAA"));
            }
            return;
        }

        // For all other processes - ONLY providers and provider delegates allowed
        // Admins (both RS and COS) are NOT allowed to execute provider processes
        LOGGER.debug("Validating access for non-admin process: {}", processTitle);

        if (!user.isRsToken()) {
            LOGGER.error("Token is not an RS token. Resource Ids don't match!");
            routingContext.fail(
                    new OgcException(401, "Not Authorized", "User is not authorised. Please contact DX AAA"));
            return;
        }

        if (user.getRole() == AuthInfo.RoleEnum.provider) {
            JsonObject results = new JsonObject();
            results.put("iid", iid);
            results.put("userId", user.getUserId());
            results.put("role", user.getRole());
            routingContext.data().put("authInfo", results);
            routingContext.next();
        } else if (user.getRole() == AuthInfo.RoleEnum.delegate
                && user.getDelegatorRole() == AuthInfo.RoleEnum.provider) {
            JsonObject results = new JsonObject();
            results.put("iid", iid);
            results.put("userId", user.getDelegatorUserId());
            results.put("role", user.getDelegatorRole());
            routingContext.data().put("authInfo", results);
            routingContext.next();
        } else {
            LOGGER.error(
                    "Only providers and provider delegates are authorized to execute this process. But, user with role {} attempted to execute this process: {}",
                    user.getRole(),
                    processTitle);
            routingContext.fail(
                    new OgcException(401, "Not Authorized", "Only providers and provider delegates are authorized to execute this process. Please contact DX AAA "));
        }
    }
}