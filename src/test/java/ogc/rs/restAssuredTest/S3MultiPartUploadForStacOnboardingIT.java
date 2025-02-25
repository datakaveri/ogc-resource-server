package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.core.ConditionTimeoutException;

import static ogc.rs.common.Constants.*;
import static ogc.rs.processes.collectionOnboarding.Constants.RESOURCE_OWNERSHIP_ERROR;
import static org.awaitility.Awaitility.await;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static ogc.rs.processes.s3MultiPartUploadForStacOnboarding.Constants.*;
import static ogc.rs.restAssuredTest.Constant.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(RestAssuredConfigExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S3MultiPartUploadForStacOnboardingIT {

    private static final Logger LOGGER = LogManager.getLogger(S3MultiPartUploadForStacOnboardingIT.class);

    private static String uploadId;
    private static int chunkSize;
    private static List<Map<String, Object>> presignedUrls;
    private static final List<String> formattedParts = new ArrayList<>();

    String uploadEndpoint = "/processes/{processId}/execution";
    String jobStatusEndpoint = "/jobs/{jobId}";
    String initiateUploadProcessId = "06db0716-156f-4320-b6fd-e0a110523508";
    String completeUploadProcessId = "07e9f356-7894-48b4-adf7-1e0d8df0909a";
    String inValidProcessId = "9c6f7ff8-df7d-41b3-8d45-0283167df069";
    String RESOURCE_ID_STAC_TEST = "04be4cc1-39f9-4441-b32d-1e5767fa8f10";
    String ITEM_ID_STAC_TEST = "C3_PAN_20211108_2485193221";

    @BeforeAll
    public static void setup() throws IOException {
        // Set up files for the tests

        // Upload a test file to the root path /04be4cc1-39f9-4441-b32d-1e5767fa8f10
        File existingFile = new File("src/test/resources/processFiles/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/ExistingFile.gpkg");

        // Upload the same test file to the S3 path /04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/ExistingFile.gpkg
        given().port(PORT).body(existingFile).when().put( "bucket1/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/ExistingFile.gpkg")
                .then().statusCode(200);

    }

    @AfterAll
    public static void tearDown() throws IOException {

        given().port(PORT).when().delete( "bucket1/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/ExistingFile.gpkg")
                .then().statusCode(204);

        given().port(PORT).when().delete( "bucket1/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/TestFile.gpkg")
                .then().statusCode(204);

    }

    private JsonObject initiateUploadRequestBody() {
        JsonObject requestBody = new JsonObject();
        JsonObject inputs = new JsonObject();

        inputs.put("collectionId", RESOURCE_ID_STAC_TEST);
        inputs.put("itemId", ITEM_ID_STAC_TEST);
        inputs.put("bucketName", "bucket1");
        inputs.put("fileName", "TestFile.gpkg");
        inputs.put("fileType", "application/octet-stream");
        inputs.put("fileSize", 106496L);

        requestBody.put("inputs", inputs);
        requestBody.put("response", "raw");
        return requestBody;
    }

    private JsonObject completeUploadRequestBody(){
        JsonObject requestBody = new JsonObject();
        JsonObject inputs = new JsonObject();

        inputs.put("uploadId", uploadId);
        inputs.put("bucketName", "bucket1");
        inputs.put("filePath", "04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/TestFile.gpkg");
        inputs.put("parts", new JsonArray(formattedParts));

        requestBody.put("inputs", inputs);
        requestBody.put("response", "raw");
        return requestBody;
    }

    private String getToken() {
        return new FakeTokenBuilder().withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceServer().withRoleProvider().withCons(new JsonObject()).build();
    }

    private Response sendExecutionRequest(String processId, String token, JsonObject requestBody) {
        return RestAssured.given().pathParam("processId", processId).auth().oauth2(token)
                .contentType("application/json").body(requestBody.toString()).when().post(uploadEndpoint);
    }

    private Response sendJobStatusRequest(String jobId, String token) {
        return RestAssured.given().pathParam("jobId", jobId).auth().oauth2(token)
                .get(jobStatusEndpoint);
    }

    private void uploadFileParts(File file) {
        LOGGER.debug("Uploading file parts for multipart upload");

        if (!file.exists()) {
            fail("Test file does not exist: " + file.getAbsolutePath());
            return;
        }

        formattedParts.clear();

        for (Map<String, Object> presignedUrl : presignedUrls) {
            int partNumber = (int) presignedUrl.get("partNumber");
            String url = (String) presignedUrl.get("url");

            if (url.contains("s3.aws-region.amazonaws.com")) {
                url = url.replace("s3.aws-region.amazonaws.com", "localhost");
            }
            LOGGER.debug("Using URL for Part {}: {}", partNumber, url);

            // Determine the start and end byte for slicing
            long start = (long) (partNumber - 1) * chunkSize;
            long end = Math.min(file.length(), start + chunkSize);

            byte[] fileChunk;
            try {
                fileChunk = Files.readAllBytes(file.toPath());
                fileChunk = java.util.Arrays.copyOfRange(fileChunk, (int) start, (int) end);
            } catch (IOException e) {
                fail("Failed to read test file chunk: " + e.getMessage());
                return;
            }

            LOGGER.debug("Uploading Part {} | Bytes: {} - {}", partNumber, start, end);

            int maxRetries = 5;
            int attempt = 1;
            Response uploadResponse = null;

            while (attempt <= maxRetries) {
                uploadResponse = given()
                        .body(fileChunk)
                        .when()
                        .put(url);

                if (uploadResponse.statusCode() == 200) {
                    break;
                }

                LOGGER.warn("Retrying part {} (Attempt {}/{}) - Response: {}", partNumber, attempt, maxRetries, uploadResponse.body().asString());
                attempt++;

                try {
                    Thread.sleep((long) Math.pow(2, attempt) * 1000);
                } catch (InterruptedException ignored) {}
            }

            if (uploadResponse.statusCode() != 200) {
                fail("Failed to upload part " + partNumber + ": " + uploadResponse.body().asString());
                return;
            }

            String eTag = uploadResponse.getHeader("ETag");
            LOGGER.debug("ETag received for part {}: {}", partNumber, eTag);

            if (eTag == null) {
                fail("No ETag received for part " + partNumber);
                return;
            }

            formattedParts.add(partNumber + ":" + eTag.replace("\"", ""));
            LOGGER.debug("Successfully uploaded part {} | ETag: {}", partNumber, eTag);
        }
    }

    @Test
    @Order(1)
    @Description("Failure: Unauthorized user.")
    public void testExecuteUnauthorizedFail() {
        LOGGER.debug("Testing Failure: Unauthorized user.");
        String invalidToken = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceServer().withRoleConsumer().withCons(new JsonObject()).build();
        Response response = sendExecutionRequest(initiateUploadProcessId, invalidToken, initiateUploadRequestBody());
        response.then().statusCode(401).body(CODE_KEY, is("Not Authorized"));
    }

    @Test
    @Order(2)
    @Description("Failure: Consumer Delegate User")
    public void testExecuteWithConsumerDelegateUserFail() {
        LOGGER.debug("Testing Failure: Consumer Delegate User");
        String invalidToken = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceServer().withDelegate(UUID.fromString("9304cb99-7125-47f1-8686-a070bb6c3eaf"), "consumer")
                .withCons(new JsonObject()).build();
        Response response = sendExecutionRequest(initiateUploadProcessId, invalidToken, initiateUploadRequestBody());
        response.then().statusCode(401).body(CODE_KEY, is("Not Authorized"));
    }

    @Test
    @Order(3)
    @Description("Failure: Provider Delegate with different provider id")
    public void testExecuteWithDifferentDelegateUserIdFail() {
        LOGGER.debug("Testing Failure: Provider Delegate with different DID");

        String invalidToken = new FakeTokenBuilder()
                .withSub(UUID.randomUUID())
                .withResourceServer()
                .withDelegate(UUID.fromString("9304cb99-7125-47f1-8686-a070bb6c3eaf"), "provider")
                .withCons(new JsonObject())
                .build();

        Response response = sendExecutionRequest(initiateUploadProcessId, invalidToken, initiateUploadRequestBody());
        //LOGGER.debug("3rd Execution Response: {}" , response.getBody().asString());
        response.then().statusCode(403).body(DESCRIPTION_KEY, is(RESOURCE_OWNERSHIP_ERROR));

    }

    @Test
    @Order(4)
    @Description("Failure: Process does not Exist")
    public void testExecuteFailProcessNotPresent() {
        LOGGER.debug("Testing Failure: Process does not Exist");
        Response response = sendExecutionRequest(inValidProcessId, getToken(), initiateUploadRequestBody());
        response.then().statusCode(404).body(TYPE_KEY, is(NOT_FOUND));
    }

    @Test
    @Order(5)
    @Description("Failure: Invalid input")
    public void testExecuteFailInvalidInput() {
        LOGGER.debug("Testing Failure: Invalid input");
        JsonObject invalidRequest = new JsonObject()
                .put("inputs", initiateUploadRequestBody().getJsonObject("inputs").remove("bucketName"));
        Response response = sendExecutionRequest(initiateUploadProcessId, getToken(), invalidRequest);
        response.then().statusCode(400).body(CODE_KEY, is("Bad Request"));
    }

    @Test
    @Order(6)
    @Description("Failure: Ownership Error")
    public void testExecuteFailOwnershipError() {
        LOGGER.debug("Failure: Ownership Error");

        String token = new FakeTokenBuilder().withSub(UUID.randomUUID())
                .withResourceServer().withRoleProvider().withCons(new JsonObject()).build();

        Response sendExecutionRequest = sendExecutionRequest(initiateUploadProcessId, token, initiateUploadRequestBody());
        //LOGGER.debug("6th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        sendExecutionRequest.then().statusCode(403).body(DESCRIPTION_KEY,is(RESOURCE_OWNERSHIP_ERROR));
    }

    @Test
    @Order(7)
    @Description("Failure: Resource not onboarded as STAC")
    public void testExecuteResourceNotOnboardedFail() {
        LOGGER.debug("Testing Failure: Resource not onboarded as STAC");

        String token = getToken();
        JsonObject requestBody = initiateUploadRequestBody();
        requestBody.getJsonObject("inputs").put("collectionId", "61f2187e-affe-4f28-be0e-fe1cd37dbd4e");

        Response sendExecutionRequest = sendExecutionRequest(initiateUploadProcessId, token, requestBody);
        //LOGGER.debug("7th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        sendExecutionRequest.then().statusCode(404).body(DESCRIPTION_KEY,is(RESOURCE_NOT_ONBOARDED_MESSAGE));
    }

    @Test
    @Order(8)
    @Description("Failure: Item ID does not exist")
    public void testExecuteItemNotExistsFail() {
        LOGGER.debug("Testing Failure: Item ID does not exist");

        String token = getToken();
        JsonObject requestBody = initiateUploadRequestBody();
        requestBody.getJsonObject("inputs").put("itemId", "C3_PAN_20211129_2485393121");

        Response sendExecutionRequest = sendExecutionRequest(initiateUploadProcessId, token, requestBody);
        //LOGGER.debug("8th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        sendExecutionRequest.then().statusCode(404).body(DESCRIPTION_KEY,is(ITEM_NOT_EXISTS_MESSAGE));
    }

    @Test
    @Order(9)
    @Description("Failure: Object already exists in S3")
    public void testExecuteObjectAlreadyExistsFail() {
        LOGGER.debug("Testing Failure: Object already exists in S3");

        String token = getToken();
        JsonObject requestBody = initiateUploadRequestBody();
        requestBody.getJsonObject("inputs").put("fileName", "ExistingFile.gpkg");

        Response sendExecutionRequest = sendExecutionRequest(initiateUploadProcessId, token, requestBody);
        //LOGGER.debug("9th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        sendExecutionRequest.then().statusCode(409).body(DESCRIPTION_KEY,is(OBJECT_ALREADY_EXISTS_MESSAGE));
    }

    @Test
    @Order(10)
    @Description("Failure: Multipart Upload Initiation")
    public void testExecuteMultipartUploadInitiationFail() {
        LOGGER.debug("Testing Failure: Multipart Upload Initiation");

        String token = getToken();
        JsonObject requestBody = initiateUploadRequestBody();
        requestBody.getJsonObject("inputs").put("bucketName", "invalidBucket");

        Response sendExecutionRequest = sendExecutionRequest(initiateUploadProcessId, token, requestBody);
        //LOGGER.debug("10th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        sendExecutionRequest.then().statusCode(500).body(DESCRIPTION_KEY,is(INITIATE_MULTIPART_UPLOAD_FAILURE_MESSAGE));
    }

    @Test
    @Order(11)
    @Description("Success: Initiate multipart upload and receive presigned URLs")
    public void testExecuteInitiateMultipartUploadSuccess() {
        LOGGER.debug("Success: Initiate multipart upload and receive presigned URLs");

        String token = getToken();
        JsonObject requestBody = initiateUploadRequestBody();

        Response sendExecutionRequest = sendExecutionRequest(initiateUploadProcessId, token, requestBody);
        //LOGGER.debug("11th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        uploadId = sendExecutionRequest.body().path("uploadId");
        chunkSize = sendExecutionRequest.body().path("chunkSize");
        presignedUrls = sendExecutionRequest.body().path("presignedUrls");
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(20, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path(MESSAGE).equals(INITIATE_MULTIPART_UPLOAD_PROCESS_COMPLETE_MESSAGE);
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }

    @Test
    @Order(12)
    @Description("Success: Upload file parts to S3 mock")
    public void testUploadFileParts() {
        File testFile = new File("src/test/resources/processFiles/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/TestFile.gpkg");
        uploadFileParts(testFile);
    }

    @Test
    @Order(13)
    @Description("Success: Complete Multipart Upload")
    public void testExecuteCompleteMultipartUploadSuccess() {
        LOGGER.debug("Success: Complete Multipart Upload");

        String token = getToken();
        JsonObject requestBody = completeUploadRequestBody();

        LOGGER.debug("complete upload request: {}",requestBody);

        Response sendExecutionRequest = sendExecutionRequest(completeUploadProcessId, token, requestBody);
        //LOGGER.debug("13th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(35, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path(MESSAGE).equals(COMPLETE_MULTIPART_UPLOAD_PROCESS_SUCCESS_MESSAGE);
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }

    @Test
    @Order(14)
    @Description("Failure: Multipart Upload Completion failure due to invalid parts format")
    public void testExecuteMultipartUploadCompletionInvalidPartsFail() {
        LOGGER.debug("Testing Failure: Multipart Upload Completion failure due to invalid parts format");

        String token = getToken();
        JsonObject requestBody = completeUploadRequestBody();

        // INVALID PARTS: The first entry is missing the colon `:`
        JsonArray invalidParts = new JsonArray()
                .add("1a13a76866ea78de10c0f12004f2d9bdc")  // Invalid format (missing `:`)
                .add("2:5d41402abc4b2a76b9719d911017c592");

        requestBody.getJsonObject("inputs").put("parts", invalidParts);

        Response sendExecutionRequest = sendExecutionRequest(completeUploadProcessId, token, requestBody);
        //LOGGER.debug("14th Execution Response: {}", sendExecutionRequest.getBody().asString());

        sendExecutionRequest.then().statusCode(400).body(DESCRIPTION_KEY, is(INVALID_PART_FORMAT_MESSAGE));
    }

    @Test
    @Order(15)
    @Description("Failure: Multipart Upload Completion failure due to invalid parts in request input that violates OAS Validation (No schema matches)")
    public void testExecuteMultipartUploadCompletionInvalidPartsInputFail() {
        LOGGER.debug("Testing Failure: Multipart Upload Completion failure due to invalid parts in request input that violates OAS Validation (No schema matches)");

        String token = getToken();
        JsonObject requestBody = completeUploadRequestBody();

        // INVALID FORMAT: Passing parts as JSON objects instead of "partNumber:eTag" strings where no schema is matched in OAS Validation
        JsonArray invalidParts = new JsonArray()
                .add(new JsonObject().put("partNumber", 1).put("eTag", "a13a76866ea78de10c0f12004f2d9bdc"))
                .add(new JsonObject().put("partNumber", 2).put("eTag", "5d41402abc4b2a76b9719d911017c592"))
                .add(new JsonObject().put("partNumber", 3).put("eTag", "e99a18c428cb38d5f260853678922e03"));

        requestBody.getJsonObject("inputs").put("parts", invalidParts);  // This will cause a format failure

        Response sendExecutionRequest = sendExecutionRequest(completeUploadProcessId, token, requestBody);
        //LOGGER.debug("15th Execution Response: {}", sendExecutionRequest.getBody().asString());

        sendExecutionRequest.then().statusCode(400).body(CODE_KEY, is("Bad Request"));
    }

    @Test
    @Order(16)
    @Description("Failure: Multipart Upload Completion due to invalid S3 Key Name")
    public void testExecuteMultipartUploadCompletionInvalidKeyNameFail() {
        LOGGER.debug("Testing Failure: Multipart Upload Completion due to invalid S3 Key Name");

        String token = getToken();
        JsonObject requestBody = completeUploadRequestBody();
        requestBody.getJsonObject("inputs").put("filePath", "04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/InvalidTestFile.gpkg");

        Response sendExecutionRequest = sendExecutionRequest(completeUploadProcessId, token, requestBody);
        //LOGGER.debug("16th Execution Response: {}" , sendExecutionRequest.getBody().asString());

        sendExecutionRequest.then().statusCode(500).body(DESCRIPTION_KEY,is(COMPLETE_MULTIPART_UPLOAD_FAILURE_MESSAGE));
    }

    @Test
    @Order(17)
    @Description("Failure: Multipart Upload Completion due to invalid S3 credentials (invalid bucket)")
    public void testExecuteMultipartUploadCompletionInvalidBucketFail() {
        LOGGER.debug("Testing Failure: Multipart Upload Completion due to invalid S3 credentials (invalid bucket)");

        String token = getToken();
        JsonObject requestBody = completeUploadRequestBody();
        requestBody.getJsonObject("inputs").put("bucketName", "invalidBucket");

        Response sendExecutionRequest = sendExecutionRequest(completeUploadProcessId, token, requestBody);
        //LOGGER.debug("17th Execution Response: {}" , sendExecutionRequest.getBody().asString());

        sendExecutionRequest.then().statusCode(500).body(DESCRIPTION_KEY,is(COMPLETE_MULTIPART_UPLOAD_FAILURE_MESSAGE));
    }

    @Test
    @Order(18)
    @Description("Failure: Multipart Upload Completion due to invalid S3 credentials (invalid upload Id)")
    public void testExecuteMultipartUploadCompletionInvalidUploadIdFail() {
        LOGGER.debug("Testing Failure: Multipart Upload Completion due to invalid S3 credentials (invalid upload Id)");

        String token = getToken();
        JsonObject requestBody = completeUploadRequestBody();
        requestBody.getJsonObject("inputs").put("uploadId", UUID.randomUUID().toString());

        Response sendExecutionRequest = sendExecutionRequest(completeUploadProcessId, token, requestBody);
        //LOGGER.debug("18th Execution Response: {}" , sendExecutionRequest.getBody().asString());

        sendExecutionRequest.then().statusCode(500).body(DESCRIPTION_KEY,is(COMPLETE_MULTIPART_UPLOAD_FAILURE_MESSAGE));
    }
}