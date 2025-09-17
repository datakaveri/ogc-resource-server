package ogc.rs.processes.s3PreSignedURLGeneration;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.authentication.util.DxUser;
import ogc.rs.catalogue.CatalogueInterface;
import ogc.rs.common.S3Config;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.util.Status;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static ogc.rs.processes.s3MultiPartUploadForStacOnboarding.Constants.HANDLE_FAILURE_MESSAGE;
import static ogc.rs.processes.s3PreSignedURLGeneration.Constants.*;

/**
 * This class handles the process of generating an S3 Pre-Signed URL and updating the job status
 * of the process in a PostgreSQL database.
 */
public class S3PreSignedURLGenerationProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(S3PreSignedURLGenerationProcess.class);
    private final UtilClass utilClass;
    private final WebClient webClient;
    private final Pool pgPool;
    private S3Config s3conf;
    private String catServerHost;
    private String catRequestUri;
    private int catServerPort;
    private CatalogueInterface catalogueService;

    /**
     * Constructor to initialize the S3PreSignedURLGenerationProcess with PostgreSQL pool, WebClient and configuration.
     *
     * @param pgPool  The PostgreSQL connection pool.
     * @param webClient  The WebClient instance for making HTTP requests.
     * @param config  The configuration containing AWS and database details.
     * @param s3conf  The S3Config instance i.e. config to access bucket requested in process input.
     */
    public S3PreSignedURLGenerationProcess(Pool pgPool, WebClient webClient, JsonObject config, S3Config s3conf,
                                           CatalogueInterface catalogueService) {
        this.catalogueService = catalogueService;
        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
        this.webClient = webClient;
        this.s3conf = s3conf;
        initializeConfig(config);
    }

    /**
     * Initializes the AWS credentials and CAT API configuration from the provided configuration.
     *
     * @param config The configuration object containing the AWS credentials and CAT API details.
     */
    private void initializeConfig(JsonObject config) {

        this.catRequestUri = config.getString("catRequestItemsUri");
        this.catServerHost = config.getString("catServerHost");
        this.catServerPort = config.getInteger("catServerPort");
    }

    // Map file types to extensions
    private static final Map<String, String> fileTypeMap = Map.of(
            "geopackage", ".gpkg"
    );
    // Map file types to item types
    private static final Map<String, String> entityTypeMap = Map.of(
            "geopackage", "FEATURE"
    );

    /**
     * Executes the Pre-Signed URL process. It updates the progress and status of the process
     * in the job table at various stages.
     *
     * @param requestInput
     * The input JSON object containing details like ResourceId,
     * AWS S3 bucket name, region and type of the file which gets uploaded into S3.
     *
     * @return A Future object containing the result of the process execution.
     */
    @Override
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> objectPromise = Promise.promise();
      JsonObject providerJson = requestInput.getJsonObject("user");
      DxUser user = DxUser.fromJsonObject(providerJson);
      String resourceId = requestInput.getString("resourceId");

        // Update initial progress
        requestInput.put("progress", calculateProgress(1));

       // Chain the process steps and handle success or failure
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, STARTING_PRE_SIGNED_URL_PROCESS_MESSAGE)
                .compose(progressUpdateHandler -> checkResourceOwnershipAndBuildS3ObjectKey(resourceId, user, requestInput))
                .compose(catResponseHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2)).put(MESSAGE, CAT_REQUEST_RESPONSE)))
                // Check if the resource has already been onboarded in collection_type table
                .compose(progressUpdateHandler -> checkIfResourceOnboarded(requestInput))
                .compose(onboarded -> {
                    if (onboarded) {
                        // Resource is already onboarded, block re-upload
                        return Future.failedFuture(new OgcException(409, "Conflict", RESOURCE_ALREADY_EXISTS_MESSAGE));
                    } else {
                        return generatePreSignedUrl(requestInput);
                    }
                })
                .compose(preSignedURLHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3))
                                .put(MESSAGE, S3_PRE_SIGNED_URL_GENERATOR_MESSAGE)
                                .put("preSignedUrl", preSignedURLHandler.getString("preSignedUrl"))))
                .compose(progressUpdateHandler -> utilClass.updateJobTableStatus(
                        requestInput, Status.SUCCESSFUL, S3_PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE))
                .onSuccess(successHandler -> {
                    // Pass the preSignedUrl, s3ObjectKeyName and message as the output of the process
                    JsonObject result = new JsonObject()
                            .put("S3PreSignedUrl", requestInput.getString("preSignedUrl"))
                            .put("s3ObjectKeyName", requestInput.getString("objectKeyName"))
                            .put("message", S3_PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE);
                    LOGGER.debug(S3_PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE);
                    objectPromise.complete(result);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(S3_PRE_SIGNED_URL_PROCESS_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler, objectPromise);
                });

        return objectPromise.future();
    }


  /**
   * Handles the process of making catalogue API request, checking resource ownership,
   * and constructing the object key name.
   *
   * @param requestInput The input JSON object containing details like resourceId, userId, and fileType.
   * @return A Future containing the updated requestInput with objectKeyName, or an error message.
   */
  public Future<JsonObject> checkResourceOwnershipAndBuildS3ObjectKey(String resourceId, DxUser user, JsonObject requestInput) {
    /*Calling catalogue to get information about resourceId  */
    Promise<JsonObject> promise = Promise.promise();
    catalogueService.getCatalogueAsset(resourceId).onSuccess(catAsset -> {
      if (catAsset == null) {
        promise.fail(new OgcException(404, "Not Found", "Item Not Found"));
        return;
      }
      /* set asset information in routing context helper */
      String ownerUserId = catAsset.getProviderId();
      boolean isProviderTheResourceOwner = ownerUserId.equals(user.getSub().toString());
      boolean IsOrganizationIdSame = catAsset.getOrganizationId().equals(user.getOrganisationId());

      if (!isProviderTheResourceOwner && !IsOrganizationIdSame) {
        LOGGER.error("Ownership check failed for resourceId:{} , isProviderTheResourceOwner:" +
            " {}, IsOrganizationIdSame : {}", resourceId, isProviderTheResourceOwner, IsOrganizationIdSame);
        promise.fail(new OgcException(403, "Forbidden", RESOURCE_OWNERSHIP_ERROR));
        return;
      }
      /*TODO: No resource group to add as a prefix to filename **/

      // Determine file extension based on file type
      String fileType = requestInput.getString("fileType").toLowerCase();
      String fileExtension = fileTypeMap.getOrDefault(fileType, "");
      if (fileExtension.isEmpty()) {
        // Unsupported file type
        promise.fail(new OgcException(415, "Unsupported Media Type", UNSUPPORTED_FILE_TYPE_ERROR));
        return;
      }

      // Construct object key name using resourceId, resourceGroup, and file extension
      String objectKeyName = resourceId + fileExtension;

      LOGGER.debug("Object key name is: {}", objectKeyName);
      requestInput.put("objectKeyName", objectKeyName);
      promise.complete(requestInput);
    }).onFailure(err -> {
      LOGGER.error("Failed to fetch item metadata: {}", err.getMessage());
      promise.fail(new OgcException(500, "Internal Server Error", "Error fetching item metadata"));
    });

    return promise.future();
  }


    /**
     * Checks if a resource has been onboarded by querying the `collection_type` table.
     * Maps the provided `fileType` to an entity type and checks if a row with the given `collectionId`
     * and entity type exists.
     *
     * @param requestInput JSON object containing "resourceId" and "fileType"
     * @return Future<Boolean> indicating `true` if the resource is onboarded, or `false` otherwise.
     */
    private Future<Boolean> checkIfResourceOnboarded(JsonObject requestInput) {
        Promise<Boolean> promise = Promise.promise();

        // Get the resourceId (collection_id) and map the fileType to entity type
        String collectionId = requestInput.getString("resourceId");
        String fileType = requestInput.getString("fileType").toLowerCase();
        String entityType = entityTypeMap.getOrDefault(fileType, "");

        if (entityType.isEmpty()) {
            // Unsupported file type
            return Future.failedFuture(new OgcException(415, "Unsupported Media Type", UNSUPPORTED_FILE_TYPE_ERROR));
        }

        pgPool.preparedQuery(COLLECTION_TYPE_QUERY)
                .execute(Tuple.of(collectionId, entityType))
                .onSuccess(rows -> {
                    Row row = rows.iterator().next();
                    long count = row.getLong(0);
                    if (count > 0) {
                        // Resource is already onboarded
                        promise.complete(true);
                    } else {
                        // Resource is not onboarded yet
                        promise.complete(false);
                    }
                })
                .onFailure(failure -> {
                    LOGGER.error("Error checking collection_type table: {}", failure.getMessage());
                    promise.fail(new OgcException(500, "Internal Server Error", "Error checking collection_type table."));
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
            Region region = Region.of(s3conf.getRegion());
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(s3conf.getAccessKey(), s3conf.getSecretKey());

            try (S3Presigner preSigner = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .endpointOverride(URI.create(s3conf.getEndpoint()))
                    .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3conf.isPathBasedAccess())
                        .build())
                    .build()) {

                // Create the S3 PutObjectRequest
                PutObjectRequest objectRequest = PutObjectRequest.builder()
                        .bucket(s3conf.getBucket())
                        .key(requestInput.getString("objectKeyName"))
                        .build();

                // Create the S3 Pre-Signed URL request
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
            }
        } catch (Exception e) {
            LOGGER.error(S3_PRE_SIGNED_URL_GENERATOR_FAILURE_MESSAGE +  e);
            promise.fail(new OgcException(500, "Internal Server Error", S3_PRE_SIGNED_URL_GENERATOR_FAILURE_MESSAGE));
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
     * Calculates the progress percentage of the process based on the current step.
     *
     * @param currentStep The current step of the process.
     * @return The progress percentage.
     */
    private float calculateProgress(int currentStep) {
        return (float) (currentStep * 100) / 5;
    }
}
