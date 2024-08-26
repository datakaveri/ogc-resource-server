package ogc.rs.processes;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import ogc.rs.common.DataFromS3;
import ogc.rs.processes.collectionAppending.CollectionAppendingProcess;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.tilesMetaDataOnboarding.TilesMetaDataOnboardingProcess;
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
  private String S3_BUCKET;
  private String S3_REGION;
  private String S3_ACCESS_KEY;
  private String S3_SECRET_KEY;
  private HttpClientOptions httpClientOptions;
  private HttpClient httpClient;
  Logger LOGGER = LogManager.getLogger(ProcessesRunnerImpl.class);

  ProcessesRunnerImpl(PgPool pgPool, WebClient webClient, JsonObject config, Vertx vertx) {
    this.pgPool = pgPool;
    this.webClient = webClient;
    this.utilClass = new UtilClass(pgPool);
    this.config = config;
    this.vertx=vertx;
  }
  private DataFromS3 getS3Object(JsonObject config){
    S3_BUCKET = config.getString("s3BucketUrl");
    S3_REGION = config.getString("awsRegion");
    S3_ACCESS_KEY = config.getString("awsAccessKey");
    S3_SECRET_KEY = config.getString("awsSecretKey");

    httpClientOptions = new HttpClientOptions().setSsl(true);
    if(System.getProperty("s3.mock") != null){
      LOGGER.fatal("S3 is being mocked!! Are you testing something?");
      httpClientOptions.setTrustAll(true).setVerifyHost(false);
    }
    httpClient = vertx.createHttpClient(httpClientOptions);
    DataFromS3 dataFromS3 =
            new DataFromS3(httpClient, S3_BUCKET, S3_REGION, S3_ACCESS_KEY, S3_SECRET_KEY);
    return dataFromS3;
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
            processService = new CollectionOnboardingProcess(pgPool, webClient, config,getS3Object(config),vertx);
            break;
          case "CollectionAppending":
            processService = new CollectionAppendingProcess(pgPool, webClient, config,getS3Object(config),vertx);
            break;
          case "TilesMetaDataOnboarding":
            processService = new TilesMetaDataOnboardingProcess(pgPool, webClient, config,getS3Object(config), vertx);
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
