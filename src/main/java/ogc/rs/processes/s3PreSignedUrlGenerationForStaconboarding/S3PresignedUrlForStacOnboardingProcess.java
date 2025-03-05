package ogc.rs.processes.s3PreSignedUrlGenerationForStaconboarding;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import ogc.rs.common.DataFromS3;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.s3MultiPartUploadForStacOnboarding.S3InitiateMultiPartUploadProcess;
import ogc.rs.processes.s3PreSignedUrlGenerationForFeatureOnboarding.S3PresignedUrlForFeatureOnboardingProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.processes.s3PreSignedUrlGenerationForStaconboarding.Constants.*;

/**
 * The S3PresignedUrlForStacOnboardingProcess class handles the generation of
 * a pre-signed URL for onboarding stac assets to an S3 bucket.
 * The process involves verifying the resource's onboarding status, checking item existence,
 * and generating the presigned url for secure stac asset upload.
 */
public class S3PresignedUrlForStacOnboardingProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(S3PresignedUrlForStacOnboardingProcess.class);
    private final UtilClass utilClass;
    private final CollectionOnboardingProcess collectionOnboarding;
    private final S3PresignedUrlForFeatureOnboardingProcess s3PresignedURL;
    private final S3InitiateMultiPartUploadProcess stacOnboardingChecks;

    /**
     * Constructor to initialize S3PresignedUrlForStacOnboardingProcess.
     *
     * @param pgPool         Database connection pool.
     * @param webClient      Web client for external API calls.
     * @param config         Configuration object for S3 and other settings.
     * @param dataFromS3     Data retrieval utility from S3.
     * @param vertx          Vertx instance.
     */
    public S3PresignedUrlForStacOnboardingProcess(PgPool pgPool, WebClient webClient, JsonObject config, DataFromS3 dataFromS3, Vertx vertx) {
        this.utilClass = new UtilClass(pgPool);
        this.collectionOnboarding = new CollectionOnboardingProcess(pgPool, webClient, config, dataFromS3, vertx);
        this.s3PresignedURL = new S3PresignedUrlForFeatureOnboardingProcess(pgPool, webClient, config);
        this.stacOnboardingChecks = new S3InitiateMultiPartUploadProcess(pgPool,webClient, config, dataFromS3, vertx);
    }

    /**
     * Executes the process for generating an S3 pre-signed URL for stac assets onboarding.
     *
     * @param requestInput JSON object containing request details like collectionId and itemId.
     * @return A future containing the result of the process.
     */
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        String resourceId = requestInput.getString("collectionId");
        requestInput.put("resourceId", resourceId);
        String assetName = requestInput.getString("fileName");
        String extension = requestInput.getString("fileExtension");
        String objectKeyName = resourceId + "/" + requestInput.getString("itemId") + "/" + assetName + "." + extension;
        requestInput.put("objectKeyName", objectKeyName);
        requestInput.put("progress", calculateProgress(1));

        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, S3_PRESIGNED_URL_PROCESS_START_MESSAGE)
                .compose(progressUpdate -> collectionOnboarding.makeCatApiRequest(requestInput))
                .compose(resourceOwnershipHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2)).put(MESSAGE, RESOURCE_OWNERSHIP_CHECK_MESSAGE)))
                .compose(progressUpdate -> stacOnboardingChecks.checkResourceOnboardedAsStac(requestInput))
                .compose(checkResourceOnboardedAsStacHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3)).put(MESSAGE, STAC_RESOURCE_ONBOARDED_MESSAGE)))
                .compose(progressUpdate -> stacOnboardingChecks.checkItemExists(requestInput))
                .compose(checkItemExistsHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(4)).put(MESSAGE, ITEM_EXISTS_MESSAGE)))
                .compose(progressUpdate ->stacOnboardingChecks.checkIfObjectExistsInS3(requestInput))
                .compose(objectDoesNotExistHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(5)).put(MESSAGE, OBJECT_DOES_NOT_EXIST_MESSAGE)))
                .compose(progressUpdate -> s3PresignedURL.generatePreSignedUrl(requestInput))
                .onSuccess(preSignedUrlResult -> {
                    LOGGER.info(S3_PRESIGNED_URL_PROCESS_SUCCESS_MESSAGE);

                    JsonObject result = new JsonObject()
                            .put("S3PreSignedUrl", preSignedUrlResult.getString("preSignedUrl"))
                            .put("s3ObjectKeyName", requestInput.getString("objectKeyName"))
                            .put("message", "Pre-Signed URL generation process for stac onboarding completed successfully.")
                            .put("status", "SUCCESSFUL");

                    utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL, S3_PRESIGNED_URL_PROCESS_SUCCESS_MESSAGE);
                    promise.complete(result);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(S3_PRESIGNED_URL_PROCESS_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler, promise);
                });

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
     * Calculates the progress percentage based on the current step.
     *
     * @param currentStep The current step in the process.
     * @return The progress percentage as a float.
     */
    private float calculateProgress(int currentStep) {
        return (float) (currentStep * 100) / 6;
    }

}