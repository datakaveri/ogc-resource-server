package ogc.rs.processes.tilesOnboardingFromExistingFeature;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import ogc.rs.common.DataFromS3;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.tilesMetaDataOnboarding.TilesMetaDataOnboardingProcess;
import static ogc.rs.processes.tilesOnboardingFromExistingFeature.Constants.*;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Process for onboarding tiles from an existing feature collection.
 * This process includes checking the collection type, converting features to tiles using ogr2ogr,
 * uploading to S3, and handling tiles metadata onboarding.
 */
public class TilesOnboardingFromExistingFeatureProcess implements ProcessService{
    private static final Logger LOGGER = LogManager.getLogger(TilesOnboardingFromExistingFeatureProcess.class);
    private final Vertx vertx;
    private final PgPool pgPool;
    private final UtilClass utilClass;
    private final CollectionOnboardingProcess featureCollectionOnboarding;
    private final TilesMetaDataOnboardingProcess tilesMetaDataOnboarding;
    private String awsEndPoint;
    private String accessKey;
    private String secretKey;
    private String awsBucketUrl;
    private String databaseName;
    private String databaseHost;
    private String databasePassword;
    private String databaseUser;
    private int databasePort;

    /**
     * Constructor for TilesOnboardingFromExistingFeatureProcess.
     *
     * @param pgPool                 the PostgreSQL connection pool
     * @param webClient              the web client for HTTP requests
     * @param config                 the configuration object containing necessary properties
     * @param dataFromS3             the DataFromS3 instance for handling S3-related operations
     * @param vertx                  the Vertx instance for asynchronous operations
     */
    public TilesOnboardingFromExistingFeatureProcess(PgPool pgPool, WebClient webClient, JsonObject config, DataFromS3 dataFromS3, Vertx vertx){
        this.vertx = vertx;
        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
        this.featureCollectionOnboarding = new CollectionOnboardingProcess(pgPool, webClient, config, dataFromS3, vertx);
        this.tilesMetaDataOnboarding = new TilesMetaDataOnboardingProcess(pgPool, webClient, config, dataFromS3, vertx);
        initializeConfig(config);
    }

    /**
     * Initializes configuration parameters from the provided JsonObject.
     *
     * @param config the configuration object
     */
    private void initializeConfig(JsonObject config){
        this.awsEndPoint = config.getString("awsEndPoint");
        this.accessKey = config.getString("awsAccessKey");
        this.secretKey = config.getString("awsSecretKey");
        this.awsBucketUrl = config.getString("s3BucketUrl");
        this.databaseName = config.getString("databaseName");
        this.databaseHost = config.getString("databaseHost");
        this.databasePassword = config.getString("databasePassword");
        this.databaseUser = config.getString("databaseUser");
        this.databasePort = config.getInteger("databasePort");
    }

    /**
     * Executes the tiles onboarding process for an existing feature collection.
     *
     * @param requestInput the input JSON object containing parameters for the process
     * @return a Future containing the final result or an error
     */
    public Future<JsonObject> execute(JsonObject requestInput){
        Promise<JsonObject> promise = Promise.promise();

        requestInput.put("tileMatrixSet","WorldCRS84Quad");
        requestInput.put("collectionType", "VECTOR");
        requestInput.put("pureTile", false);
        requestInput.put("progress",calculateProgress(1));
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, START_TILES_ONBOARDING_PROCESS)
                .compose(progressUpdateHandler -> featureCollectionOnboarding.makeCatApiRequest(requestInput))
                .compose(resourceOwnershipHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress",calculateProgress(2)).put("message",RESOURCE_OWNERSHIP_CHECK_MESSAGE)))
                .compose(progressUpdateHandler -> checkIfFeatureCollectionExists(requestInput))
                .compose(checkFeatureCollectionHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress",calculateProgress(3)).put("message", FEATURE_EXISTS_MESSAGE)))
                .compose(progressUpdateHandler -> tilesMetaDataOnboarding.checkTileMatrixSet(requestInput))
                .compose(checkTileMatrixSetHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress",calculateProgress(4)).put("message", TILE_MATRIX_SET_EXISTS_MESSAGE)))
                .compose(progressUpdateHandler -> onboardTilesFromExistingFeatureCollection(requestInput))
                .compose(onboardTilesHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress",calculateProgress(5)).put("message",TILES_ONBOARDING_SUCCESS_MESSAGE)))
                .compose(progressUpdateHandler -> tilesMetaDataOnboarding.onboardTileMetadata(requestInput))
                .compose(onboardTilesMetaDataHandler -> utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL, PROCESS_SUCCESS_MESSAGE))
                .onSuccess(tilesOnboardingSuccessHandler -> {
                    LOGGER.debug(PROCESS_SUCCESS_MESSAGE);
                    promise.complete();
                }).onFailure(tilesOnboardingFailureHandler -> {
                    LOGGER.debug(PROCESS_FAILURE_MESSAGE);
                    handleFailure(requestInput, tilesOnboardingFailureHandler.getMessage(), promise);
                });

        return promise.future();
    }

    /**
     * Checks if the feature collection for the given resource ID exists in the database.
     *
     * @param requestInput the input JSON object containing the resource ID
     * @return a Future that completes if the feature collection exists and is not already a tile
     */
    private Future<Void> checkIfFeatureCollectionExists(JsonObject requestInput) {
        Promise<Void> promise = Promise.promise();
        String collection_id = requestInput.getString("resourceId");

        pgPool.withConnection(postgisConnection ->
                postgisConnection.preparedQuery(COLLECTION_TYPE_SELECT_QUERY)
                        .execute(Tuple.of(collection_id))
                        .onSuccess(rowSet -> {
                            if (rowSet.size() == 0) {
                                // No matching collection_id
                                LOGGER.error(RESOURCE_NOT_ONBOARDED_MESSAGE);
                                promise.fail(RESOURCE_NOT_ONBOARDED_MESSAGE);
                            } else {
                                // Extract all types associated with the collection_id
                                List<String> types = new ArrayList<>();
                                rowSet.forEach(row -> types.add(row.getString("type")));

                                // Check if only FEATURE is present
                                if (types.size() == 1 && "FEATURE".equals(types.get(0))) {
                                    LOGGER.debug(FEATURE_EXISTS_MESSAGE);
                                    promise.complete();
                                } else {
                                    LOGGER.error("Collection has additional types: {}. Process cannot proceed.", types);
                                    promise.fail(TILES_ALREADY_ONBOARDED_MESSAGE);
                                }
                            }
                        })
                        .onFailure(error -> {
                            LOGGER.error("Failed to check the existing feature collection: " + error.getMessage());
                            promise.fail("Failed to check the existing feature collection: " + error.getMessage());
                        })
        );
        return promise.future();
    }

    /**
     * Onboards tiles from an existing feature collection using ogr2ogr.
     *
     * @param requestInput the input JSON object containing process parameters
     * @return a Future containing the result of the onboarding operation
     */
    private Future<JsonObject> onboardTilesFromExistingFeatureCollection(JsonObject requestInput){
        Promise<JsonObject> promise = Promise.promise();
        boolean VERTX_EXECUTE_BLOCKING_IN_ORDER = false;
        vertx.<JsonObject>executeBlocking(
                        onboardingPromise -> {
                            LOGGER.debug("Trying to onboard tiles from the existing feature collection...");
                            CommandLine cmdLine = getCommandLineOgr2Ogr(requestInput);
                            DefaultExecutor defaultExecutor = DefaultExecutor.builder().get();
                            defaultExecutor.setExitValue(0);
                            ExecuteWatchdog watchDog = ExecuteWatchdog.builder().setTimeout(Duration.ofHours(1)).get();
                            defaultExecutor.setWatchdog(watchDog);

                            try{
                                int exitValue = defaultExecutor.execute(cmdLine);
                                LOGGER.debug("Exit value in ogr2ogr: {}",exitValue);
                                requestInput.put("exitValue",exitValue);
                                LOGGER.debug("OGR2OGR command executed successfully!");
                                onboardingPromise.complete(requestInput);
                            }
                            catch(IOException e){
                                LOGGER.error("Failed to onboard tiles using ogr2ogr because:{}", e.getMessage());
                                onboardingPromise.fail(OGR_2_OGR_FAILED);
                            }}, VERTX_EXECUTE_BLOCKING_IN_ORDER)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Constructs the ogr2ogr command for converting features to tiles.
     *
     * @param requestInput the input JSON object containing process parameters
     * @return a CommandLine object representing the ogr2ogr command
     */
    private CommandLine getCommandLineOgr2Ogr(JsonObject requestInput) {
        String collectionId = requestInput.getString("resourceId");
        String tileMatrixSet = requestInput.getString("tileMatrixSet");
        int minZoomLevel = requestInput.getInteger("minZoomLevel");
        int maxZoomLevel = requestInput.getInteger("maxZoomLevel");

        // Construct the S3 output path dynamically using resourceId and tileMatrixSet
        String s3OutputPath = String.format("/vsis3/%s/%s/%s", awsBucketUrl, collectionId, tileMatrixSet);

        // SQL query to select tiles from the feature collection
        String sqlQuery = String.format("SELECT * FROM \"%s\"", collectionId);

        // Build the ogr2ogr command
        CommandLine cmdLine = new CommandLine("ogr2ogr");
        cmdLine.addArgument("--config");
        cmdLine.addArgument("CPL_VSIL_USE_TEMP_FILE_FOR_RANDOM_WRITE");
        cmdLine.addArgument("YES");
        // Set the output format to MVT
        cmdLine.addArgument("-f");
        cmdLine.addArgument("MVT");
        // Add the output path
        cmdLine.addArgument(s3OutputPath);
        // Add the database connection string
        cmdLine.addArgument(
                String.format("PG:host=%s dbname=%s user=%s port=%d password=%s schemas=public", databaseHost,
                        databaseName, databaseUser, databasePort, databasePassword), false);
        // Add the SQL query
        cmdLine.addArgument("-sql");
        cmdLine.addArgument(sqlQuery, false);
        // Configure AWS credentials and endpoint
        cmdLine.addArgument("--config");
        cmdLine.addArgument("AWS_S3_ENDPOINT");
        cmdLine.addArgument(awsEndPoint);
        cmdLine.addArgument("--config");
        cmdLine.addArgument("AWS_ACCESS_KEY_ID");
        cmdLine.addArgument(accessKey);
        cmdLine.addArgument("--config");
        cmdLine.addArgument("AWS_SECRET_ACCESS_KEY");
        cmdLine.addArgument(secretKey);
        // Set dataset creation options (TILING_SCHEME, MINZOOM, MAXZOOM, COMPRESS)
        cmdLine.addArgument("-dsco");
        cmdLine.addArgument("TILING_SCHEME=EPSG:4326,-180,90,180");
        cmdLine.addArgument("-dsco");
        cmdLine.addArgument(String.format("MAXZOOM=%d", maxZoomLevel));
        cmdLine.addArgument("-dsco");
        cmdLine.addArgument(String.format("MINZOOM=%d", minZoomLevel));
        cmdLine.addArgument("-dsco");
        cmdLine.addArgument("COMPRESS=NO");
        // Enable progress and debug
        cmdLine.addArgument("-progress");
        cmdLine.addArgument("--debug");
        cmdLine.addArgument("ON");

        LOGGER.debug("Generated ogr2ogr command: {}", cmdLine);

        return cmdLine;
    }

    /**
     * Handles failure during the onboarding process.
     *
     * @param requestInput  the input JSON object containing process parameters
     * @param errorMessage  the error message
     * @param promise       the promise to fail
     */
    private void handleFailure(JsonObject requestInput, String errorMessage, Promise<JsonObject> promise){
        utilClass.updateJobTableStatus(requestInput, Status.FAILED, errorMessage)
                .onSuccess(successHandler -> {
                    LOGGER.error("Process failed: {}",errorMessage);
                    promise.fail(errorMessage);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(HANDLE_FAILURE_MESSAGE + ": " + failureHandler.getMessage());
                    promise.fail(failureHandler.getMessage());
                });
    }

    /**
     * Calculates the progress percentage for the given step.
     *
     * @param currentStep the current step in the process
     * @return the progress percentage
     */
    private float calculateProgress(int currentStep){
        return ((float) currentStep / 6) * 100;
    }
}