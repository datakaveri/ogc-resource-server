package ogc.rs.processes.s3PreSignedUrlGenerationForStaconboarding;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.common.DataFromS3;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
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
    private final PgPool pgPool;

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
        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
        this.collectionOnboarding = new CollectionOnboardingProcess(pgPool, webClient, config, dataFromS3, vertx);
        this.s3PresignedURL = new S3PresignedUrlForFeatureOnboardingProcess(pgPool, webClient, config);
    }

    /**
     * Executes the process for generating a pre-signed URL for S3.
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

        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, PROCESS_START_MESSAGE)
                .compose(progressUpdate -> collectionOnboarding.makeCatApiRequest(requestInput))
                .compose(resourceOwnershipHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2)).put(MESSAGE, RESOURCE_OWNERSHIP_CHECK_MESSAGE)))
                .compose(progressUpdate -> checkResourceOnboardedAsStac(requestInput))
                .compose(checkResourceOnboardedAsStacHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3)).put(MESSAGE, STAC_RESOURCE_ONBOARDED_MESSAGE)))
                .compose(progressUpdate -> checkItemExists(requestInput))
                .compose(checkItemExistsHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(4)).put(MESSAGE, ITEM_EXISTS_MESSAGE)))
                .compose(progressUpdate -> s3PresignedURL.generatePreSignedUrl(requestInput))
                .onSuccess(preSignedUrlResult -> {
                    LOGGER.info(PROCESS_COMPLETE_MESSAGE);

                    JsonObject result = new JsonObject()
                            .put("S3PreSignedUrl", preSignedUrlResult.getString("preSignedUrl"))
                            .put("s3ObjectKeyName", requestInput.getString("objectKeyName"))
                            .put("message", "Pre-Signed URL generation process for stac onboarding completed successfully.")
                            .put("status", "SUCCESSFUL");

                    utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL, PROCESS_COMPLETE_MESSAGE);
                    promise.complete(result);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(PROCESS_FAILURE_MESSAGE);
                    promise.fail(failureHandler);
                });

        return promise.future();
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
     * Calculates the progress percentage based on the current step.
     *
     * @param currentStep The current step in the process.
     * @return The progress percentage as a float.
     */
    private float calculateProgress(int currentStep) {
        return (float) (currentStep * 100) / 5;
    }
}