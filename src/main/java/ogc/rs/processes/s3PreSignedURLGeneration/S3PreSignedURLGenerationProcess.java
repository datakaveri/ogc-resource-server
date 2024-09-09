package ogc.rs.processes.s3PreSignedURLGeneration;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;

import ogc.rs.common.DataFromS3;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;

import static ogc.rs.processes.s3PreSignedURLGeneration.Constants.*;

/**
 * This class handles the process of generating an S3 Pre-Signed URL and updating the job status
 * of the process in a PostgreSQL database.
 */
public class S3PreSignedURLGenerationProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(S3PreSignedURLGenerationProcess.class);
    private final UtilClass utilClass;
    private final DataFromS3 dataFromS3;
    private final WebClient webClient;
    private String accessKey;
    private String secretKey;
    private String catServerHost;
    private String catRequestUri;
    private int catServerPort;

    /**
     * Constructor to initialize the S3PreSignedURLGenerationProcess with PostgreSQL pool, WebClient and configuration.
     *
     * @param pgPool  The PostgreSQL connection pool.
     * @param webClient  The WebClient instance for making HTTP requests.
     * @param config  The configuration containing AWS and database details.
     */
    public S3PreSignedURLGenerationProcess(PgPool pgPool, WebClient webClient, DataFromS3 dataFromS3, JsonObject config) {
        this.utilClass = new UtilClass(pgPool);
        this.webClient = webClient;
        initializeConfig(config);
        this.dataFromS3 = dataFromS3;
    }

    /**
     * Initializes the AWS credentials and CAT API configuration from the provided configuration.
     *
     * @param config The configuration object containing the AWS credentials and CAT API details.
     */
    private void initializeConfig(JsonObject config) {
        this.accessKey = config.getString("awsAccessKey");
        this.secretKey = config.getString("awsSecretKey");
        this.catRequestUri = config.getString("catRequestItemsUri");
        this.catServerHost = config.getString("catServerHost");
        this.catServerPort = config.getInteger("catServerPort");
    }

    // Map file types to extensions
    private static final Map<String, String> fileTypeMap = Map.of(
            "geopackage", ".gpkg" // Add more mappings as needed in the future
    );

    /**
     * Executes the Pre-Signed URL process. It updates the progress and status of the process
     * in the job table at various stages.
     *
     * @param requestInput The input JSON object containing details like AWS S3 bucket name, region and type of the file which gets uploaded into S3.
     * @return A Future object containing the result of the process execution.
     */
    @Override
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> objectPromise = Promise.promise();

        // Update initial progress
        requestInput.put("progress", calculateProgress(1));

        // Chain the process steps and handle success or failure
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, STARTING_PRE_SIGNED_URL_PROCESS_MESSAGE)
                .compose(progressUpdateHandler -> makeCatApiRequest(requestInput))
                .compose(catResponseHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2)).put(MESSAGE, CAT_REQUEST_RESPONSE)))
                .compose(progressUpdateHandler -> checkIfObjectExistsInS3(requestInput.getString("objectKeyName")))
                .compose(checkResult -> {
                    if (checkResult) {
                        return generatePreSignedUrl(requestInput);
                    } else {
                        return Future.failedFuture("Object already exists in S3.");
                    }
                })
                .compose(preSignedURLHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3))
                                .put(MESSAGE, S3_PRE_SIGNED_URL_GENERATOR_MESSAGE)
                                .put("preSignedUrl", preSignedURLHandler.getString("preSignedUrl"))))
                .compose(progressUpdateHandler -> utilClass.updateJobTableStatus(
                        requestInput, Status.SUCCESSFUL, S3_PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE))
                .onSuccess(successHandler -> {
                    // Pass the preSignedUrl as the output of the process
                    JsonObject result = new JsonObject()
                            .put("S3PreSignedUrl", requestInput.getString("preSignedUrl"));

                    LOGGER.debug(S3_PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE);
                    objectPromise.complete(result);  // Complete with result including preSignedUrl
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(S3_PRE_SIGNED_URL_PROCESS_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler.getMessage(), objectPromise);
                });

        return objectPromise.future();
    }

    /**
     * Fetches resource and resource group information from the CAT API based on the provided resourceId.
     * Validates the resource ownership and constructs the object key name.
     *
     * @param requestInput The input JSON object containing the resourceId, userId, and fileType.
     * @return A Future containing the updated requestInput with resource name, resource group name, and object key name.
     */
    public Future<JsonObject> makeCatApiRequest(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();

        // First API call to get resource info using resourceId
        webClient.get(catServerPort, catServerHost, catRequestUri)
                .addQueryParam("id", requestInput.getString("resourceId")).send()
                .onSuccess(responseFromCat -> {
                    if (responseFromCat.statusCode() == 200) {
                        JsonObject resourceInfo = responseFromCat.bodyAsJsonObject().getJsonArray("results").getJsonObject(0);

                        // Check resource ownership
                        String ownerUserId = resourceInfo.getString("ownerUserId");
                        if (!ownerUserId.equals(requestInput.getString("userId"))) {
                            LOGGER.error(RESOURCE_OWNERSHIP_ERROR);
                            promise.fail(RESOURCE_OWNERSHIP_ERROR);
                            return;
                        }

                        // Extract resourceId name (riName) and resourceGroup
                        requestInput.put("riName", resourceInfo.getString("name"));
                        String resourceGroupId = resourceInfo.getString("resourceGroup");

                        // Make second API call to get resourceGroup info
                        webClient.get(catServerPort, catServerHost, catRequestUri)
                                .addQueryParam("id", resourceGroupId).send()
                                .onSuccess(groupResponse -> {
                                    if (groupResponse.statusCode() == 200) {
                                        JsonObject groupInfo = groupResponse.bodyAsJsonObject().getJsonArray("results").getJsonObject(0);
                                        requestInput.put("rgName", groupInfo.getString("name"));

                                        // Now construct objectKeyName
                                        String fileType = requestInput.getString("fileType").toLowerCase(); // Convert to lowercase
                                        String fileExtension = fileTypeMap.getOrDefault(fileType, "");
                                        if (fileExtension.isEmpty()) {
                                            promise.fail("Unsupported file type");
                                            return;
                                        }

                                        String objectKeyName = requestInput.getString("rgName") + "/" + requestInput.getString("riName") + fileExtension;
                                        LOGGER.debug("Object key name is: {}", objectKeyName);
                                        requestInput.put("objectKeyName", objectKeyName);

                                        // Complete with the updated requestInput
                                        promise.complete(requestInput);
                                    } else {
                                        LOGGER.error("Resource group not found");
                                        promise.fail("Resource group not found");
                                    }
                                }).onFailure(failureResponse -> {
                                    LOGGER.error("Failed to fetch resource group: " + failureResponse.getMessage());
                                    promise.fail("Failed to fetch resource group");
                                });
                    } else {
                        LOGGER.error("Resource not found");
                        promise.fail("Resource not found");
                    }
                }).onFailure(failureResponse -> {
                    LOGGER.error("Failed to fetch resource: " + failureResponse.getMessage());
                    promise.fail("Failed to fetch resource");
                });

        return promise.future();
    }

    /**
     * Checks if the object already exists in S3 using a HEAD request.
     * If the object exists, fail the process. If not, proceed with URL generation.
     *
     * @param objectKeyName The key of the object in S3.
     * @return A {@link Future<Boolean>} that completes with {@code true} if the object does not exist,
     *         or fails with an appropriate error message if the object exists.
     */
    private Future<Boolean> checkIfObjectExistsInS3(String objectKeyName) {
        Promise<Boolean> promise = Promise.promise();
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
                        LOGGER.error("Object already exists in S3: {}", objectKeyName);
                        promise.fail(OBJECT_ALREADY_EXISTS_MESSAGE);
                    }
                })
                .onFailure(failure -> {
                    LOGGER.debug("Object does not exist in S3: {}", objectKeyName);
                    promise.complete(true);
                });

        return promise.future();
    }

    /**
     * Generates a Pre-Signed URL for the given S3 object using the AWS SDK.
     *
     * @param requestInput The input JSON object containing AWS details such as bucket name, region, and object key.
     * @return A Future containing a JSON object with the generated Pre-Signed URL.
     */
    private Future<JsonObject> generatePreSignedUrl(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        try {
            // Create AWS credentials and presigner
            Region region = Region.of(requestInput.getString("region"));
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

            try (S3Presigner preSigner = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build()) {

                // Create the S3 PutObjectRequest
                PutObjectRequest objectRequest = PutObjectRequest.builder()
                        .bucket(requestInput.getString("bucketName"))
                        .key(requestInput.getString("objectKeyName"))
                        .build();

                // Create the S3 Pre-Signed URL request
                PutObjectPresignRequest preSignRequest = PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10)) // Set URL expiration time
                        .putObjectRequest(objectRequest)
                        .build();

                // Generate the Pre-Signed URL
                PresignedPutObjectRequest preSignedRequest = preSigner.presignPutObject(preSignRequest);
                String preSignedUrl = preSignedRequest.url().toString();

                // Log and return the generated Pre-Signed URL
                JsonObject result = new JsonObject().put("preSignedUrl", preSignedUrl);
                LOGGER.debug("Generated pre-signed URL: {}", preSignedUrl);
                promise.complete(result);
            }
        } catch (Exception e) {
            LOGGER.error(S3_PRE_SIGNED_URL_GENERATOR_FAILURE_MESSAGE +  e);
            promise.fail(S3_PRE_SIGNED_URL_GENERATOR_FAILURE_MESSAGE);
        }
        return promise.future();
    }

    /**
     * Handles the failure scenario by updating the job table status to FAILED and logging the error.
     *
     * @param requestInput The input JSON object containing the process details.
     * @param errorMessage The error message to log and store.
     * @param promise      The promise to fail after handling the error.
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
     * Calculates the progress percentage of the process based on the current step.
     *
     * @param currentStep The current step of the process.
     * @return The progress percentage.
     */
    private float calculateProgress(int currentStep) {
        return (float) (currentStep * 100) / 5;
    }
}
