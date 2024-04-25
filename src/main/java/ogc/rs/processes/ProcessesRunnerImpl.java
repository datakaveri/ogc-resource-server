package ogc.rs.processes;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import ogc.rs.common.DataFromS3;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.UUID;

import static ogc.rs.common.Constants.processException404;
import static ogc.rs.common.Constants.processException500;
import static ogc.rs.processes.util.Constants.PROCESS_EXIST_CHECK_QUERY;

public class ProcessesRunnerImpl implements ProcessesRunnerService {
  private final PgPool pgPool;
  private final WebClient webClient;
  private final UtilClass utilClass;
  private final JsonObject config;
  private final Vertx vertx;
  private final DataFromS3 dataFromS3;
  Logger LOGGER = LogManager.getLogger(ProcessesRunnerImpl.class);

  ProcessesRunnerImpl(PgPool pgPool, WebClient webClient, JsonObject config, DataFromS3 dataFromS3, Vertx vertx) {
    this.pgPool = pgPool;
    this.webClient = webClient;
    this.utilClass = new UtilClass(pgPool);
    this.config = config;
    this.dataFromS3=dataFromS3;
    this.vertx=vertx;
  }

  @Override
  public ProcessesRunnerService run(JsonObject input, Handler<AsyncResult<JsonObject>> handler) {
    Promise<JsonObject> executeMethodPromise = Promise.promise();

    Future<JsonObject> checkForProcess = processExistCheck(input);

    checkForProcess.onSuccess(processExist -> {

      String processName = processExist.getString("title");
      boolean validateInput = validateInput(input, processExist);
      if (validateInput) {
        ProcessService processService = null;

        switch (processName) {
          case "CollectionOnboarding":
            processService = new CollectionOnboardingProcess(pgPool, webClient, config,dataFromS3,vertx);
            break;
        }

        ProcessService finalProcessService = processService;
        Future<JsonObject> startAJobInDB = utilClass.startAJobInDB(input);

        startAJobInDB.onSuccess(jobStarted -> {
          LOGGER.info("Job started in DB with jobId {} ", jobStarted.getValue("jobId"),
            " for process with processId {}", input.getString("processId"));
          handler.handle(Future.succeededFuture(
            new JsonObject().put("jobId", jobStarted.getValue("jobId"))
              .put("processId", input.getString("processId")).put("type", "PROCESS")
              .put("status", Status.ACCEPTED).put("location",
                config.getString("hostName").concat("/jobs/")
                  .concat(jobStarted.getString("jobId")))));
        }).onFailure(jobFailed -> handler.handle(Future.failedFuture(jobFailed.getMessage())));

        startAJobInDB.compose(updatedInputJson -> finalProcessService.execute(updatedInputJson))
          .onSuccess(executeMethodPromise::complete)
          .onFailure(failureHandler -> {
            executeMethodPromise.fail(failureHandler.getMessage());;
        });
      } else {
        LOGGER.error("Failed to validate the input");
        handler.handle(Future.failedFuture(processException500));
      }
    }).onFailure(processNotExist -> {
      handler.handle(Future.failedFuture(processNotExist));
    });
    return this;
  }

  /**
   * This method is used to check if the process exists or not.
   *
   * @param input the input json
   * @return a future with the result
   */
  private Future<JsonObject> processExistCheck(JsonObject input) {
    Promise<JsonObject> promise = Promise.promise();

    pgPool.withConnection(sqlConnection -> sqlConnection.preparedQuery(PROCESS_EXIST_CHECK_QUERY)
        .execute(Tuple.of(UUID.fromString(input.getString("processId")))))
      .onSuccess(successResult -> {
        if (successResult.size() > 0) {
          Row row = successResult.iterator().next();
          JsonObject rowJson = row.toJson();
          promise.complete(rowJson);
        } else {
          LOGGER.error("Process does not exist");
          promise.fail(processException404);
        }
      }).onFailure(failureHandler -> {
        LOGGER.error("Failed to check process in database {}",failureHandler.getMessage());
        promise.fail(processException500);
      });

    return promise.future();
  }

  /**
   * This method is used to validate the input.
   *
   * @param requestBody   contains the inputs of the request
   * @param processInputs the process input json
   * @return true if the input is valid, false otherwise
   */
  private boolean validateInput(JsonObject requestBody, JsonObject processInputs) {
    Set<String> processInputKeys = processInputs.getJsonObject("input", new JsonObject())
      .getJsonObject("inputs", new JsonObject()).getMap().keySet();
    return processInputKeys.stream().allMatch(
      inputKey -> requestBody.containsKey(inputKey) && !requestBody.getString(inputKey).isEmpty());
  }


}
