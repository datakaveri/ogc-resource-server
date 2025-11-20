package ogc.rs.processes.auditLogsIngestion;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static ogc.rs.processes.auditLogsIngestion.Constants.*;

/**
 * AuditLogsIngestionProcess ingests S3 or MinIO audit/webhook logs
 * and inserts them into the "metering" table in Postgres.
 * Expected input format:
 * {
 *   "inputs": {
 *     "logs": [
 *       "userId|collectionId|itemId|timestamp|respSize|storageBackend",
 *       "userId|collectionId|itemId|timestamp|respSize|storageBackend"
 *     ]
 *   }
 * }
 */
public class AuditLogsIngestionProcess implements ProcessService {

    private static final Logger LOGGER = LogManager.getLogger(AuditLogsIngestionProcess.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String LOG_DELIMITER = "\\|";  // Pipe delimiter (escaped for regex)

    private final UtilClass utilClass;
    private final PgPool pgPool;

    public AuditLogsIngestionProcess(PgPool pgPool) {
        this.utilClass = new UtilClass(pgPool);
        this.pgPool = pgPool;
    }

    @Override
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        LOGGER.info(STARTING_AUDIT_LOGS_INGESTION_PROCESS_MESSAGE);

        requestInput.put("progress", calculateProgress(1));
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, STARTING_AUDIT_LOGS_INGESTION_PROCESS_MESSAGE)
                .compose(v -> insertLogs(requestInput))
                .compose(insertResult -> {
                    int logsInserted = insertResult.getInteger("logsInserted", 0);
                    String message = String.format("%d log%s inserted into metering table",
                            logsInserted,
                            logsInserted == 1 ? "" : "s");
                    return utilClass.updateJobTableProgress(
                                    requestInput.put("progress", calculateProgress(2))
                                            .put("message", message))
                            .map(insertResult); // Pass the result forward
                })
                .compose(insertResult -> {
                    int logsInserted = insertResult.getInteger("logsInserted", 0);
                    String message = String.format("Completed Audit Logs Ingestion - %d log%s inserted",
                            logsInserted,
                            logsInserted == 1 ? "" : "s");
                    return utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL, message)
                            .map(insertResult); // Pass the result forward
                })
                .onSuccess(insertResult -> {
                    int logsInserted = insertResult.getInteger("logsInserted", 0);
                    LOGGER.info("Audit Logs Ingestion completed successfully - {} log(s) inserted", logsInserted);
                    JsonObject result = new JsonObject()
                            .put("status", "success")
                            .put("message", String.format("%d log%s inserted into metering table",
                                    logsInserted,
                                    logsInserted == 1 ? "" : "s"))
                            .put("logsInserted", logsInserted);
                    promise.complete(result);
                })
                .onFailure(failure -> {
                    LOGGER.error("Audit Logs Ingestion Process failed", failure);
                    handleFailure(requestInput, failure, promise);
                });

        return promise.future();
    }

    /**
     * Handles array of log strings and inserts them using batch query with transaction.
     * Returns a JsonObject with the count of logs inserted.
     * All inserts are atomic - either all succeed or all are rolled back.
     */
    private Future<JsonObject> insertLogs(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();

        try {
            JsonArray logsArray = requestInput.getJsonArray("logs");
            if (logsArray == null || logsArray.isEmpty()) {
                LOGGER.warn("No logs found in inputs");
                promise.complete(new JsonObject().put("logsInserted", 0));
                return promise.future();
            }

            int logsSize = logsArray.size();
            LOGGER.info("Processing {} log entries", logsSize);

            // Parse all logs first
            List<Tuple> batchParams = new ArrayList<>();
            for (int i = 0; i < logsArray.size(); i++) {
                String logString = logsArray.getString(i);
                int logNumber = i + 1;

                try {
                    Tuple params = parseLogEntry(logString, logNumber);
                    batchParams.add(params);
                } catch (OgcException oe) {
                    LOGGER.error("Validation failed for log #{}: {}", logNumber, oe.getMessage());
                    promise.fail(oe);
                    return promise.future();
                } catch (Exception e) {
                    LOGGER.error("Unexpected error parsing log #{}", logNumber, e);
                    promise.fail(new OgcException(500, "Internal Server Error", "Unexpected error while processing log entry"));
                    return promise.future();
                }
            }

            // Execute batch insert within a transaction
            pgPool.withTransaction(conn ->
                            conn.preparedQuery(AUDIT_LOG_INSERTION_QUERY)
                                    .executeBatch(batchParams)
                                    .map(SqlResult::rowCount)
                    )
                    .onSuccess(insertedCount -> {
                        LOGGER.info("Successfully inserted {} log entries in this batch transaction", logsSize);
                        promise.complete(new JsonObject().put("logsInserted", logsSize));
                    })
                    .onFailure(err -> {
                        LOGGER.error("Failed during batch log insertion, transaction rolled back", err);
                        promise.fail(new OgcException(500, "Internal Server Error", AUDIT_LOGS_INSERTION_FAILURE_MESSAGE));
                    });

        } catch (Exception e) {
            LOGGER.error("Error processing logs array", e);
            promise.fail(new OgcException(500, "Internal Server Error", e.getMessage()));
        }

        return promise.future();
    }

    /**
     * Parses a log string and returns a Tuple for batch insertion.
     * Expected format: userId|collectionId|itemId|timestamp|respSize|storageBackend
     */
    private Tuple parseLogEntry(String logString, int logNumber) throws OgcException {
        if (logString == null || logString.trim().isEmpty()) {
            String error = String.format("Log entry #%d is empty", logNumber);
            LOGGER.warn(error);
            throw new OgcException(500, "Internal Server Error", LOG_ENTRY_EMPTY_MESSAGE);
        }

        // Parse the log string
        String[] parts = logString.split(LOG_DELIMITER, -1);
        if (parts.length != 6) {
            String error = String.format(
                    "Invalid log format for entry #%d. Expected 6 fields (userId|collectionId|itemId|timestamp|respSize|storageBackend), got %d: %s",
                    logNumber, parts.length, logString
            );
            LOGGER.error(error);
            throw new OgcException(400, "Bad Request", INVALID_LOG_FORMAT_MESSAGE);
        }

        String userIdStr = parts[0].trim();
        String collectionIdStr = parts[1].trim();
        String itemId = parts[2].trim();
        String timestampStr = parts[3].trim();
        String respSizeStr = parts[4].trim();
        String storageBackend = parts[5].trim();

        // Validate required fields are not empty
        if (userIdStr.isEmpty() || collectionIdStr.isEmpty() ||
                itemId.isEmpty() || timestampStr.isEmpty() || respSizeStr.isEmpty()) {
            String error = String.format(
                    "Missing required fields in log entry #%d: %s",
                    logNumber, logString
            );
            LOGGER.error(error);
            throw new OgcException(400, "Bad Request", MISSING_REQUIRED_FIELDS_MESSAGE);
        }

        // Validate UUIDs
        UUID userId;
        UUID collectionId;
        try {
            userId = UUID.fromString(userIdStr);
            collectionId = UUID.fromString(collectionIdStr);
        } catch (IllegalArgumentException e) {
            String error = String.format(
                    "Invalid UUID format in log entry #%d: userId=%s, collectionId=%s",
                    logNumber, userIdStr, collectionIdStr
            );
            LOGGER.error(error, e);
            throw new OgcException(400, "Bad Request", INVALID_UUID_FORMAT_MESSAGE);
        }

        // Parse respSize
        Long respSize;
        try {
            respSize = Long.parseLong(respSizeStr);
        } catch (NumberFormatException e) {
            String error = String.format(
                    "Invalid respSize format in log entry #%d: %s",
                    logNumber, respSizeStr
            );
            LOGGER.error(error, e);
            throw new OgcException(400, "Bad Request", INVALID_RESPONSE_SIZE_FORMAT_MESSAGE);
        }

        // Parse and format timestamp
        LocalDateTime timestampObj;
        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(timestampStr, ISO_FORMATTER);
            timestampObj = dateTime.toLocalDateTime();
        } catch (DateTimeParseException e) {
            String error = String.format(
                    "Invalid timestamp format in log entry #%d: %s. Expected ISO-8601 format (e.g., 2025-09-08T05:46:24Z)",
                    logNumber, timestampStr
            );
            LOGGER.error(error, e);
            throw new OgcException(400, "Bad Request", INVALID_TIME_STAMP_FORMAT_MESSAGE);
        }

        // Construct API path
        String apiPath = String.format(
                "stac/collections/%s/items/%s",
                collectionIdStr,
                itemId
        );

        LOGGER.debug("Parsed log #{} from storage backend: {}", logNumber, storageBackend);

        // Return Tuple for batch insert
        return Tuple.of(
                userId,
                collectionId,
                apiPath,
                timestampObj,
                respSize
        );
    }

    private void handleFailure(JsonObject requestInput, Throwable failure, Promise<JsonObject> promise) {
        String errorMessage = failure.getMessage();

        utilClass.updateJobTableStatus(requestInput, Status.FAILED, errorMessage)
                .onSuccess(v -> {
                    LOGGER.error("AuditLogsIngestionProcess failed due to: {}", errorMessage);
                    promise.fail(failure);
                })
                .onFailure(updateErr -> {
                    LOGGER.error("Failed to update job status: {}", updateErr.getMessage());
                    promise.fail(updateErr);
                });
    }

    private int calculateProgress(int step) {
        // 3 total steps: start, insert, completion
        return (step * 100) / 3;
    }
}