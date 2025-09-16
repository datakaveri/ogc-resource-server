package ogc.rs.processes.tilesMetaDataOnboarding;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.authentication.util.DxUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ogc.rs.common.DataFromS3;
import ogc.rs.common.S3Config;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.ProcessesRunnerImpl;
import static ogc.rs.processes.tilesMetaDataOnboarding.MessageConstants.*;
import static ogc.rs.processes.tilesMetaDataOnboarding.SqlConstants.*;

import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;

/**
 * Handles the onboarding process for tiles, including file existence checks,
 * resource ownership verification, collection type validation, and collection
 * existence checks.
 */

public class TilesMetaDataOnboardingProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(TilesMetaDataOnboardingProcess.class);
    private final PgPool pgPool;
    private final UtilClass utilClass;
    private final CollectionOnboardingProcess collectionOnboarding;
    private final DataFromS3 dataFromS3;

    public enum EncodingFormatEnum {
        PNG,
        MVT
    }

    /**
     * Constructs a TilesMetaDataOnboardingProcess.
     *
     * @param pgPool       Postgresql database connection pool
     * @param webClient    Vert.x web client for making HTTP requests
     * @param config       Configuration JSON object containing database and other settings
     * @param s3conf       The S3Config instance i.e. config to access bucket requested in process input.
     * @param vertx        Vert.x instance for asynchronous event-driven programming
     */
    public TilesMetaDataOnboardingProcess(PgPool pgPool, WebClient webClient, JsonObject config, S3Config s3conf, Vertx vertx){
        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
        this.collectionOnboarding = new CollectionOnboardingProcess(pgPool, webClient, config, s3conf, vertx);
        this.dataFromS3 = new DataFromS3(vertx.createHttpClient(new HttpClientOptions().setShared(true)), s3conf);

    }
    /**
     * Executes the tiles metadata onboarding process asynchronously.
     * <p>
     * This method performs the onboarding process for tiles by checking file existence in S3, verifying collection type,
     * checking collection existence, verifying tile matrix set, and onboarding tile metadata. It updates the job table
     * status and progress throughout the process.
     * </p>
     *
     * @param requestInput A {@link JsonObject} containing collection and tile matrix set details. Must include keys
     *                     {@code "resourceId"}, {@code "tileMatrixSet"}, and {@code "collectionType"}.
     * @return A {@link Future<JsonObject>} that will be completed with the result JSON object after successful completion
     *         of the onboarding process, or failed with an appropriate error message if any step fails.
     */

    public Future<JsonObject> execute(JsonObject requestInput){
        Promise<JsonObject> promise = Promise.promise();
        String collectionId = requestInput.getString("resourceId");
        String testTileCoordinateIndexes = requestInput.getString("testTileCoordinateIndexes");
        String tileMatrixSet = requestInput.getString("tileMatrixSet");
        String encoding = requestInput.getString("encoding");
        String collectionType = "MVT".equalsIgnoreCase(encoding) ? "VECTOR" : "MAP";
        requestInput.put("collectionType",collectionType);
        JsonObject providerJson = requestInput.getJsonObject("user");
        DxUser user = DxUser.fromJsonObject(providerJson);

      // Determine file extension based on encoding
        String fileExtension = "MVT".equalsIgnoreCase(encoding) ? ".pbf" : "." + encoding.toLowerCase();
        // Construct the file path
        String fileName = collectionId + "/" + tileMatrixSet +  "/" +testTileCoordinateIndexes + fileExtension;
        requestInput.put("fileName",fileName);
        requestInput.put("progress",calculateProgress(1));
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, START_TILES_METADATA_ONBOARDING_PROCESS)
                .compose(progressUpdateHandler -> checkEncodingFormat(requestInput))
                .compose(checkEncodingFormatHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2)).put("message", ENCODING_FORMAT_CHECK_MESSAGE)))
                .compose(progressUpdateHandler -> checkTileMatrixSet(requestInput))
                .compose(tileMatrixCheckHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3)).put("message", TILE_MATRIX_SET_FOUND_MESSAGE)))
                .compose(progressUpdateHandler -> collectionOnboarding.validateOwnershipAndGetResourceInfo(collectionId,user))
                .compose(resourceOwnershipHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(4)).put("message", RESOURCE_OWNERSHIP_CHECK_MESSAGE)))
                .compose(progressUpdateHandler -> checkFileExistenceInS3(requestInput))
                .compose(s3FileExistenceHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(5)).put("message", S3_FILE_EXISTENCE_MESSAGE)))
                .compose(progressUpdateHandler -> evaluateCollectionTileType(requestInput))
                .compose(identifyPureTileCollectionHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(6)).put("message", COLLECTION_EVALUATION_MESSAGE)))
                .compose(progressUpdateHandler -> onboardTileMetadata(requestInput))
                .compose(tilesMetaDataOnboardingHandler -> utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL, TILES_METADATA_ONBOARDING_SUCCESS_MESSAGE))
                .onSuccess(successHandler -> {
                    LOGGER.debug(TILES_METADATA_ONBOARDING_SUCCESS_MESSAGE);
                    promise.complete();
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(TILES_METADATA_ONBOARDING_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler.getMessage(), promise);
                });
        return promise.future();
    }

    /**
     * Validates the encoding format provided in the request body.
     * <p>
     * This method checks if the `encoding` parameter in the `requestBody` JSON object is one of the acceptable formats:
     * PNG or MVT. If the encoding is invalid or null, it logs an error and fails the promise with an appropriate message.
     * </p>
     *
     * @param requestBody the JSON object containing the request parameters, including the encoding format.
     * @return a {@link Future} that will be completed when the validation is finished.
     *         If the encoding is valid, the future will be completed successfully.
     *         If the encoding is invalid, the future will be failed with an error message.
     */
    private Future<Void> checkEncodingFormat(JsonObject requestBody) {
        Promise<Void> promise = Promise.promise();
        String encoding = requestBody.getString("encoding");

        if (!isValidEncoding(encoding)) {
            LOGGER.error(INVALID_ENCODING_FORMAT_MESSAGE + ": " + encoding);
            promise.fail(INVALID_ENCODING_FORMAT_MESSAGE);
        } else {
            promise.complete();
        }

        return promise.future();
    }

    /**
     * Checks if the provided encoding format is valid according to the {@link EncodingFormatEnum}.
     * <p>
     * This method attempts to convert the provided encoding string to an enum constant of {@link EncodingFormatEnum}.
     * If the conversion is successful, the encoding is considered valid. If an {@link IllegalArgumentException}
     * is thrown due to an invalid encoding format, the method returns {@code false}.
     * </p>
     *
     * @param encoding the encoding format string to be validated.
     * @return {@code true} if the encoding format is valid and corresponds to an enum constant in {@link EncodingFormatEnum};
     *         {@code false} otherwise.
     */
    private static boolean isValidEncoding(String encoding) {
        try {
            EncodingFormatEnum.valueOf(encoding.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if the tile matrix set exists in the {@code tms_metadata} table and adds 'id' and 'crs'
     * column values into the {@code requestInput} JSON object.
     * <p>
     * This method queries the database to check if the tile matrix set exists. If it exists, it adds the 'id' and 'crs'
     * values to the request input.
     * </p>
     *
     * @param requestInput Input JSON object containing tile matrix set details.
     * @return A {@link Future<JsonObject>} containing the updated JSON object with 'id' and 'crs' values if the tile matrix set exists.
     */
    public Future<JsonObject> checkTileMatrixSet(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        String tmsTitle = requestInput.getString("tileMatrixSet");

        pgPool.preparedQuery(CHECK_TILE_MATRIX_SET_EXISTENCE_QUERY)
                .execute(Tuple.of(tmsTitle))
                .onSuccess(rowSet -> {
                    // Collect Rows into List<JsonObject>
                    List<JsonObject> jsonList = new ArrayList<>();
                    rowSet.forEach(row -> jsonList.add(row.toJson())); // Convert each Row to JsonObject and add to the list

                    // Check if we have any results
                    if (!jsonList.isEmpty()) {
                        JsonObject rowJson = jsonList.get(0);
                        // Check if the tile matrix set exists
                        if (rowJson.getBoolean("exists")) {
                            LOGGER.debug(TILE_MATRIX_SET_FOUND_MESSAGE + ": " + tmsTitle);
                            // Add 'id' and 'crs' column values into requestInput
                            requestInput.put("tms_id", rowJson.getValue("id"));
                            requestInput.put("crs", rowJson.getValue("crs"));
                            promise.complete(requestInput);
                        } else {
                            LOGGER.error(TILE_MATRIX_SET_NOT_FOUND_MESSAGE + ": " + tmsTitle);
                            promise.fail(TILE_MATRIX_SET_NOT_FOUND_MESSAGE);
                        }
                    } else {
                        LOGGER.error(TILE_MATRIX_SET_NOT_FOUND_MESSAGE + ": " + tmsTitle);
                        promise.fail(TILE_MATRIX_SET_NOT_FOUND_MESSAGE);
                    }
                })
                .onFailure(err -> {
                    LOGGER.error(TILE_MATRIX_SET_CHECK_FAILURE_MESSAGE + ": " + err.getMessage());
                    promise.fail(TILE_MATRIX_SET_CHECK_FAILURE_MESSAGE + ": " + err.getMessage());
                });

        return promise.future();
    }

    /**
     * Checks the existence of a file in an S3 bucket and verifies its size.
     * <p>
     * This method constructs the URL for the file based on the provided filename, sends an HTTP HEAD request to S3,
     * and checks the response status code and content length. If the file exists and has a non-zero size, it completes
     * the promise with {@code true}. If the file does not exist or is empty, or if an error occurs during the request,
     * it fails the promise with an appropriate error message.
     * </p>
     *
     * @param requestInput A {@link JsonObject} containing the file information. Must include a key {@code "fileName"}
     *                     with the name of the file to check.
     * @return A {@link Future<Boolean>} that will be completed with {@code true} if the file exists and is not empty,
     *         or failed with an appropriate error message if the file does not exist, is empty, or an error occurs.
     *
     */
    private Future<Boolean> checkFileExistenceInS3(JsonObject requestInput) {
        Promise<Boolean> promise = Promise.promise();
        String fileName = requestInput.getString("fileName");
        LOGGER.debug("Checking existence of file: {}", fileName);
        String urlString = dataFromS3.getFullyQualifiedUrlString(fileName);
        LOGGER.debug("Constructed URL: {}", urlString);
        dataFromS3.setUrlFromString(urlString);
        dataFromS3.setSignatureHeader(HttpMethod.HEAD);

        dataFromS3.getDataFromS3(HttpMethod.HEAD)
                .onSuccess(responseFromS3 -> {
                            String contentLengthHeader = responseFromS3.getHeader("Content-Length");
                            BigInteger fileSize = contentLengthHeader != null
                                    ? new BigInteger(contentLengthHeader)
                                    : BigInteger.ZERO;
                            if (fileSize.compareTo(BigInteger.ZERO) > 0) {
                                LOGGER.debug("File exists and has size: {}", fileSize);
                                promise.complete(true);
                            } else {
                                LOGGER.warn("File exists but is empty");
                                promise.fail(S3_EMPTY_FILE_MESSAGE);
                            }
                })
                .onFailure(failed -> {
                    LOGGER.error("Failed to get response from S3: {}", failed.getMessage());
                    promise.fail(S3_FILE_EXISTENCE_FAIL_MESSAGE);
                });

        return promise.future();
    }

    /**
     * Checks the existence of the collection and determines if it is a pure tile collection
     * or a combination of feature and tile collection.
     * <p>
     * This method queries the database to check if the collection exists. If the collection exists, it further checks
     * its type. If the collection does not exist, it marks the collection as a pure tile collection.
     * </p>
     *
     * @param requestInput A JSON object containing collection details with a key {@code "resourceId"} for the collection identifier.
     * @return A {@link Future<JsonObject>} containing the updated JSON object with the {@code "pureTile"} attribute
     * indicating whether the collection is a pure tile collection (true) or not (false).
     */
    private Future<JsonObject> evaluateCollectionTileType(JsonObject requestInput) {
        String collectionId = requestInput.getString("resourceId");
        Promise<JsonObject> promise = Promise.promise();

        // Check if the collection exists in the collection_details table
        pgPool.preparedQuery(CHECK_COLLECTION_EXISTENCE_QUERY)
                .execute(Tuple.of(collectionId))
                .onSuccess(rowSet -> {
                    // Check if the collection exists
                    if (rowSet.rowCount() > 0 && rowSet.iterator().next().getBoolean("exists")) {
                        // Collection exists, now get its types from the collection_type table
                        pgPool.preparedQuery(GET_COLLECTION_TYPE_QUERY)
                                .execute(Tuple.of(collectionId))
                                .onSuccess(typeRowSet -> {
                                    if (typeRowSet.rowCount() > 0) {
                                        Row row = typeRowSet.iterator().next();
                                        // Get the array of strings
                                        String[] typesArray = row.getArrayOfStrings("array_agg");

                                        // Convert String[] to a List for easier processing
                                        List<String> typesList = Arrays.asList(typesArray);

                                        boolean isFeature = typesList.stream()
                                                .anyMatch("FEATURE"::equalsIgnoreCase);
                                        boolean isVector = typesList.stream()
                                                .anyMatch("VECTOR"::equalsIgnoreCase);
                                        boolean isMap = typesList.stream()
                                                .anyMatch("MAP"::equalsIgnoreCase);

                                        if (isFeature && !isVector && !isMap) {
                                            requestInput.put("pureTile", false);
                                            promise.complete(requestInput);
                                        } else if (isVector || isMap) {
                                            promise.fail(COLLECTION_EXISTS_MESSAGE);
                                        } else {
                                            promise.fail(UNKNOWN_COLLECTION_TYPE);
                                        }
                                    } else {
                                        promise.fail(COLLECTION_TYPE_NOT_FOUND_MESSAGE);
                                    }
                                })
                                .onFailure(err -> promise.fail(COLLECTION_EXISTENCE_CHECK_FAILURE_MESSAGE + " :" + err.getMessage()));
                    } else {
                        // Collection does not exist
                        requestInput.put("pureTile", true);
                        promise.complete(requestInput);
                    }
                })
                .onFailure(err -> {
                    // Handle failure
                    promise.fail(COLLECTION_EXISTENCE_CHECK_FAILURE_MESSAGE + " :" + err.getMessage());
                });

        return promise.future();
    }


    /**
     * Onboards tile metadata into the database. Depending on whether the collection is a pure tile collection,
     * it inserts data into different tables.
     * <p>
     * This method performs the necessary database operations to onboard tile metadata. It handles both pure tile collections
     * and collections that combine feature and tile data.
     * </p>
     *
     * @param requestInput JSON object containing tile metadata information and other details.
     * @return A {@link Future<Void>} indicating the success or failure of the onboarding process.
     */
    public Future<Void> onboardTileMetadata(JsonObject requestInput) {
        return pgPool.withTransaction(sqlClient -> {
            Promise<Void> promise = Promise.promise();
            LOGGER.debug("Starting the onboarding process for tile metadata.");

            boolean pureTile = requestInput.getBoolean("pureTile");
            String collectionId = requestInput.getString("resourceId");
            String title = requestInput.getString("title");
            String description = requestInput.getString("description");
            String crs = requestInput.getString("crs");
            JsonArray bbox = requestInput.getJsonArray("bbox");
            JsonArray temporal = requestInput.getJsonArray("temporal");
            String accessPolicy = requestInput.getString("accessPolicy");
            String userId = requestInput.getString("userId");
            String collectionType = requestInput.getString("collectionType");
            String tmsId = requestInput.getString("tms_id");
            JsonArray pointOfOrigin = requestInput.getJsonArray("pointOfOrigin");
            String s3BucketId =
                requestInput.getString(ProcessesRunnerImpl.S3_BUCKET_IDENTIFIER_PROCESS_INPUT_KEY);

            // Convert JsonArray to Double[] for bbox and pointOfOrigin
            Double[] bboxArray = (bbox != null) ? bbox.stream()
                    .map(obj -> obj instanceof Number ? ((Number) obj).doubleValue() : 0.0)
                    .toArray(Double[]::new) : new Double[0];

            Double[] pointOfOriginArray = (pointOfOrigin != null) ? pointOfOrigin.stream()
                    .map(obj -> obj instanceof Number ? ((Number) obj).doubleValue() : 0.0)
                    .toArray(Double[]::new) : new Double[0];

            // Convert JsonArray to String[] for temporal
            String[] temporalArray = (temporal != null) ? temporal.stream()
                    .map(Object::toString)
                    .toArray(String[]::new) : new String[0];

            // Start the chain based on the condition
            Future<Void> chainFuture;

            if (pureTile) {
                LOGGER.debug("Inserting collection details for collectionId: {}", collectionId);
                chainFuture = sqlClient.preparedQuery(INSERT_COLLECTION_DETAILS_QUERY)
                        .execute(Tuple.of(collectionId, title, description, crs, bboxArray, temporalArray))
                        .mapEmpty()
                        .compose(v -> {
                            LOGGER.debug("Inserting RI details for collectionId: {}", collectionId);
                            return sqlClient.preparedQuery(INSERT_RI_DETAILS_QUERY)
                                    .execute(Tuple.of(collectionId, accessPolicy, userId))
                                    .mapEmpty();
                        });
            } else {
                chainFuture = Future.succeededFuture(); // No initial operations, just continue
            }

            return chainFuture
                    .compose(v -> {
                        LOGGER.debug("Inserting collection type for collectionId: {}", collectionId);
                        return sqlClient.preparedQuery(INSERT_COLLECTION_TYPE_QUERY)
                                .execute(Tuple.of(collectionId, collectionType))
                                .mapEmpty();
                    })
                    .compose(v -> {
                        LOGGER.debug("Inserting tile matrix set relation for collectionId: {}", collectionId);
                        return sqlClient.preparedQuery(INSERT_TILE_MATRIX_SET_RELATION_QUERY)
                                .execute(Tuple.of(collectionId, tmsId, pointOfOriginArray, s3BucketId))
                                .mapEmpty();
                    })
                    .onSuccess(v -> {
                        LOGGER.debug("Tiles Meta Data Onboarding process completed successfully for collectionId: {}", collectionId);
                        promise.complete();
                    })
                    .onFailure(throwable -> {
                        LOGGER.error("Error occurred during tiles meta data onboarding process for collectionId: {}. Error: {}", collectionId, throwable.getMessage());
                        promise.fail(throwable);
                    })
                    .mapEmpty();
        });
    }

    /**
     * Handles failure scenarios by updating the job table status and failing the promise.
     * <p>
     * This method updates the job table status to "FAILED" and logs the error message. It then fails the promise
     * with the provided error message.
     * </p>
     *
     * @param requestInput Input JSON object containing request details.
     * @param errorMessage Error message describing the failure reason.
     * @param promise      Promise to fail with the error message.
     */
    private void handleFailure(JsonObject requestInput, String errorMessage, Promise<JsonObject> promise) {

        utilClass.updateJobTableStatus(requestInput, Status.FAILED, errorMessage)
                .onSuccess(successHandler -> {
                    LOGGER.error("Process failed: {}", errorMessage);
                    promise.fail(errorMessage);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(HANDLE_FAILURE_MESSAGE + ": " + failureHandler.getMessage());
                    promise.fail(HANDLE_FAILURE_MESSAGE);
                });

    }

    /**
     * Calculates the progress percentage based on the current step in a multiple step process.
     * <p>
     * This method computes the progress as a percentage of the current step relative to the total number of steps.
     * </p>
     *
     * @param currentStep Current step number in the process.
     * @return Progress percentage as a float value.
     */
    private float calculateProgress(int currentStep){
        return ((float) currentStep / 7) * 100;
    }
}
