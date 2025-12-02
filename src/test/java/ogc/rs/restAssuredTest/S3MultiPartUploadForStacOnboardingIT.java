package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.processes.ProcessesRunnerImpl;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.common.Constants.*;
import static ogc.rs.processes.collectionOnboarding.Constants.RESOURCE_OWNERSHIP_ERROR;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static ogc.rs.processes.s3MultiPartUploadForStacOnboarding.Constants.*;
import static ogc.rs.processes.util.Constants.NO_S3_CONF_FOUND_FOR_BUCKET_ID;
import static ogc.rs.restAssuredTest.Constant.*;
import static org.hamcrest.Matchers.*;
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
    
    /**
     * Since result of {@link #testExecuteInitiateMultipartUploadSuccess()} is used for the complete
     * upload tests, if that test fails, rest of tests will fail with NPE since uploadId, chunkSize
     * and presignedUrls will be null. Hence this method checks if any of those are null and fails
     * the test if they are.
     */
    private void checkIfInitiateTestPassed() {
      if (uploadId == null || chunkSize == 0 || presignedUrls == null) {
        fail(
            "uploadId, chunkSize or presignedUrls is null, multipart initiate test may have failed");
      }
      return;
    }

    private JsonObject initiateUploadRequestBody() {
        JsonObject requestBody = new JsonObject();
        JsonObject inputs = new JsonObject();

        inputs.put("collectionId", RESOURCE_ID_STAC_TEST);
        inputs.put("itemId", ITEM_ID_STAC_TEST);
        inputs.put(ProcessesRunnerImpl.S3_BUCKET_IDENTIFIER_PROCESS_INPUT_KEY, "default");
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
        inputs.put(ProcessesRunnerImpl.S3_BUCKET_IDENTIFIER_PROCESS_INPUT_KEY, "default");
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
        response.then().statusCode(404).body(CODE_KEY, is(NOT_FOUND));
    }

    @Test
    @Order(5)
    @Description("Failure: Invalid input")
    public void testExecuteFailInvalidInput() {
        LOGGER.debug("Testing Failure: Invalid input");
        JsonObject invalidRequest = new JsonObject()
                .put("inputs", initiateUploadRequestBody().getJsonObject("inputs").remove("collectionId"));
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
    @Description("Failure: Multipart Upload Initiation - bucket config not found")
    public void testExecuteMultipartUploadInitiationFailBucketNotfound() {
        LOGGER.debug("Testing Failure: Multipart Upload Initiation - bucket config not found");

        String token = getToken();
        JsonObject requestBody = initiateUploadRequestBody();
        requestBody.getJsonObject("inputs").put(ProcessesRunnerImpl.S3_BUCKET_IDENTIFIER_PROCESS_INPUT_KEY, "something");

        Response sendExecutionRequest = sendExecutionRequest(initiateUploadProcessId, token, requestBody);
        //LOGGER.debug("10th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        sendExecutionRequest.then().statusCode(403).body(DESCRIPTION_KEY,is(NO_S3_CONF_FOUND_FOR_BUCKET_ID + "something"));
    }

    @Test
    @Order(11)
    @Description("Success: Initiate multipart upload and receive presigned URLs")
    public void testExecuteInitiateMultipartUploadSuccess() {
        LOGGER.debug("Success: Initiate multipart upload and receive presigned URLs");

        String token = getToken();
        JsonObject requestBody = initiateUploadRequestBody();

        Response sendExecutionRequest = sendExecutionRequest(initiateUploadProcessId, token, requestBody)
            .then().statusCode(200)
                .body("uploadId", not(emptyOrNullString()))
                .body("chunkSize", is(greaterThan(0)))
                .body("presignedUrls", not(emptyArray()))
                .body("presignedUrls", everyItem(hasKey("partNumber")))
                .body("presignedUrls", everyItem(hasKey("url")))
                .extract().response();
        
        //LOGGER.debug("11th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        uploadId = sendExecutionRequest.body().path("uploadId");
        chunkSize = sendExecutionRequest.body().path("chunkSize");
        presignedUrls = sendExecutionRequest.body().path("presignedUrls");
    }

    @Test
    @Order(12)
    @Description("Success: Upload file parts to S3 mock")
    public void testUploadFileParts() {
        checkIfInitiateTestPassed();
        
        File testFile = new File("src/test/resources/processFiles/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/TestFile.gpkg");
        uploadFileParts(testFile);
    }

    @Test
    @Order(13)
    @Description("Success: Complete Multipart Upload")
    public void testExecuteCompleteMultipartUploadSuccess() {
        LOGGER.debug("Success: Complete Multipart Upload");
        checkIfInitiateTestPassed();

        String token = getToken();
        JsonObject requestBody = completeUploadRequestBody();

        LOGGER.debug("complete upload request: {}",requestBody);

        sendExecutionRequest(completeUploadProcessId, token, requestBody).then().statusCode(200)
            .body("message", is(COMPLETE_MULTIPART_UPLOAD_PROCESS_SUCCESS_MESSAGE));
    }

    @Test
    @Order(14)
    @Description("Failure: Multipart Upload Completion failure due to invalid parts format")
    public void testExecuteMultipartUploadCompletionInvalidPartsFail() {
        LOGGER.debug("Testing Failure: Multipart Upload Completion failure due to invalid parts format");
        checkIfInitiateTestPassed();

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
        checkIfInitiateTestPassed();

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
        checkIfInitiateTestPassed();

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
        checkIfInitiateTestPassed();

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
        checkIfInitiateTestPassed();

        String token = getToken();
        JsonObject requestBody = completeUploadRequestBody();
        requestBody.getJsonObject("inputs").put("uploadId", UUID.randomUUID().toString());

        Response sendExecutionRequest = sendExecutionRequest(completeUploadProcessId, token, requestBody);
        //LOGGER.debug("18th Execution Response: {}" , sendExecutionRequest.getBody().asString());

        sendExecutionRequest.then().statusCode(500).body(DESCRIPTION_KEY,is(COMPLETE_MULTIPART_UPLOAD_FAILURE_MESSAGE));
    }
}