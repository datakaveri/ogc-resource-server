package ogc.rs.processes.presignedPostUrlForStacOnboarding;

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
import ogc.rs.common.S3Config;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static ogc.rs.processes.presignedPostUrlForStacOnboarding.Constants.*;

/**
 * The S3PresignedPostUrlGenerationProcess class handles the generation of
 * a pre-signed POST URL for onboarding files to an S3 bucket.
 * The process involves verifying the resource's onboarding status, checking item existence,
 * and generating the necessary credentials for secure file upload.
 */
public class S3PresignedPostUrlGenerationProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(S3PresignedPostUrlGenerationProcess.class);
    private final UtilClass utilClass;
    private final CollectionOnboardingProcess collectionOnboarding;
    private final PgPool pgPool;
    private S3Config s3conf;

    /**
     * Constructor to initialize S3PresignedPostUrlGenerationProcess.
     *
     * @param pgPool         Database connection pool.
     * @param webClient      Web client for external API calls.
     * @param config         Configuration object for S3 and other settings.
     * @param dataFromS3     Data retrieval utility from S3.
     * @param vertx          Vertx instance.
     */
    public S3PresignedPostUrlGenerationProcess(PgPool pgPool, WebClient webClient, JsonObject config, DataFromS3 dataFromS3, Vertx vertx) {
        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
        this.collectionOnboarding = new CollectionOnboardingProcess(pgPool, webClient, config, dataFromS3, vertx);
        initializeConfig(config);
    }

    /**
     * Initializes the S3 configuration using the provided config JSON object.
     *
     * @param config Configuration object containing S3 settings.
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
     * Executes the process for generating a pre-signed POST URL for S3.
     *
     * @param requestInput JSON object containing request details like collectionId and itemId.
     * @return A future containing the result of the process.
     */
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        String resourceId = requestInput.getString("collectionId");
        requestInput.put("resourceId", resourceId);
        String s3KeyName = resourceId + "/" + requestInput.getString("itemId") + "/";
        requestInput.put("s3KeyName", s3KeyName);
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
                .compose(progressUpdate -> generatePresignedPostUrl(requestInput))
                .onSuccess(result -> {
                    LOGGER.info(PROCESS_COMPLETE_MESSAGE);
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
     * Generates the pre-signed POST URL for the S3 bucket.
     *
     * @param requestInput JSON object containing request details.
     * @return A future containing the pre-signed URL and other metadata.
     */
    private Future<JsonObject> generatePresignedPostUrl(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();

        try {
            Map<String, String> fields = new HashMap<>();
            String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(Instant.now());
            String amzDate = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(Instant.now());
            String s3KeyName = requestInput.getString("s3KeyName");

            fields.put("bucket", s3conf.getBucket());
            fields.put("key", s3KeyName + "/${filename}");
            fields.put("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
            fields.put("X-Amz-Credential", s3conf.getAccessKey() + "/" + date + "/" + s3conf.getRegion() + "/s3/aws4_request");
            fields.put("X-Amz-Date", amzDate);

            String policy = createPolicy(fields, s3KeyName);
            fields.put("Policy", policy);

            String signature = signPolicy(policy, date, s3conf.getSecretKey(), s3conf.getRegion());
            fields.put("X-Amz-Signature", signature);

            JsonObject response = new JsonObject()
                    .put("policy", policy)
                    .put("status", "SUCCESSFUL")
                    .put("message", "Post Pre-Signed URL generation process completed successfully.")
                    .put("s3KeyName", s3KeyName)
                    .put("X-Amz-Date", amzDate)
                    .put("s3BucketName", s3conf.getBucket())
                    .put("X-Amz-Algorithm", "AWS4-HMAC-SHA256")
                    .put("X-Amz-Signature", signature)
                    .put("X-Amz-Credential", fields.get("X-Amz-Credential"));

            promise.complete(response);
        } catch (Exception e) {
            LOGGER.error(S3_PRE_SIGNED_POST_URL_GENERATOR_FAILURE_MESSAGE +  e.getMessage());
            promise.fail(new OgcException(500, "Internal Server Error", S3_PRE_SIGNED_POST_URL_GENERATOR_FAILURE_MESSAGE));
        }
        return promise.future();
    }

    /**
     * Creates an encoded policy for the pre-signed POST request.
     *
     * @param fields   Fields required for the policy.
     * @param s3KeyName S3 key name prefix.
     * @return Encoded policy as a string.
     */
    private String createPolicy(Map<String, String> fields, String s3KeyName) {
        String expiration = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(600));  // Expire in 10 minutes
        String policyDoc = "{ \"expiration\": \"" + expiration + "\", \"conditions\": ["
                + "{\"bucket\": \"" + s3conf.getBucket() + "\"},"
                + "[\"starts-with\", \"$key\", \"" + s3KeyName + "/\"],"
                + "{\"x-amz-algorithm\": \"AWS4-HMAC-SHA256\"},"
                + "{\"x-amz-credential\": \"" + fields.get("X-Amz-Credential") + "\"},"
                + "{\"x-amz-date\": \"" + fields.get("X-Amz-Date") + "\"}"
                + "] }";

        return Base64.getEncoder().encodeToString(policyDoc.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Signs the policy document using the AWS Signature Version 4 algorithm.
     *
     * @param policy    The policy document.
     * @param date      The date in 'yyyyMMdd' format.
     * @param secretKey The secret access key for signing.
     * @param region    AWS region for the S3 bucket.
     * @return The signed policy.
     * @throws Exception If an error occurs during signing.
     */
    private String signPolicy(String policy, String date, String secretKey, String region) throws Exception {
        byte[] signingKey = getSignatureKey(secretKey, date, region, "s3");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
        return bytesToHex(mac.doFinal(policy.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Generates the AWS signature key.
     *
     * @param key     The secret access key.
     * @param date    The date in 'yyyyMMdd' format.
     * @param region  AWS region.
     * @param service AWS service, e.g., "s3".
     * @return The derived signing key.
     * @throws Exception If an error occurs during key generation.
     */
    private byte[] getSignatureKey(String key, String date, String region, String service) throws Exception {
        byte[] kDate = hmacSHA256(("AWS4" + key).getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = hmacSHA256(kDate, region);
        byte[] kService = hmacSHA256(kRegion, service);
        return hmacSHA256(kService, "aws4_request");
    }

    /**
     * Computes the HMAC-SHA256 hash of the given data with the given key.
     *
     * @param key  The secret key.
     * @param data The data to hash.
     * @return The computed hash.
     * @throws Exception If an error occurs during hashing.
     */
    private byte[] hmacSHA256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes The byte array.
     * @return The hexadecimal representation of the byte array.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
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
