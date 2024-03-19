package ogc.rs.processes;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.Set;
import java.util.UUID;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessesRunnerImpl implements ProcessesRunnerService {
  private final PgPool pgPool;
  private final WebClient webClient;
  private final UtilClass utilClass;
  private final JsonObject config;
  Logger LOGGER = LogManager.getLogger(ProcessesRunnerImpl.class);

  ProcessesRunnerImpl(PgPool pgPool, WebClient webClient, JsonObject config) {
    this.pgPool = pgPool;
    this.webClient = webClient;
    this.utilClass = new UtilClass(pgPool);
    this.config = config;
    //create objects of all the processes
  }

  @Override
  public ProcessesRunnerService run(JsonObject input, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.info("in process runner "+input);
    Promise<JsonObject> jsonObjectPromise = Promise.promise();

    Future<JsonObject> checkForProcess = processExistCheck(input);

    checkForProcess.onSuccess(processExist -> {

      String processName = processExist.getString("title");
      boolean validateInput = validateInput(input, processExist);
      if (validateInput) {
        ProcessService processService = null;

        switch (processName) {
          case "CollectionOnboarding":
            processService = new CollectionOnboardingProcess(pgPool, webClient, config);
            break;
        }

        ProcessService finalProcessService = processService;
        Future<JsonObject> startAJobInDB = utilClass.startAJobInDB(input);
        startAJobInDB.onSuccess(jobStarted -> {
          LOGGER.info("job started successfully");
          handler.handle(Future.succeededFuture(
            new JsonObject().put("jobId", jobStarted.getValue("job_id"))
              .put("processId", input.getString("processId")).put("type", "PROCESS")
              .put("status", Status.ACCEPTED).put("location",
                config.getString("hostName").concat("/jobs/")
                  .concat(jobStarted.getString("job_id")))));
        }).onFailure(jobFailed -> handler.handle(Future.failedFuture("failed to start the job")));

        Vertx.vertx().executeBlocking(blockCode -> {
          Future<JsonObject> jobObjectFuture = startAJobInDB.compose(updatedInputJson -> {
            try {
              return finalProcessService.execute(updatedInputJson);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });

          jobObjectFuture.onSuccess(blockCode::complete).onFailure(failureHandler -> {
            LOGGER.error("FAILED IN BLOCKING CODE " + failureHandler.getMessage());
            blockCode.fail("failed");
          });
        }, blockResult -> {
          if (blockResult.failed()) {
            jsonObjectPromise.fail(blockResult.cause());
          }else
            jsonObjectPromise.complete((JsonObject) blockResult.result());
        });
        // Step1 : get the process id from i/p and use switch case to create obj of that process.
        // step2 : execute the process using execute() from that class
      } else {
        LOGGER.error("Failed to validate the input");
        handler.handle(Future.failedFuture("Invalid Input"));
      }
    }).onFailure(processNotExist -> {
      LOGGER.error("Process does not exist.");
      handler.handle(Future.failedFuture("Process not found."));
    });
    return this;
  }

  private Future<JsonObject> processExistCheck(JsonObject input) {
    Promise<JsonObject> promise = Promise.promise();

    pgPool.withConnection(
        sqlConnection -> sqlConnection.preparedQuery("SELECT * FROM PROCESSES_TABLE WHERE ID=$1")
          .execute(Tuple.of(UUID.fromString(input.getString("processId")))))
      .onSuccess(successResult -> {
        if (successResult.size() > 0) {
          Row row = successResult.iterator().next();
          JsonObject rowJson = row.toJson();
          LOGGER.info(
            "ROW " + rowJson.getJsonObject("input").getJsonObject("inputs").getMap().keySet());
          promise.complete(rowJson);
        } else {
          LOGGER.warn("Process not found.");
          promise.fail("Process not found.");
        }
      }).onFailure(failureHandler -> {
        LOGGER.error("FailResult " + failureHandler.toString());
        promise.fail("Failed to accept the process" + failureHandler);
      });

    return promise.future();
  }

  private boolean validateInput(JsonObject input, JsonObject processInputs) {
    Set<String> processInputKeys = processInputs.getJsonObject("input", new JsonObject())
      .getJsonObject("inputs", new JsonObject()).getMap().keySet();
    return processInputKeys.stream()
      .allMatch(inputKey -> input.containsKey(inputKey) && !input.getString(inputKey).isEmpty());
  }


}
