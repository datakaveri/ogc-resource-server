package ogc.rs.processes.s3MultiPartUploadForStacOnboarding;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.common.DataFromS3;
import ogc.rs.common.S3Config;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static ogc.rs.processes.s3MultiPartUploadForStacOnboarding.Constants.*;

/**
 * This class handles the process of initiating a multipart upload to S3
 * and generating presigned URLs for each part to facilitate STAC resource onboarding.
 */
public class S3InitiateMultiPartUploadProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(S3InitiateMultiPartUploadProcess.class);
    private final UtilClass utilClass;
    private final CollectionOnboardingProcess collectionOnboarding;
    private final DataFromS3 dataFromS3;
    private final PgPool pgPool;
    private S3Config s3conf;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    /**
     * Constructor to initialize the S3 multipart upload process.
     *
     * @param pgPool          The PostgreSQL connection pool for database interactions.
     * @param webClient       The WebClient for making HTTP requests.
     * @param config          The configuration object containing S3 credentials and settings.
     * @param dataFromS3      Utility class for interacting with S3.
     * @param vertx           The Vertx instance for asynchronous operations.
     */
    public S3InitiateMultiPartUploadProcess(PgPool pgPool, WebClient webClient, JsonObject config, DataFromS3 dataFromS3, Vertx vertx) {
        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
        this.dataFromS3 = dataFromS3;
        this.collectionOnboarding = new CollectionOnboardingProcess(pgPool, webClient, config, dataFromS3, vertx);
        initializeConfig(config);

        // Initialize S3 Client & S3 Presigner
        this.s3Client = S3Client.builder()
                .region(Region.of(s3conf.getRegion()))
                .endpointOverride(URI.create(s3conf.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3conf.getAccessKey(), s3conf.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3conf.isPathBasedAccess())
                        .build())
                .build();

        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(s3conf.getRegion()))
                .endpointOverride(URI.create(s3conf.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3conf.getAccessKey(), s3conf.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3conf.isPathBasedAccess())
                        .build())
                .build();

    }

    /**
     * Initializes S3 configuration from the provided JSON configuration.
     *
     * @param config JSON object containing S3 credentials, region, bucket, and endpoint details.
     */
    private void initializeConfig(JsonObject config) {
        this.s3conf = new S3Config.Builder()
                .endpoint(config.getString(S3Config.ENDPOINT_CONF_OP))
                .bucket(config.getString(S3Config.BUCKET_CONF_OP))
                .region(config.getString(S3Config.REGION_CONF_OP))
                .accessKey(config.getString(S3Config.ACCESS_KEY_CONF_OP))
                .secretKey(config.getString(S3Config.SECRET_KEY_CONF_OP))
                .pathBasedAccess(config.getBoolean(S3Config.PATH_BASED_ACC_CONF_OP))
                .build();
    }

    /**
     * Executes the initiating multipart upload process, validating resource ownership,
     * checking item existence, verifying S3 object existence, and generating presigned URLs.
     *
     * @param requestInput JSON object containing upload request details.
     * @return A future representing the completion of the process.
     */
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        LOGGER.info("collection-id fetched from UI:{}",requestInput.getString("collectionId"));
        String resourceId = requestInput.getString("collectionId");
        requestInput.put("resourceId", resourceId);
        String itemId = requestInput.getString("itemId");
        String bucketName = requestInput.getString("bucketName");
        String fileName = requestInput.getString("fileName");
        String fileType = requestInput.getString("fileType");
        long fileSize = requestInput.getLong("fileSize");

        // Determine chunk size based on file size
        long chunkSize = determineChunkSize(fileSize);
        int totalParts = (int) Math.ceil((double) fileSize / chunkSize);

        String s3KeyName = resourceId + "/" + itemId + "/" + fileName;
        requestInput.put("s3KeyName", s3KeyName);
        requestInput.put("progress", calculateProgress(1));

        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, INITIATE_MULTIPART_UPLOAD_PROCESS_START_MESSAGE)
                .compose(progressUpdate -> collectionOnboarding.makeCatApiRequest(requestInput))
                .compose(resourceOwnershipHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2)).put(MESSAGE, RESOURCE_OWNERSHIP_CHECK_MESSAGE)))
                .compose(progressUpdate -> checkResourceOnboardedAsStac(requestInput))
                .compose(stacCheckHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3)).put(MESSAGE, STAC_RESOURCE_ONBOARDED_MESSAGE)))
                .compose(progressUpdate -> checkItemExists(requestInput))
                .compose(itemCheckHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(4)).put(MESSAGE, ITEM_EXISTS_MESSAGE)))
                .compose(progressUpdateHandler -> checkIfObjectExistsInS3(requestInput))
                .compose(objectDoesNotExist -> {
                    if (objectDoesNotExist) {
                        LOGGER.info(OBJECT_DOES_NOT_EXIST_MESSAGE);
                        return initiateMultipartUpload(bucketName, s3KeyName,fileName,fileType);
                    } else {
                        LOGGER.error(OBJECT_ALREADY_EXISTS_MESSAGE);
                        return Future.failedFuture(new OgcException(409, "Conflict", OBJECT_ALREADY_EXISTS_MESSAGE));
                    }
                })
                .compose(uploadIdHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress",calculateProgress(5)).put(MESSAGE, INITIATE_MULTIPART_UPLOAD_MESSAGE)
                ).map(uploadIdHandler))
                .compose(uploadId -> generatePresignedUrls(bucketName, s3KeyName, uploadId, totalParts,chunkSize))
                .compose(result -> utilClass.updateJobTableStatus(
                        requestInput, Status.SUCCESSFUL, INITIATE_MULTIPART_UPLOAD_PROCESS_COMPLETE_MESSAGE
                ).map(result))
                .onSuccess(result -> {
                    LOGGER.info(INITIATE_MULTIPART_UPLOAD_PROCESS_COMPLETE_MESSAGE);
                    promise.complete(result);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(INITIATE_MULTIPART_UPLOAD_PROCESS_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler, promise);
                });

        return promise.future();
    }

    /**
     * Determines the optimal chunk size for a multipart upload based on the file size.
     * @param fileSize The size of the file in bytes.
     * @return The determined chunk size in bytes.
     */
    private long determineChunkSize(long fileSize) {
        if (fileSize > 100L * 1024 * 1024 * 1024) {  // > 100GB
            return 500L * 1024 * 1024; // 500MB
        } else if (fileSize > 50L * 1024 * 1024 * 1024) {  // > 50GB
            return 200L * 1024 * 1024; // 200MB
        } else if (fileSize > 10L * 1024 * 1024 * 1024) {  // > 10GB
            return 100L * 1024 * 1024; // 100MB
        } else {  // <= 10GB
            return 50L * 1024 * 1024; // 50MB
        }
    }

    /**
     * Checks if the resource is already onboarded as STAC.
     *
     * @param requestInput JSON object containing request details.
     * @return A future indicating the success or failure of the check.
     */
    private Future<Void> checkResourceOnboardedAsStac(JsonObject requestInput) {
        Promise<Void> promise = Promise.promise();
        String resourceId = requestInput.getString("resourceId");

        pgPool.preparedQuery(COLLECTION_TYPE_QUERY)
                .execute(Tuple.of(resourceId), ar -> {
                    if (ar.succeeded()) {
                        RowSet<Row> rows = ar.result();
                        if (rows.iterator().next().getInteger(0) > 0) {
                            LOGGER.info("Resource {} is onboarded as a STAC item.", resourceId);
                            promise.complete();
                        } else {
                            LOGGER.error(RESOURCE_NOT_ONBOARDED_MESSAGE);
                            promise.fail(new OgcException(404, "Not Found", RESOURCE_NOT_ONBOARDED_MESSAGE));
                        }
                    } else {
                        LOGGER.error("Failed to query collection_type table: {}", ar.cause().getMessage());
                        promise.fail(new OgcException(500, "Internal Server Error", "Error checking collection_type table."));
                    }
                });

        return promise.future();
    }

    /**
     * Checks if the item exists within the resource.
     *
     * @param requestInput JSON object containing request details.
     * @return A future indicating the success or failure of the check.
     */
    private Future<Void> checkItemExists(JsonObject requestInput) {
        Promise<Void> promise = Promise.promise();
        String resourceId = requestInput.getString("resourceId");
        String itemId = requestInput.getString("itemId");

        // Skip the check if itemId is null or empty
        if (itemId == null || itemId.isEmpty()) {
            LOGGER.info(SKIP_ITEM_EXISTENCE_CHECK_MESSAGE);
            promise.complete();
            return promise.future();
        }

        pgPool.preparedQuery(ITEM_EXISTENCE_CHECK_QUERY)
                .execute(Tuple.of(resourceId, itemId), ar -> {
                    if (ar.succeeded()) {
                        RowSet<Row> rows = ar.result();
                        int count = rows.iterator().next().getInteger(0);
                        if (count > 0) {
                            LOGGER.info("Item {} is associated with resource {}. Proceeding with the process.", itemId, resourceId);
                            promise.complete();
                        } else {
                            LOGGER.error("No item with ID {} is associated with resource {}", itemId, resourceId);
                            promise.fail(new OgcException(404, "Not Found", ITEM_NOT_EXISTS_MESSAGE));
                        }
                    } else {
                        LOGGER.error("Failed to query stac_collections_part: {}", ar.cause().getMessage());
                        promise.fail(new OgcException(500, "Internal Server Error", "Error checking stac_collections_part."));
                    }
                });

        return promise.future();
    }

    /**
     * Checks if the object already exists in S3 using a HEAD request.
     * If the object exists, fail the process. If not, proceed with initiating multipart upload process.
     *
     * @param requestInput The input JSON object containing the key of the object in S3.
     * @return A {@link Future<Boolean>} that completes with {@code true} if the object does not exist,
     *         or {@code false} if the object exists.
     */
    private Future<Boolean> checkIfObjectExistsInS3(JsonObject requestInput) {
        Promise<Boolean> promise = Promise.promise();

        String objectKeyName = requestInput.getString("s3KeyName");
        LOGGER.debug("Checking existence of object: {}", objectKeyName);

        // Construct the URL for the object
        String urlString = dataFromS3.getFullyQualifiedUrlString(objectKeyName);
        LOGGER.debug("Constructed URL: {}", urlString);
        dataFromS3.setUrlFromString(urlString);
        dataFromS3.setSignatureHeader(HttpMethod.HEAD);

        // Send the HEAD request to check if the object exists
        dataFromS3.getDataFromS3(HttpMethod.HEAD)
                .onSuccess(responseFromS3 -> {
                    if (responseFromS3.statusCode() == 200) {
                        // Object already exists in S3
                        LOGGER.error("Object already exists in S3: {}", objectKeyName);
                        promise.complete(false);
                    }
                })
                .onFailure(failure -> {
                    if (failure instanceof OgcException) {
                        OgcException ogcEx = (OgcException) failure;
                        if (ogcEx.getStatusCode() == 404) {
                            // Object does not exist in S3
                            LOGGER.debug("Object does not exist in S3: {}", objectKeyName);
                            promise.complete(true);
                        } else {
                            LOGGER.error("Failed to check S3 object existence: {}", ogcEx.getMessage());
                            promise.fail(failure);
                        }
                    } else {
                        // General failure case
                        LOGGER.error("Error while checking object existence in S3: {}", failure.getMessage());
                        promise.fail(failure);
                    }
                });

        return promise.future();
    }

    /**
     * Initiates a multipart upload on S3.
     *
     * @param bucket   The S3 bucket name.
     * @param key      The S3 object key.
     * @param filename The original file name.
     * @param filetype The content type of the file.
     * @return A future that resolves with the upload ID.
     */
    private Future<String> initiateMultipartUpload(String bucket, String key, String filename, String filetype) {
        Promise<String> promise = Promise.promise();

        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(filetype)
                .metadata(Map.of(
                        "filename", filename,
                        "content-type", filetype
                ))
                .build();
        try {
            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createRequest);
            String uploadId = response.uploadId();
            LOGGER.info("Multipart upload initiated: Upload ID = {}", uploadId);
            promise.complete(uploadId);
        } catch (Exception e) {
            LOGGER.error("Failed to initiate S3 multipart upload: {}", e.getMessage(), e);
            promise.fail(new OgcException(500, "Internal Server Error", INITIATE_MULTIPART_UPLOAD_FAILURE_MESSAGE));
        }

        return promise.future();
    }

    /**
     * Generates pre-signed URLs for each part of the multipart upload.
     *
     * @return A future containing the JSON response with pre-signed URLs.
     */
    private Future<JsonObject> generatePresignedUrls(String bucket, String key, final String uploadId, int totalParts, long chunkSize) {
        Promise<JsonObject> promise = Promise.promise();
        List<JsonObject> presignedUrls = new ArrayList<>();

        try {
            for (int i = 1; i <= totalParts; i++) {
                final int partNumber = i;

                PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(r -> r
                        .signatureDuration(Duration.ofMinutes(15))
                        .uploadPartRequest(UploadPartRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .build()));

                presignedUrls.add(new JsonObject()
                        .put("partNumber", partNumber)
                        .put("url", presignedRequest.url().toString()));
            }

            JsonObject response = new JsonObject()
                    .put("uploadId", uploadId)
                    .put("chunkSize", chunkSize)
                    .put("totalParts", totalParts)
                    .put("presignedUrls", new JsonArray(presignedUrls));

            LOGGER.info("Generated presigned URLs for {} parts.", totalParts);
            LOGGER.info("Response of multipart upload initiation: {}", response);

            promise.complete(response);
        } catch (Exception e) {
            LOGGER.error("Failed to generate presigned URLs for multipart upload: {}", e.getMessage(), e);
            promise.fail(new OgcException(500, "Internal Server Error", GENERATE_PRESIGNED_URLS_FAILURE_MESSAGE));
        }

        return promise.future();
    }

    /**
     * Handles failure by updating the job status to "FAILED" in the jobs_table and logging the error.
     *
     * @param requestInput   The JSON object containing request details.
     * @param failureHandler The exception that caused the failure.
     * @param promise        The promise to complete with the failure.
     */
    private void handleFailure(JsonObject requestInput, Throwable failureHandler, Promise<JsonObject> promise) {

        utilClass.updateJobTableStatus(requestInput, Status.FAILED, failureHandler.getMessage())
                .onSuccess(successHandler -> {
                    LOGGER.error("Process failed: {}", failureHandler.getMessage());
                    promise.fail(failureHandler);
                })
                .onFailure(jobStatusFailureHandler -> {
                    LOGGER.error(HANDLE_FAILURE_MESSAGE + ": {}", jobStatusFailureHandler.getMessage());
                    promise.fail(jobStatusFailureHandler);
                });
    }

    /**
     * Calculates the progress percentage based on steps completed.
     */
    private float calculateProgress(int step) {
        return (float) (step * 100) / 6;
    }
}
