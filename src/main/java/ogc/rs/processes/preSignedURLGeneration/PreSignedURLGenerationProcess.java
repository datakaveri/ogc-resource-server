package ogc.rs.processes.preSignedURLGeneration;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;

import ogc.rs.common.DataFromS3;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.util.Status;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.processes.util.UtilClass;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static ogc.rs.processes.preSignedURLGeneration.Constants.*;

/**
 * This class handles the process of generating an S3 Pre-Signed URL and updating the job status
 * of the process in a PostgreSQL database.
 */
public class PreSignedURLGenerationProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(PreSignedURLGenerationProcess.class);
    private final boolean isMinIO;
    private final UtilClass utilClass;
    private final DataFromS3 dataFromS3;
    private final WebClient webClient;
    private String accessKey;
    private String secretKey;
    private String awsEndPoint;
    private String catServerHost;
    private String catRequestUri;
    private int catServerPort;

    /**
     * Constructor to initialize the PreSignedURLGenerationProcess with PostgreSQL pool, WebClient and configuration.
     *
     * @param pgPool  The PostgreSQL connection pool.
     * @param webClient  The WebClient instance for making HTTP requests.
     * @param config  The configuration containing AWS and database details.
     */
    public PreSignedURLGenerationProcess(PgPool pgPool, WebClient webClient, DataFromS3 dataFromS3, JsonObject config) {
        this.isMinIO = config.getBoolean("isMinIO");
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
        this.awsEndPoint = config.getString("awsEndPoint");
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
     * @param requestInput
     * The input JSON object containing details like ResourceId,
     * AWS bucket name, region and type of the file which gets uploaded into S3/MinIO.
     *
     * @return A Future object containing the result of the process execution.
     */
    @Override
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> objectPromise = Promise.promise();

        // Update initial progress
        requestInput.put("progress", calculateProgress(1));

        // Chain the process steps and handle success or failure
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, STARTING_PRE_SIGNED_URL_PROCESS_MESSAGE)
                .compose(progressUpdateHandler -> checkResourceOwnershipAndBuildAWSObjectKey(requestInput))
                .compose(catResponseHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2)).put(MESSAGE, CAT_REQUEST_RESPONSE)))
                .compose(progressUpdateHandler -> checkIfObjectExistsInAwsBucket(requestInput))
                .compose(checkResult -> {
                    if (checkResult) {
                        return generatePreSignedUrl(requestInput);
                    } else {
                        return Future.failedFuture(new OgcException(409, "Conflict", OBJECT_ALREADY_EXISTS_MESSAGE));
                    }
                })
                .compose(preSignedURLHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3))
                                .put(MESSAGE, PRE_SIGNED_URL_GENERATOR_MESSAGE)
                                .put("preSignedUrl", preSignedURLHandler.getString("preSignedUrl"))))
                .compose(progressUpdateHandler -> utilClass.updateJobTableStatus(
                        requestInput, Status.SUCCESSFUL, PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE))
                .onSuccess(successHandler -> {
                    // Pass the preSignedUrl, AWSObjectKeyName and message as the output of the process
                    JsonObject result = new JsonObject()
                            .put("PreSignedUrl", requestInput.getString("preSignedUrl"))
                            .put("ObjectKeyName", requestInput.getString("objectKeyName"))
                            .put("message", PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE);

                    LOGGER.debug(PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE);
                    objectPromise.complete(result);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(PRE_SIGNED_URL_PROCESS_FAILURE_MESSAGE);
                    objectPromise.fail(failureHandler);
                });

        return objectPromise.future();
    }

    /**
     * Makes a generic API call to the catalogue server with the provided resourceID.
     *
     * @param id The resourceID to include as a query parameter.
     * @return A Future containing the response body as a JsonObject.
     */
    private Future<JsonObject> makeCatApiRequest(String id) {
        Promise<JsonObject> promise = Promise.promise();

        webClient.get(catServerPort, catServerHost, catRequestUri)
                .addQueryParam("id", id).send()
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject result = response.bodyAsJsonObject().getJsonArray("results").getJsonObject(0);
                        promise.complete(result);
                    } else {
                        promise.fail(new OgcException(404, "Not Found", ITEM_NOT_PRESENT_ERROR));
                    }
                })
                .onFailure(failureResponseFromCat -> {
                    LOGGER.error(CAT_RESPONSE_FAILURE + failureResponseFromCat.getMessage());
                    promise.fail(new OgcException(500, "Internal Server Error", CAT_RESPONSE_FAILURE));
                });

        return promise.future();
    }

    /**
     * Handles the process of making catalogue API requests, checking resource ownership,
     * and constructing the object key name.
     *
     * @param requestInput The input JSON object containing details like resourceId, userId, and fileType.
     * @return A Future containing the updated requestInput with objectKeyName, or an error message.
     */
    public Future<JsonObject> checkResourceOwnershipAndBuildAWSObjectKey(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();

        // Call catalogue API to get resource info
        makeCatApiRequest(requestInput.getString("resourceId"))
                .compose(resourceInfo -> {
                    // Check resource ownership using resourceId
                    String ownerUserId = resourceInfo.getString("ownerUserId");
                    if (!ownerUserId.equals(requestInput.getString("userId"))) {
                        // Ownership check failed
                        LOGGER.error(RESOURCE_OWNERSHIP_ERROR);
                        return Future.failedFuture(new OgcException(403, "Forbidden", RESOURCE_OWNERSHIP_ERROR));
                    }

                    // Extract resourceId name (riName) and call CAT API to get resourceGroup ID info
                    requestInput.put("riName", resourceInfo.getString("name"));
                    return makeCatApiRequest(resourceInfo.getString("resourceGroup"));
                })
                .compose(groupInfo -> {
                    // Extract resource group name and construct object key name
                    requestInput.put("rgName", groupInfo.getString("name"));

                    // Determine file extension based on file type
                    String fileType = requestInput.getString("fileType").toLowerCase();
                    String fileExtension = fileTypeMap.getOrDefault(fileType, "");
                    if (fileExtension.isEmpty()) {
                        // Unsupported file type
                        return Future.failedFuture(new OgcException(415, "Unsupported Media Type", UNSUPPORTED_FILE_TYPE_ERROR));
                    }

                    // Construct the object key name and update requestInput
                    String objectKeyName = requestInput.getString("rgName") + "/" +
                            requestInput.getString("riName") + fileExtension;
                    LOGGER.debug("Object key name is: {}", objectKeyName);
                    requestInput.put("objectKeyName", objectKeyName);

                    return Future.succeededFuture(requestInput);
                })

                // Success case: complete the promise with the updated requestInput
                .onSuccess(promise::complete)
                .onFailure(failure -> {
                    // Failure case: log the error and fail the promise
                    LOGGER.error(failure.getMessage());
                    promise.fail(failure);
                });

        return promise.future();
    }

    /**
     * Checks if the object already exists in S3/MinIO using a HEAD request.
     * If the object exists, fail the process. If not, proceed with URL generation.
     *
     * @param requestInput The input JSON object containing the key of the object in S3/MinIO.
     * @return A {@link Future<Boolean>} that completes with {@code true} if the object does not exist,
     *         or fails with an appropriate error message if the object exists.
     */
    private Future<Boolean> checkIfObjectExistsInAwsBucket(JsonObject requestInput) {
        Promise<Boolean> promise = Promise.promise();

        String objectKeyName = requestInput.getString("objectKeyName");
        LOGGER.debug("Checking existence of object: {}", objectKeyName);
        String urlString;
        if (isMinIO) {
            // Construct the URL for the object in MinIO
            urlString = awsEndPoint  + requestInput.getString("bucketName") + "/" + objectKeyName;
           }
           else {
               // Construct the URL for the object in S3
               urlString = dataFromS3.getFullyQualifiedUrlString(objectKeyName);
            }
        LOGGER.debug("Constructed URL: {}", urlString);
        dataFromS3.setUrlFromString(urlString);
        dataFromS3.setSignatureHeader(HttpMethod.HEAD);

        // Send the HEAD request to check if the object exists
        dataFromS3.getDataFromS3(HttpMethod.HEAD)
                .onSuccess(responseFromS3 -> {
                    if (responseFromS3.statusCode() == 200) {
                        LOGGER.error("Object already exists in AWS bucket: {}", objectKeyName);
                        promise.fail(new OgcException(409, "Conflict", OBJECT_ALREADY_EXISTS_MESSAGE));
                    }
                })
                .onFailure(failure -> {
                    LOGGER.debug("Object does not exist in AWS bucket: {}", objectKeyName);
                    promise.complete(true);
                });

        return promise.future();
    }

    /**
     * Generates a Pre-Signed URL for the given AWS object using the AWS SDK.
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
            S3Presigner preSigner;

            if(isMinIO){
                // MinIO pre-signed URL generation logic
                LOGGER.debug("Generating pre-signed URL using MinIO");
                preSigner = S3Presigner.builder()
                        .endpointOverride(URI.create(awsEndPoint))
                        .region(region)
                        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                        .serviceConfiguration(S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                        .build();
            }
            else {
                LOGGER.debug("Generating pre-signed URL using S3");
                preSigner = S3Presigner.builder()
                        .endpointOverride(URI.create(awsEndPoint))
                        .region(region)
                        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                        .build();
            }
                // Create the PutObjectRequest
                PutObjectRequest objectRequest = PutObjectRequest.builder()
                        .bucket(requestInput.getString("bucketName"))
                        .key(requestInput.getString("objectKeyName"))
                        .build();

                // Create the Pre-Signed URL request
                PutObjectPresignRequest preSignRequest = PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(5)) // Set URL expiration time
                        .putObjectRequest(objectRequest)
                        .build();

                // Generate the Pre-Signed URL
                PresignedPutObjectRequest preSignedRequest = preSigner.presignPutObject(preSignRequest);
                String preSignedUrl = preSignedRequest.url().toString();

                // Log and return the generated Pre-Signed URL
                JsonObject result = new JsonObject().put("preSignedUrl", preSignedUrl);
                LOGGER.debug("Generated pre-signed URL: {}", preSignedUrl);
                promise.complete(result);

        } catch (Exception e) {
            LOGGER.error(PRE_SIGNED_URL_GENERATOR_FAILURE_MESSAGE +  e);
            promise.fail(new OgcException(500, "Internal Server Error", PRE_SIGNED_URL_GENERATOR_FAILURE_MESSAGE));
        }
        return promise.future();
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
