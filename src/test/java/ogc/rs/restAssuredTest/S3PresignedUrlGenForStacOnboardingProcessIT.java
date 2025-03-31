package ogc.rs.restAssuredTest;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static ogc.rs.processes.s3MultiPartUploadForStacOnboarding.Constants.OBJECT_ALREADY_EXISTS_MESSAGE;
import static ogc.rs.processes.collectionOnboarding.Constants.RESOURCE_OWNERSHIP_ERROR;
import static ogc.rs.processes.s3PreSignedUrlGenerationForStaconboarding.Constants.*;
import static ogc.rs.common.Constants.*;
import static ogc.rs.restAssuredTest.Constant.*;
import static ogc.rs.processes.util.Status.*;
import static org.hamcrest.Matchers.*;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import static io.restassured.RestAssured.*;


@ExtendWith(RestAssuredConfigExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S3PresignedUrlGenForStacOnboardingProcessIT {
    private static final Logger LOGGER = LogManager.getLogger(S3PresignedUrlGenForStacOnboardingProcessIT.class);

    String executionEndpoint = "/processes/{processId}/execution";
    final static String processId = "05cd7e21-da7c-45a5-bd76-21c1366ea3d8";
    final static String invalidProcessId = "b118b4d4-0bc1-4d0b-b137-fdf5b0558c1b";
    final static String RESOURCE_ID_STAC_TEST = "04be4cc1-39f9-4441-b32d-1e5767fa8f10";
    final static String ITEM_ID_STAC_TEST = "C3_PAN_20211108_2485193221";

    private JsonObject requestBody() {
        JsonObject requestBody = new JsonObject();

        JsonObject inputs = new JsonObject();
        inputs.put("collectionId", RESOURCE_ID_STAC_TEST);
        inputs.put("itemId", ITEM_ID_STAC_TEST);
        inputs.put("fileName", "TestFile");
        inputs.put("fileExtension", "png");
        inputs.put("region", "aws-region");
        inputs.put("bucketName", "bucket1");

        requestBody.put("inputs", inputs);

        requestBody.put("response", "raw");
        return requestBody;
    }

    private String getToken() {
        return new FakeTokenBuilder().withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceServer().withRoleProvider().withCons(new JsonObject()).build();
    }

    private Response sendExecutionRequest(String processId, String token, JsonObject requestBody) {
        return given().pathParam("processId", processId).auth().oauth2(token)
                .contentType("application/json").body(requestBody.toString()).when().post(executionEndpoint);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        given().port(PORT).when().delete( "bucket1/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/TestFile.png")
                .then().statusCode(204);

        given().port(PORT).when().delete( "bucket1/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/TestFile1.png")
                .then().statusCode(204);
    }

    @Test
    @Order(1)
    @Description("Failure: Unauthorized user.")
    public void testExecuteUnauthorizedFail() {
        LOGGER.debug("Testing Failure: Unauthorized user.");
        String invalidToken =
                new FakeTokenBuilder().withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer().withRoleConsumer().withCons(new JsonObject()).build();
        Response response = sendExecutionRequest(processId, invalidToken, requestBody());
        response.then().statusCode(401).body(CODE_KEY, is("Not Authorized"));
    }

    @Test
    @Order(2)
    @Description("Failure: Consumer Delegate User")
    public void testExecuteWithConsumerDelegateUserFail() {
        LOGGER.debug("Testing Failure: Consumer Delegate User");
        String invalidToken = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97")).withResourceServer()
                .withDelegate(UUID.fromString("9304cb99-7125-47f1-8686-a070bb6c3eaf"), "consumer")
                .withCons(new JsonObject()).build();
        Response response = sendExecutionRequest(processId, invalidToken, requestBody());
        response.then().statusCode(401).body(CODE_KEY, is("Not Authorized"));
    }

    @Test
    @Order(3)
    @Description("Failure: Provider Delegate with different provider id")
    public void testExecuteWithDifferentDelegateUserIdFail() {
        LOGGER.debug("Testing Failure: Provider Delegate with different DID");
        String invalidToken = new FakeTokenBuilder().withSub(UUID.randomUUID()).withResourceServer()
                .withDelegate(UUID.fromString("9304cb99-7125-47f1-8686-a070bb6c3eaf"), "provider")
                .withCons(new JsonObject()).build();

        sendExecutionRequest(processId, invalidToken, requestBody()).then().statusCode(403).and()
                .body("description", equalTo(RESOURCE_OWNERSHIP_ERROR));
    }
    @Test
    @Order(4)
    @Description("Failure: Process does not Exist")
    public void testExecuteFailProcessNotPresent() {
        LOGGER.debug("Testing Failure: Process does not Exist");
        Response response = sendExecutionRequest(invalidProcessId, getToken(), requestBody());
        response.then().statusCode(404).body(TYPE_KEY, is(NOT_FOUND));
    }

    @Test
    @Order(5)
    @Description("Failure: Invalid input")
    public void testExecuteFailInvalidInput() {
        LOGGER.debug("Testing Failure: Invalid input");
        JsonObject invalidRequest =
                new JsonObject().put("inputs", requestBody().getJsonObject("inputs").remove("fileName"));
        Response response = sendExecutionRequest(processId, getToken(), invalidRequest);
        response.then().statusCode(400).body(CODE_KEY, is("Bad Request"));
    }

    @Test
    @Order(6)
    @Description("Failure: Ownership Error")
    public void testExecuteFailOwnershipError() {
        LOGGER.debug("Failure: Ownership Error");

        String token = new FakeTokenBuilder().withSub(UUID.randomUUID())
                .withResourceServer().withRoleProvider().withCons(new JsonObject()).build();

        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody());
        LOGGER.debug("6th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        sendExecutionRequest.then().statusCode(403).body(DESCRIPTION_KEY,is(RESOURCE_OWNERSHIP_ERROR));
    }
    @Test
    @Order(7)
    @Description("Failure: Resource not onboarded as STAC")
    public void testExecuteResourceNotOnboardedFail() {
        LOGGER.debug("Testing Failure: Resource not onboarded as STAC");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("collectionId", "61f2187e-affe-4f28-be0e-fe1cd37dbd4e");

        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        LOGGER.debug("7th Execution Response: {}" , sendExecutionRequest.getBody().asString());
        sendExecutionRequest.then().statusCode(404).body(DESCRIPTION_KEY,is(RESOURCE_NOT_ONBOARDED_MESSAGE));
    }

    @Test
    @Order(8)
    @Description("Failure: Item ID does not exist")
    public void testExecuteItemNotExistsFail() {
        LOGGER.debug("Testing Failure: Item ID does not exist");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("itemId", "C3_PAN_20211129_2485393121");

        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        sendExecutionRequest.then().statusCode(404).body(DESCRIPTION_KEY,is(ITEM_NOT_EXISTS_MESSAGE));
    }
    @Test
    @Order(9)
    @Description("Success: Provider Delegate Flow")
    public void testExecuteWithProviderDelegateUser() {
        LOGGER.debug("Testing Success: Provider Delegate User");
        String token = new FakeTokenBuilder()
                .withSub(UUID.fromString("9304cb99-7125-47f1-8686-a070bb6c3eaf")).withResourceServer()
                .withDelegate(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"), "provider")
                .withCons(new JsonObject()).build();

        String url = sendExecutionRequest(processId, token, requestBody()).then().statusCode(200)
                .body("status", equalTo(SUCCESSFUL.toString()))
                .body("message", equalTo(S3_PRESIGNED_URL_PROCESS_SUCCESS_MESSAGE))
                .extract().jsonPath().get("S3PreSignedUrl");

        url = url.replace("s3.aws-region.amazonaws.com", "localhost");

        File file = new File("src/test/resources/assets/AssetSample.txt");
        given().baseUri(url).multiPart("file", file).when().put().then().statusCode(200);

        given().port(PORT).head("bucket1/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/TestFile.png").then().statusCode(200);
    }
    @Test
    @Order(10)
    @Description("Failure: Object Already Exists in S3")
    public void testObjectAlreadyExistsFail(){
        LOGGER.debug("Testing Failure: Object Already Exists in S3");

        String token = getToken();
        sendExecutionRequest(processId, token, requestBody()).then().statusCode(409).and()
                .body(DESCRIPTION_KEY, is(OBJECT_ALREADY_EXISTS_MESSAGE));

    }
    @Test
    @Order(11)
    @Description("Success: Presigned Url generation for stac onboarding")
    public void testPresignedUrlGenerationForStacOnboardingFlow() {
        LOGGER.debug("Testing Success: Provider User");
        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "TestFile1");

        String url = sendExecutionRequest(processId, token, requestBody).then().statusCode(200)
                .body("status", equalTo(SUCCESSFUL.toString()))
                .body("message", equalTo(S3_PRESIGNED_URL_PROCESS_SUCCESS_MESSAGE))
                .extract().jsonPath().get("S3PreSignedUrl");

        url = url.replace("s3.aws-region.amazonaws.com", "localhost");

        File file = new File("src/test/resources/assets/AssetSample.txt");
        given().baseUri(url).multiPart("file", file).when().put().then().statusCode(200);

        given().port(PORT).head("bucket1/04be4cc1-39f9-4441-b32d-1e5767fa8f10/C3_PAN_20211108_2485193221/TestFile1.png").then().statusCode(200);
    }
}
