package ogc.rs.processes.util;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static ogc.rs.processes.util.Constants.*;

/**
 * This class provides utility methods for the other classes.
 */
public class UtilClass {
  Logger LOGGER = LogManager.getLogger(UtilClass.class);

  PgPool pgPool;

  public UtilClass(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  /**
   * This method is used to start a new job.
   *
   * @param input the input json object
   * @return the input json object with the job id added
   */
  public Future<JsonObject> startAJobInDB(JsonObject input) {
    Promise<JsonObject> promise = Promise.promise();
    String process_id = input.getString("processId");
    String userId = input.getString("userId");
    JsonObject output_json = input.getJsonObject("outputs");

    pgPool.withConnection(sqlConnection -> sqlConnection.preparedQuery(NEW_JOB_INSERT_QUERY)
      .execute(Tuple.of(UUID.fromString(process_id), UUID.fromString(userId), input, output_json,
        Status.ACCEPTED, PROCESS_ACCEPTED_RESPONSE))).onSuccess(successResult -> {
      for (Row row : successResult) {
        input.put("jobId", row.getUUID("id").toString());
      }
      LOGGER.debug("New Job created.");
      promise.complete(input);
    }).onFailure(failureHandler -> {
      LOGGER.error("Failed to create a job: {}", failureHandler.getMessage());
      promise.fail("Failed to create a job " + failureHandler.getMessage());
    });

    return promise.future();
  }

  /**
   * Updates the status of a job in the database.
   *
   * @param input  the input JSON object
   * @param status the status to update to
   * @return a future that completes when the update is complete
   */
  public Future<Void> updateJobTableStatus(JsonObject input, Status status,String message) {
    Promise<Void> promise = Promise.promise();
    UUID jobId = UUID.fromString(input.getString("jobId"));
    pgPool.withConnection(
      sqlConnection -> sqlConnection.preparedQuery(UPDATE_JOB_TABLE_STATUS_QUERY)
        .execute(Tuple.of(status, message, jobId))).onSuccess(successResult -> {
      LOGGER.debug("Job status updated to {}", status);
      promise.complete();
    }).onFailure(failureHandler -> {
      LOGGER.error("Failed to update job status: {}", failureHandler.getMessage());
      promise.fail("Failed to update job status: " + failureHandler.getMessage());
    });
    return promise.future();
  }

  /**
   * Updates the progress of a job in the database.
   *
   * @param input the input JSON object
   * @return a future that completes when the update is complete
   */
  public Future<Void> updateJobTableProgress(JsonObject input) {

    Promise<Void> promise = Promise.promise();
    float progress = input.getFloat("progress");
    UUID jobId = UUID.fromString(input.getString("jobId"));
    String message = input.getString("message");
    pgPool.withConnection(sqlConnection -> sqlConnection.preparedQuery(UPDATE_JOB_STATUS_PROGRESS)
      .execute(Tuple.of(progress,message, jobId))).onSuccess(successResult -> {
      LOGGER.debug("Job progress: {}", message);
      promise.complete();
    }).onFailure(failureHandler -> {
      LOGGER.error("Failed to update job progress: {}", failureHandler.getMessage());
      promise.fail("Failed to update job progress: " + failureHandler.getMessage());
    });
    return promise.future();
  }


}
