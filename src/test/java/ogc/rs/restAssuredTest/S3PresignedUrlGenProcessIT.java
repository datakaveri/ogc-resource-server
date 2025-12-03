package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.processes.ProcessesRunnerImpl;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.*;
import static ogc.rs.common.Constants.*;
import static ogc.rs.processes.s3PreSignedURLGeneration.Constants.*;
import static ogc.rs.processes.util.Status.*;
import static ogc.rs.processes.util.Constants.NO_S3_CONF_FOUND_FOR_BUCKET_ID;
import static ogc.rs.restAssuredTest.Constant.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfigExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S3PresignedUrlGenProcessIT {
  private static final Logger LOGGER = LogManager.getLogger(S3PresignedUrlGenProcessIT.class);
  String executionEndpoint = "/processes/{processId}/execution";
  String jobStatusEndpoint = "/jobs/{jobId}";
  static String processId;
  
  final static String TESTING_RESOURCE_ID = "03cee7f4-d470-4d1d-b41b-c4dfcbf4ff50";
  final static String TESTING_RESOURCE_ID_RES_GRP = "99a4a805-4954-4947-a819-d9f23c67548f";
  final static String UPLOADED_FILE_OBJECT_KEY = "bucket1/" + TESTING_RESOURCE_ID_RES_GRP + "/" + TESTING_RESOURCE_ID + ".gpkg";
  
  final static String RESOURCE_ID_ALREADY_IN_DB = "a5a6e26f-d252-446d-b7dd-4d50ea945102"; 


  @BeforeAll
  public static void setup() throws IOException {

    processId = given().get("/processes").then().statusCode(200).extract().jsonPath()
        .getString("processes.find{ it.title == 'S3PreSignedURLGeneration' }.id");
  }

  private JsonObject requestBody() {
    JsonObject requestBody = new JsonObject();

    JsonObject inputs = new JsonObject();
    inputs.put("fileType", "geopackage");
    inputs.put("resourceId", TESTING_RESOURCE_ID);
    inputs.put("version", "1.0.0");
    inputs.put(ProcessesRunnerImpl.S3_BUCKET_IDENTIFIER_PROCESS_INPUT_KEY, "default");

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
        .contentType("application/json").body(requestBody.toString()).when()
        .post(executionEndpoint);
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
  @Description("Success: Provider Delegate Flow")
  public void testExecuteWithProviderDelegateUser() {
    LOGGER.debug("Testing Success: Provider Delegate User");
    String token = new FakeTokenBuilder()
        .withSub(UUID.fromString("9304cb99-7125-47f1-8686-a070bb6c3eaf")).withResourceServer()
        .withDelegate(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"), "provider")
        .withCons(new JsonObject()).build();

    String url = sendExecutionRequest(processId, token, requestBody()).then().statusCode(200)
        .body("status", equalTo("successful"))
        .body("message", equalTo(S3_PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE))
        .extract().jsonPath().get("S3PreSignedUrl");
    
    // replace s3.aws-region.amazonaws.com in URL with localhost, since former is not routable outside the docker-compose network
    url = url.replace("s3.aws-region.amazonaws.com", "localhost");
    
    File file = new File("src/test/resources/assets/AssetSample.txt");
    given().baseUri(url).multiPart("file", file).when().put().then().statusCode(200);
    
    given().port(PORT).head(UPLOADED_FILE_OBJECT_KEY).then().statusCode(200);
  }

  @Test
  @Order(5)
  @Description("Success: Provider flow")
  public void testExecuteWithProviderFlow() {
    LOGGER.debug("Testing Success: Provider User");
    String token = new FakeTokenBuilder()
        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97")).withRoleProvider().withResourceServer()
        .withCons(new JsonObject()).build();

    String url = sendExecutionRequest(processId, token, requestBody()).then().statusCode(200)
        .body("status", equalTo("successful"))
        .body("message", equalTo(S3_PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE))
        .extract().jsonPath().get("S3PreSignedUrl");
    
    // replace s3.aws-region.amazonaws.com in URL with localhost, since former is not routable outside the docker-compose network
    url = url.replace("s3.aws-region.amazonaws.com", "localhost");
    
    File file = new File("src/test/resources/assets/AssetSample.txt");
    given().baseUri(url).multiPart("file", file).when().put().then().statusCode(200);
    
    given().port(PORT).head(UPLOADED_FILE_OBJECT_KEY).then().statusCode(200);
  }

  @Test
  @Order(6)
  @Description("Failure: Process does not Exist")
  public void testExecuteFailProcessNotPresent() {
    LOGGER.debug("Testing Failure: Process does not Exist");
    String invalidProcessId = "b118b4d4-0bc1-4d0b-b137-fdf5b0558c1b";
    Response response = sendExecutionRequest(invalidProcessId, getToken(), requestBody());
    response.then().statusCode(404).body(CODE_KEY, is(NOT_FOUND));
  }

  @Test
  @Order(7)
  @Description("Failure: Invalid input")
  public void testExecuteFailInvalidInput() {
    LOGGER.debug("Testing Failure: Invalid input");
    JsonObject invalidRequest =
        new JsonObject().put("inputs", requestBody().getJsonObject("inputs").remove("fileName"));
    Response response = sendExecutionRequest(processId, getToken(), invalidRequest);
    response.then().statusCode(400).body(CODE_KEY, is("Bad Request"));
  }

  @Test
  @Order(8)
  @Description("Failure: Ownership Error")
  public void testExecuteFailOwnershipError() {
    LOGGER.debug("Failure: Ownership Error");

    String token = new FakeTokenBuilder().withSub(UUID.randomUUID()).withResourceServer()
        .withRoleProvider().withCons(new JsonObject()).build();
    
    sendExecutionRequest(processId, token, requestBody()).then().statusCode(403).and()
        .body("description", equalTo(RESOURCE_OWNERSHIP_ERROR));
  }

  @Test
  @Order(9)
  @Description("Failure: Collection already onboarded as feature")
  public void testFailCollectionAlreadyOnboardedAsFeature() {

    String token = getToken();
    JsonObject requestBody = requestBody();
    requestBody.getJsonObject("inputs").put("resourceId", RESOURCE_ID_ALREADY_IN_DB);
    
    sendExecutionRequest(processId, token, requestBody).then().statusCode(409).and()
        .body("description", equalTo(RESOURCE_ALREADY_EXISTS_MESSAGE));
  }
  
  @Test
  @Order(10)
  @Description("Failure: No S3 config found for bucket ID")
  public void testFailNoS3ConfigFoundForBucketId() {

    String randBucketId =  UUID.randomUUID().toString();
    String token = getToken();
    
    JsonObject requestBody = requestBody();
    requestBody.getJsonObject("inputs").put(ProcessesRunnerImpl.S3_BUCKET_IDENTIFIER_PROCESS_INPUT_KEY, randBucketId);
    
    sendExecutionRequest(processId, token, requestBody).then().statusCode(403).and()
        .body("description", equalTo(NO_S3_CONF_FOUND_FOR_BUCKET_ID + randBucketId));
  }

}
