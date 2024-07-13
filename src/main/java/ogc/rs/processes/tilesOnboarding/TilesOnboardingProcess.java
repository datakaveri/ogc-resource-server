package ogc.rs.processes.tilesOnboarding;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.math.BigInteger;
import static ogc.rs.processes.tilesOnboarding.Constants.*;
import ogc.rs.common.DataFromS3;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;


/**
 * This class handles the tiles onboarding process.
 */

public class TilesOnboardingProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(TilesOnboardingProcess.class);
    private final Vertx vertx;
    private final PgPool pgPool;
    private final UtilClass utilClass;
    private final CollectionOnboardingProcess collectionOnboarding;
    private final DataFromS3 dataFromS3;
    private String databaseName;
    private String databaseHost;
    private String databasePort;
    private String databaseUser;
    private String databasePassword;

    /**
     * Constructs a TilesOnboardingProcess.
     *
     * @param pgPool       the PostgreSQL pool
     * @param webClient    the WebClient
     * @param config       the configuration
     * @param dataFromS3   the DataFromS3 instance
     * @param vertx        the Vertx instance
     */

    public TilesOnboardingProcess(PgPool pgPool, WebClient webClient, JsonObject config, DataFromS3 dataFromS3, Vertx vertx){
        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
        this.collectionOnboarding = new CollectionOnboardingProcess(pgPool, webClient, config, dataFromS3, vertx);
        this.dataFromS3=dataFromS3;
        this.vertx = vertx;
        initializeConfig(config);
    }

    /**
     * Initializes the configuration.
     *
     * @param config the configuration
     */

    private void initializeConfig(JsonObject config){
        this.databaseName = config.getString("databaseName");
        this.databaseHost = config.getString("databaseHost");
        this.databasePort = config.getString("databasePort");
        this.databaseUser = config.getString("databaseUser");
        this.databasePassword = config.getString("databasePassword");
    }

    /**
     * Executes the tiles onboarding process.
     *
     * @param requestInput the input JSON object
     * @return a Future containing the result JSON object
     */

    public Future<JsonObject> execute(JsonObject requestInput){
        Promise<JsonObject> promise = Promise.promise();
        String collectionId = requestInput.getString("collectionId");
        String tileMatrixSet = requestInput.getString("tileMatrixSet");
        String fileName = collectionId + "/" + tileMatrixSet +"/";
        requestInput.put("fileName",fileName);
        requestInput.put("progress",calculateProgress(1,6));
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING,START_TILES_ONBOARDING_PROCESS)
           .compose(progressUpdateHandler-> checkFileExistenceInS3(requestInput))
                .compose(s3FileExistenceHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2, 6)).put("message", S3_FILE_EXISTENCE_MESSAGE)))
                .compose(progressUpdateHandler -> collectionOnboarding.makeCatApiRequest(requestInput))
                .compose(resourceOwnershipHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress",calculateProgress(3,6)).put("message", RESOURCE_OWNERSHIP_CHECK_MESSAGE)))
                .compose(progressUpdateHandler -> checkCollectionType(requestInput))
                .compose(checkCollectionTypeHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(4, 6)).put("message", COLLECTION_TYPE_CHECK_MESSAGE)))
                .onSuccess(successHandler -> {
                    LOGGER.debug(TILES_ONBOARDING_SUCCESS_MESSAGE);
                    promise.complete();
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(TILES_ONBOARDING_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler.getMessage(), promise);
                });
        return promise.future();
    }

    /**
     * Checks if a file exists in S3.
     *
     * @param requestInput the input JSON object
     * @return a Future containing a boolean indicating file existence
     */

    public Future<Boolean> checkFileExistenceInS3(JsonObject requestInput) {
        Promise<Boolean> promise = Promise.promise();
        String fileName = requestInput.getString("fileName");
        String urlString = dataFromS3.getFullyQualifiedUrlString(fileName);
        dataFromS3.setUrlFromString(urlString);
        dataFromS3.setSignatureHeader(HttpMethod.HEAD);
        dataFromS3.getDataFromS3(HttpMethod.HEAD)
                .onSuccess(responseFromS3 -> {
                    BigInteger fileSize = new BigInteger(responseFromS3.getHeader("Content-Length"));
                    if (fileSize.compareTo(BigInteger.ZERO) > 0) {
                        LOGGER.debug(S3_FILE_EXISTENCE_MESSAGE + " with size: {}",fileSize);
                        promise.complete(true);
                    } else {
                        promise.fail(S3_FILE_EXISTENCE_FAIL_MESSAGE);
                    }
                })
                .onFailure(failed -> {
                    LOGGER.error("Failed to get response from S3: " + failed.getLocalizedMessage());
                    promise.fail(failed.getMessage());
                });

        return promise.future();
    }

    /**
     * Checks the collection type.
     *
     * @param requestBody the input JSON object
     * @return a Future indicating completion
     */

    private Future<Void> checkCollectionType(JsonObject requestBody){
        Promise<Void> promise = Promise.promise();
        String collectionType = requestBody.getString("collectionType");
        if("feature".equalsIgnoreCase(collectionType)){
            LOGGER.debug(FEATURE_COLLECTION_MESSAGE);
            promise.fail(FEATURE_COLLECTION_MESSAGE);
        }
        return promise.future();
    }

    /**
     * Handles failure scenarios.
     *
     * @param requestInput the input JSON object
     * @param errorMessage the error message
     * @param promise      the promise to fail
     */

    private void handleFailure(JsonObject requestInput, String errorMessage, Promise<JsonObject> promise) {

        utilClass.updateJobTableStatus(requestInput, Status.FAILED, errorMessage)
                .onSuccess(successHandler -> {
                    LOGGER.error("Process failed: {}" ,errorMessage);
                    promise.fail(errorMessage);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(HANDLE_FAILURE_MESSAGE + ": " +failureHandler.getMessage());
                    promise.fail(HANDLE_FAILURE_MESSAGE);
                });

    }

    /**
     * Calculates the progress percentage.
     *
     * @param currentStep the current step number
     * @param totalSteps  the total number of steps
     * @return the progress percentage
     */

    private float calculateProgress(int currentStep, int totalSteps){
        return ((float) currentStep / totalSteps) * 100;

    }
}
