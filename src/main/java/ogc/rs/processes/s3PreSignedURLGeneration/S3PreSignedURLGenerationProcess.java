package ogc.rs.processes.s3PreSignedURLGeneration;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;

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

import static ogc.rs.processes.s3PreSignedURLGeneration.Constants.*;

/**
 * This class handles the process of generating an S3 Pre-Signed URL and updating the status
 * of the process in a PostgreSQL database.
 */
public class S3PreSignedURLGenerationProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(S3PreSignedURLGenerationProcess.class);
    private final UtilClass utilClass;
    private String accessKey;
    private String secretKey;

    /**
     * Constructor to initialize the S3PreSignedURLGenerationProcess with PostgreSQL pool and configuration.
     *
     * @param pgPool  The PostgreSQL connection pool.
     * @param config  The configuration containing AWS and database details.
     */
    public S3PreSignedURLGenerationProcess(PgPool pgPool, JsonObject config) {
        this.utilClass = new UtilClass(pgPool);
        initializeConfig(config);
    }

    /**
     * Initializes the AWS credentials from the provided configuration.
     *
     * @param config The configuration object containing the AWS credentials.
     */
    private void initializeConfig(JsonObject config) {
        this.accessKey = config.getString("awsAccessKey");
        this.secretKey = config.getString("awsSecretKey");
    }

    /**
     * Executes the Pre-Signed URL process. It updates the progress and status of the process
     * in the job table at various stages.
     *
     * @param requestInput The input JSON object containing details like AWS bucket name, region, and object key name.
     * @return A Future object containing the result of the process execution.
     */
    @Override
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> objectPromise = Promise.promise();

        // Update initial progress
        requestInput.put("progress", calculateProgress(1));

        // Chain the process steps and handle success or failure
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, STARTING_PRE_SIGNED_URL_PROCESS_MESSAGE)
                .compose(progressUpdateHandler -> generatePreSignedUrl(requestInput))
                .compose(preSignedURLHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2))
                                .put("message", S3_PRE_SIGNED_URL_GENERATOR_MESSAGE)
                                .put("preSignedUrl", preSignedURLHandler.getString("preSignedUrl"))))
                .compose(progressUpdateHandler -> utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL, requestInput.getString("preSignedUrl")))
                .onSuccess(successHandler -> {
                    LOGGER.debug(S3_PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE);
                    objectPromise.complete();
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(S3_PRE_SIGNED_URL_PROCESS_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler.getMessage(), objectPromise);
                });

        return objectPromise.future();
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
            Region region = Region.of(requestInput.getString("awsRegion"));
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            S3Presigner preSigner = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();

            // Create the S3 PutObjectRequest
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(requestInput.getString("awsBucketName"))
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
        } catch (Exception e) {
            LOGGER.error("Failed to generate pre-signed URL", e);
            promise.fail(e);
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
     * Calculates the progress percentage based on the current step in the process.
     *
     * @param currentStep The current step number.
     * @return The progress percentage.
     */
    private float calculateProgress(int currentStep) {
        return ((float) currentStep / 3) * 100;
    }
}
