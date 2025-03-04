package ogc.rs.processes.s3MultiPartUploadForStacOnboarding;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.common.S3Config;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static ogc.rs.processes.s3MultiPartUploadForStacOnboarding.Constants.*;

/**
 * Process for completing a multipart upload to S3.
 * This process finalizes an ongoing multipart upload by combining uploaded parts.
 */
public class S3CompleteMultiPartUploadProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(S3CompleteMultiPartUploadProcess.class);
    private final UtilClass utilClass;
    private final S3Client s3Client;
    private S3Config s3conf;

    /**
     * Initializes the S3CompleteMultiPartUploadProcess with S3 client and configuration.
     *
     * @param pgPool  PgPool instance for database interactions.
     * @param s3conf  S3 config of the bucket to be operated upon.
     */
    public S3CompleteMultiPartUploadProcess(PgPool pgPool, S3Config s3conf) {
        this.utilClass = new UtilClass(pgPool);
        this.s3conf = s3conf;
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
    }

    /**
     * Executes the multipart upload completion process.
     *
     * @param requestInput JSON object containing upload details.
     * @return Future with the result of the completion process.
     */
    @Override
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, COMPLETE_MULTIPART_UPLOAD_PROCESS_START_MESSAGE)
                .compose(progressUpdate -> completeMultipartUpload(requestInput))
                .compose(result -> utilClass.updateJobTableStatus(
                        requestInput, Status.SUCCESSFUL, COMPLETE_MULTIPART_UPLOAD_PROCESS_SUCCESS_MESSAGE
                ).map(result))
                .onSuccess(result -> {
                    LOGGER.info(COMPLETE_MULTIPART_UPLOAD_PROCESS_SUCCESS_MESSAGE);
                    promise.complete(result);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(COMPLETE_MULTIPART_UPLOAD_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler, promise);
                });

        return promise.future();
    }

    /**
     * Completes a multipart upload by assembling uploaded parts.
     *
     * @param requestInput JSON object containing uploadId, filePath, and parts.
     * @return Future with success or failure result of the multipart upload completion.
     */
    public Future<JsonObject> completeMultipartUpload(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        String uploadId = requestInput.getString("uploadId");
        String key = requestInput.getString("filePath");

        List<CompletedPart> completedParts = new ArrayList<>();
        JsonArray parts = requestInput.getJsonArray("parts");
        LOGGER.info("Upload ID: {}", uploadId);
        LOGGER.info("Bucket: {}", s3conf.getBucket());
        LOGGER.info("Key (filePath): {}", key);
        LOGGER.info("Parts Received for Completion: {}", parts);

        // Parsing the parts array and constructing the list of CompletedPart
        for (Object partObj : parts) {
            String partString = (String) partObj; // Convert from JSON string
            String[] partDetails = partString.split(":"); // Split into [partNumber, eTag]

            if (partDetails.length == 2) {
                int partNumber = Integer.parseInt(partDetails[0]); // Extract part number
                String eTag = partDetails[1]; // Extract eTag

                completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(eTag)
                        .build());
            } else {
                LOGGER.error("Invalid part format: {}", partString);
                promise.fail(new OgcException(400, "Bad Request", INVALID_PART_FORMAT_MESSAGE));
                return promise.future();
            }
        }

        // Creating the complete multipart upload request
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(s3conf.getBucket())
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build();

        try {
            CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(completeRequest);
            LOGGER.info("Multipart upload completed successfully: {}", response.location());
            promise.complete(new JsonObject()
                    .put("message", COMPLETE_MULTIPART_UPLOAD_PROCESS_SUCCESS_MESSAGE)
                    .put("location", response.location()));
        } catch (Exception e) {
            LOGGER.error("Failed to complete S3 multipart upload completion: {}", e.getMessage(), e);
            promise.fail(new OgcException(500, "Internal Server Error", COMPLETE_MULTIPART_UPLOAD_FAILURE_MESSAGE));
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
}
