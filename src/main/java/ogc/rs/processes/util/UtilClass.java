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

public class UtilClass {
  Logger LOGGER = LogManager.getLogger(UtilClass.class);

  PgPool pgPool;
  public UtilClass(PgPool pgPool){
    this.pgPool=pgPool;
  }

  public Future<JsonObject> startAJobInDB(JsonObject input){
    Promise<JsonObject> promise= Promise.promise();

    input.put("user_id","dd3551b3-77a3-4da2-bb3c-92ca7d54a5ab");
    String process_id = input.getString("processId");
    String user_id = input.getString("user_id");
    JsonObject output_json = input.getJsonObject("outputs");

    pgPool.withConnection(sqlConnection -> sqlConnection.
      preparedQuery("INSERT INTO JOBS_TABLE (process_id,user_id,created_at," +
        "updated_at,input,output,progress,status,type,message) VALUES($1,$2,NOW(),NOW()," +
        "$3,$4,'0.0',$5,'PROCESS',$6) RETURNING ID,status;").
      execute(Tuple.of(UUID.fromString(process_id),UUID.fromString(user_id),input,output_json,
        Status.ACCEPTED,Status.ACCEPTED.message))).onSuccess(successResult->{
      for(Row row: successResult){
        input.put("job_id",row.getUUID("id").toString());
      }
      promise.complete(input);
    }).onFailure(failureHandler->{
      LOGGER.error("FailResult "+failureHandler.toString());
      promise.fail("failed to accept the process"+failureHandler.toString());
    });

    return promise.future();
  }

  public Future<Void> updateJobTableStatus(JsonObject input, Status status) {
    LOGGER.info("Inside update table for "+status.message);
    input.put("message","updated msg");
    Promise<Void> promise = Promise.promise();
    UUID jobId = UUID.fromString(input.getString("job_id"));
    pgPool.withConnection(sqlConnection ->
      sqlConnection.preparedQuery(
          "UPDATE JOBS_TABLE\n" +
            "SET\n" +
            "    UPDATED_AT = NOW(),\n" +
            "    STARTED_AT = CASE\n" +
            "                    WHEN $1 = 'RUNNING' THEN NOW()\n" +
            "                    ELSE STARTED_AT\n" +
            "                 END,\n" +
            "    FINISHED_AT = CASE\n" +
            "                     WHEN $1 IN ('FAILED', 'SUCCESSFUL') THEN NOW()\n" +
            "                     ELSE NULL\n" +
            "                 END,\n" +
            "    PROGRESS = CASE\n" +
            "                   WHEN $1 = 'SUCCESSFUL' THEN 100.0\n" +
            "                   WHEN $1 = 'RUNNING' THEN 25.0\n" +
            "                   ELSE PROGRESS\n" +
            "               END,\n" +
            "    STATUS = $1::JOB_STATUS_TYPE,\n" +
            "    MESSAGE = $2\n" +
            "WHERE\n" +
            "    ID = $3;\n"
        )
        .execute(Tuple.of(status,status.message,jobId))
    ).onSuccess(successResult -> {
      LOGGER.info("Insertion into db complete " + status.message);
      promise.complete();
    }).onFailure(failureHandler -> {
      LOGGER.error("FailResult " + failureHandler.toString());
      promise.fail("failed to accept the process" + failureHandler);
    });
    return promise.future();
  }

  public Future<Void> updateJobTableProgress(JsonObject input) {
    LOGGER.info("Inside update table for progress update "+input.getFloat("progress"));

    Promise<Void> promise = Promise.promise();
    float progress = input.getFloat("progress");
    UUID jobId = UUID.fromString(input.getString("job_id"));
    pgPool.withConnection(sqlConnection ->
      sqlConnection.preparedQuery(
          "UPDATE JOBS_TABLE " +
            "SET PROGRESS = $1 " +
            "WHERE ID = $2;"
        )
        .execute(Tuple.of(progress,jobId))
    ).onSuccess(successResult -> {
      LOGGER.info("Progress update into db complete.");
      promise.complete();
    }).onFailure(failureHandler -> {
      LOGGER.error("FailResult " + failureHandler.getLocalizedMessage());
      promise.fail("failed to update the progress");
    });
    return promise.future();
  }


}
