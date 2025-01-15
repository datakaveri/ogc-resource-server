package ogc.rs.processes;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import ogc.rs.common.DataFromS3;
import ogc.rs.common.S3Config;
import ogc.rs.processes.collectionAppending.CollectionAppendingProcess;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.postPresignedUrlForStacOnboarding.S3PresignedPostUrlGenerationProcess;
import ogc.rs.processes.tilesMetaDataOnboarding.TilesMetaDataOnboardingProcess;
import ogc.rs.processes.s3PreSignedURLGeneration.S3PreSignedURLGenerationProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import java.util.UUID;
import static ogc.rs.common.Constants.processException404;
import static ogc.rs.common.Constants.processException500;
import static ogc.rs.processes.util.Constants.PROCESS_EXIST_CHECK_QUERY;

/**
 * The {@code ProcessesRunnerImpl} class implements the {@link ProcessesRunnerService} interface
 * to handle the execution of various processes. It validates the input, checks for the existence
 * of the process, and then executes it either asynchronously or synchronously.
 */
public class ProcessesRunnerImpl implements ProcessesRunnerService {

  private final PgPool pgPool;
  private final WebClient webClient;
  private final UtilClass utilClass;
  private final JsonObject config;
  private final Vertx vertx;
  Logger LOGGER = LogManager.getLogger(ProcessesRunnerImpl.class);

  /**
   * Constructs a new {@code ProcessesRunnerImpl} instance.
   *
   * @param pgPool    the PostgreSQL pool for database operations
   * @param webClient the web client used for making HTTP requests
   * @param config    the configuration object
   * @param vertx     the Vert.x instance
   */
  ProcessesRunnerImpl(PgPool pgPool, WebClient webClient, JsonObject config, Vertx vertx) {
    this.pgPool = pgPool;
    this.webClient = webClient;
    this.utilClass = new UtilClass(pgPool);
    this.config = config;
    this.vertx = vertx;
  }

  /**
   * Returns an instance of {@link DataFromS3} for interacting with S3.
   *
   * @param config the configuration object containing S3 settings
   * @return a {@code DataFromS3} instance
   */
  private DataFromS3 getS3Object(JsonObject config) {
    S3Config s3conf = new S3Config.Builder()
        .endpoint(config.getString(S3Config.ENDPOINT_CONF_OP))
        .bucket(config.getString(S3Config.BUCKET_CONF_OP))
        .region(config.getString(S3Config.REGION_CONF_OP))
        .accessKey(config.getString(S3Config.ACCESS_KEY_CONF_OP))
        .secretKey(config.getString(S3Config.SECRET_KEY_CONF_OP))
        .pathBasedAccess(config.getBoolean(S3Config.PATH_BASED_ACC_CONF_OP))
        .build();

    HttpClient httpClient = vertx.createHttpClient();
    return new DataFromS3(httpClient, s3conf);
  }

  /**
   * Runs the specified process by validating input and starting the process either synchronously
   * or asynchronously, depending on the process type.
   *
   * @param input   the input JSON containing process details
   * @param handler the handler to manage async results
   * @return the current instance of {@code ProcessesRunnerService}
   */
  @Override
  public ProcessesRunnerService run(JsonObject input, Handler<AsyncResult<JsonObject>> handler) {

    Future<JsonObject> checkForProcess = processExistCheck(input);

    checkForProcess.onSuccess(processExist -> {
      String processName = processExist.getString("title");
      boolean isAsync = processExist.getJsonArray("response")
              .stream()
              .anyMatch(item -> item.toString().equalsIgnoreCase("ASYNC"));
      boolean validateInput = validateInput(input, processExist);

      if (validateInput) {
        ProcessService processService;

        // Switch case to handle different processes
        switch (processName) {
          case "CollectionOnboarding":
            processService = new CollectionOnboardingProcess(pgPool, webClient, config, getS3Object(config), vertx);
            break;
          case "CollectionAppending":
            processService = new CollectionAppendingProcess(pgPool, webClient, config, getS3Object(config), vertx);
            break;
          case "S3PreSignedURLGeneration":
            processService = new S3PreSignedURLGenerationProcess(pgPool, webClient, config);
            break;
          case "S3PresignedPostUrlGeneration":
            processService = new S3PresignedPostUrlGenerationProcess(pgPool, webClient, config, getS3Object(config), vertx);
            break;
          case "TilesMetaDataOnboarding":
            processService = new TilesMetaDataOnboardingProcess(pgPool, webClient, config, getS3Object(config), vertx);
            break;
          default:
            LOGGER.error("No method specified for process {}", processName);
            handler.handle(Future.failedFuture("Process could not be executed: no method specified for process " + processName));
            return;
        }

        ProcessService finalProcessService = processService;
        Future<JsonObject> startAJobInDB = utilClass.startAJobInDB(input);

        startAJobInDB.onSuccess(jobStarted -> {
          if (isAsync) {
            // Handle async process
            LOGGER.info("Async Job started in DB with jobId {} for process with processId {}",
                    jobStarted.getValue("jobId"), input.getString("processId"));
            handler.handle(Future.succeededFuture(
                    new JsonObject()
                            .put("jobId", jobStarted.getValue("jobId"))
                            .put("processId", input.getString("processId"))
                            .put("type", "PROCESS")
                            .put("status", Status.ACCEPTED)
                            .put("location",
                                    config.getString("hostName")
                                            .concat("/jobs/")
                                            .concat(jobStarted.getString("jobId")))
            ));
            finalProcessService.execute(input); // Start async process
          } else {
            // Handle sync process
            LOGGER.info("Sync Job started in DB with jobId {} for process with processId {}",
                    jobStarted.getValue("jobId"), input.getString("processId"));
            finalProcessService.execute(input).onSuccess(result -> {
              JsonObject response = result.copy();
              response.put("sync", "true");
              response.put("status", Status.SUCCESSFUL);
              response.put("location",
                      config.getString("hostName")
                              .concat("/jobs/")
                              .concat(jobStarted.getString("jobId")));
              handler.handle(Future.succeededFuture(response));
            }).onFailure(failureHandler -> handler.handle(Future.failedFuture(failureHandler)));
          }
        }).onFailure(jobFailed -> handler.handle(Future.failedFuture(jobFailed.getMessage())));

      } else {
        LOGGER.error("Failed to validate the input");
        handler.handle(Future.failedFuture(processException500));
      }
    }).onFailure(processNotExist -> handler.handle(Future.failedFuture(processNotExist)));
    return this;
  }

  /**
   * Checks if the process exists in the database.
   *
   * @param input the input JSON containing processId
   * @return a future containing the process details if it exists, or an error if it doesn't
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
              LOGGER.error("Failed to check process in database {}", failureHandler.getMessage());
              promise.fail(processException500);
            });

    return promise.future();
  }

  /**
   * Validates the input for a specific process by checking if all required fields are present
   * and non-empty.
   *
   * @param requestBody   the input JSON containing request data
   * @param processInputs the JSON containing the required input fields for the process
   * @return {@code true} if the input is valid, {@code false} otherwise
   */
  private boolean validateInput(JsonObject requestBody, JsonObject processInputs) {
    Set<String> processInputKeys = processInputs.getJsonObject("input", new JsonObject())
            .getJsonObject("inputs", new JsonObject()).getMap().keySet();
    return processInputKeys.stream().allMatch(
            inputKey -> requestBody.containsKey(inputKey) && !requestBody.getString(inputKey).isEmpty());
  }

}
