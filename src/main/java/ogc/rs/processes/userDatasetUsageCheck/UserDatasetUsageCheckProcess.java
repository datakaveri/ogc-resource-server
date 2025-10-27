package ogc.rs.processes.userDatasetUsageCheck;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.processes.ProcessService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * UserDatasetUsageCheckProcess

 * This process checks if a user has ever accessed/used a given dataset (collection).
 * It queries the metering table using the user_id and collection_id and returns:
 * - {"used": false} if the user has never used the dataset
 * - {"used": true, "last_used": "<UTC timestamp>"} if user has used the dataset

 * Expected input JSON:
 * {
 *   "user_id": "<UUID>",
 *   "collection_id": "<UUID>"
 * }
 */
public class UserDatasetUsageCheckProcess implements ProcessService {

    private static final Logger LOGGER = LogManager.getLogger(UserDatasetUsageCheckProcess.class);
    private final PgPool client;

    public UserDatasetUsageCheckProcess(PgPool client) {
        this.client = client;
    }

    @Override
    public Future<JsonObject> execute(JsonObject input) {
        Promise<JsonObject> promise = Promise.promise();
        LOGGER.info("Starting User Dataset Usage Check Process...");

        String userId = input.getString("user_id");
        String collectionId = input.getString("collection_id");

        String query =
                "SELECT MAX(timestamp) AS last_used "
                        + "FROM metering "
                        + "WHERE user_id = $1 AND collection_id = $2";

        client
                .preparedQuery(query)
                .execute(Tuple.of(userId, collectionId))
                .onSuccess(rows -> handleQueryResult(rows, promise))
                .onFailure(
                        err -> {
                            LOGGER.error("Failed to check user dataset usage", err);
                            promise.fail(new OgcException(500, "Internal Server Error", "Database error during user data set usage check."));
                        });

        return promise.future();
    }

    private void handleQueryResult(RowSet<Row> rows, Promise<JsonObject> promise) {
        if (!rows.iterator().hasNext()) {
            promise.complete(new JsonObject().put("used", false));
            return;
        }

        Row row = rows.iterator().next();
        LocalDateTime lastUsed = row.getLocalDateTime("last_used");

        if (lastUsed == null) {
            promise.complete(new JsonObject().put("used", false));
        } else {
            // Convert LocalDateTime to UTC Instant
            Instant utcInstant = lastUsed.toInstant(ZoneOffset.UTC);
            promise.complete(
                    new JsonObject()
                            .put("used", true)
                            .put("last_used", utcInstant.toString())); // UTC ISO-8601 format
        }
    }

}
