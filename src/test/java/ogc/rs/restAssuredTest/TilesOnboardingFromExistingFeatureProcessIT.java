package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.processes.ProcessesRunnerImpl;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.core.TerminalFailureException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static io.restassured.RestAssured.*;
import static ogc.rs.common.Constants.*;
import static ogc.rs.processes.tilesOnboardingFromExistingFeature.Constants.*;
import static ogc.rs.processes.collectionOnboarding.Constants.*;
import static ogc.rs.processes.util.Status.*;
import static ogc.rs.processes.util.Constants.NO_S3_CONF_FOUND_FOR_BUCKET_ID;
import static ogc.rs.restAssuredTest.Constant.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

@ExtendWith(RestAssuredConfigExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TilesOnboardingFromExistingFeatureProcessIT {
  private static final Logger LOGGER =
      LogManager.getLogger(TilesOnboardingFromExistingFeatureProcessIT.class);
  String executionEndpoint = "/processes/{processId}/execution";
  String jobStatusEndpoint = "/jobs/{jobId}";
  private static final String TILES_TESTING_ENDPOINT =
      "/collections/{collectionId}/map/tiles/WorldCRS84Quad/0/0/0";
  private static final String MVT_CONTENT_TYPE = "application/vnd.mapbox-vector-tile";
  static String processId;

  final static String NYC_CAB_RESOURCE_ID = "6dbcd15f-b497-42cd-8468-e3ecdf2daf7d";
  final static String CODE_POINT_OPEN_RESOURCE_ID = "1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a";
  final static String NON_FEATURE_RESOURCE_ID = "04be4cc1-39f9-4441-b32d-1e5767fa8f10";


  @BeforeAll
  public static void setup() throws IOException {

    processId = given().get("/processes").then().statusCode(200).extract().jsonPath()
        .getString("processes.find{ it.title == 'TilesOnboardingFromExistingFeature' }.id");
  }

  @AfterAll
  public static void tearDown() throws IOException {

    // Delete data from s3Mock bucket after tests, so that test can be run repeatedly without
    // restarting s3Mock container.
    // It's not really required when running in Jenkins pipeline, more required when testing locally
    // We list all objects in the container and delete them.

    List<String> objects = given().port(PORT).when().get("bucket1").then().statusCode(200).extract()
        .xmlPath().getList("ListBucketResult.Contents.Key");

    objects.forEach(obj -> {
      given().port(PORT).when().delete("bucket1/" + obj).then().statusCode(204);
    });
  }

  private JsonObject requestBody() {
    JsonObject requestBody = new JsonObject();

    JsonObject inputs = new JsonObject();
    inputs.put("resourceId", NYC_CAB_RESOURCE_ID);
    inputs.put("maxZoomLevel", 0);
    inputs.put("minZoomLevel", 0);
    inputs.put("pointOfOrigin", new JsonArray().add(-180).add(90));
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

  private Response sendJobStatusRequest(String jobId, String token) {
    return RestAssured.given().pathParam("jobId", jobId).auth().oauth2(token)
        .get(jobStatusEndpoint);
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

    String jobId =
        sendExecutionRequest(processId, invalidToken, requestBody()).body().path("jobId");

    try {
      // Use Awaitility to wait for the job status response
      await().atMost(25, TimeUnit.SECONDS).until(() -> {
        Response getJobStatus = sendJobStatusRequest(jobId, invalidToken);
        return getJobStatus.body().path("message").equals(RESOURCE_OWNERSHIP_ERROR);
      });
    } catch (ConditionTimeoutException e) {
      fail(
          "Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:"
              + " " + e.getMessage());
    }
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

    String jobId = sendExecutionRequest(processId, token, requestBody()).then().statusCode(201)
        .extract().body().path("jobId");

    try {
      // Use Awaitility to wait for the job status response
      await().atMost(25, TimeUnit.SECONDS).failFast(() -> {
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        return getJobStatus.body().path("status").equals(FAILED.toString());
      }).until(() -> {
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        return getJobStatus.body().path("message").equals(PROCESS_SUCCESS_MESSAGE);
      });
    } catch (ConditionTimeoutException e) {
      fail(
          "Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:"
              + " " + e.getMessage());
    } catch (TerminalFailureException x) {
      fail("Unexpected failure in test, fail fast condition was met");
    }

    // test that MVT was actually generated, put into S3 bucket and is accessible via the tiles
    // API
    byte[] op = given().pathParam("collectionId", NYC_CAB_RESOURCE_ID).accept(MVT_CONTENT_TYPE)
        .auth().oauth2(token).when().get(TILES_TESTING_ENDPOINT).then().statusCode(200).and()
        .contentType(MVT_CONTENT_TYPE).extract().asByteArray();

    assertThat(op.toString(), not(emptyOrNullString()));

  }

  @Test
  @Order(5)
  @Description("Failure: Resource already has vector tiles created (testing with previous success case)")
  public void testFailureResourceAlreadyHasVectorTilesCreated() {
    String token =
        new FakeTokenBuilder().withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
            .withRoleProvider().withResourceServer().withCons(new JsonObject()).build();

    String jobId = sendExecutionRequest(processId, token, requestBody()).then().statusCode(201)
        .extract().body().path("jobId");

    try {
      // Use Awaitility to wait for the job status response
      await().atMost(25, TimeUnit.SECONDS).failFast(() -> {
        ResponseBody jobStatus = sendJobStatusRequest(jobId, token).body();
        return jobStatus.path("status").equals(FAILED.toString())
            && !jobStatus.path("message").equals(TILES_ALREADY_ONBOARDED_MESSAGE);
      }).until(() -> {
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        return getJobStatus.body().path("message").equals(TILES_ALREADY_ONBOARDED_MESSAGE);
      });
    } catch (ConditionTimeoutException e) {
      fail(
          "Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:"
              + " " + e.getMessage());
    }
  }

  @Test
  @Order(6)
  @Description("Success: Provider flow with non EPSG:4326 dataset (EPSG:27700)")
  public void testExecuteWithProviderFlow() {
    LOGGER.debug("Testing Success: Provider User");
    String token =
        new FakeTokenBuilder().withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
            .withRoleProvider().withResourceServer().withCons(new JsonObject()).build();

    JsonObject moddedReqBody = requestBody();
    moddedReqBody.getJsonObject("inputs").put("resourceId", CODE_POINT_OPEN_RESOURCE_ID);

    String jobId = sendExecutionRequest(processId, token, moddedReqBody).then().statusCode(201)
        .extract().body().path("jobId");

    try {
      // Use Awaitility to wait for the job status response
      await().atMost(25, TimeUnit.SECONDS).failFast(() -> {
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        return getJobStatus.body().path("status").equals(FAILED.toString());
      }).until(() -> {
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        return getJobStatus.body().path("message").equals(PROCESS_SUCCESS_MESSAGE);
      });
    } catch (ConditionTimeoutException e) {
      fail(
          "Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:"
              + " " + e.getMessage());
    } catch (TerminalFailureException x) {
      fail("Unexpected failure in test, fail fast condition was met");
    }

    // test that MVT was actually generated, put into S3 bucket and is accessible via the tiles
    // API
    byte[] op = given().pathParam("collectionId", CODE_POINT_OPEN_RESOURCE_ID)
        .accept(MVT_CONTENT_TYPE).auth().oauth2(token).when().get(TILES_TESTING_ENDPOINT).then()
        .statusCode(200).and().contentType(MVT_CONTENT_TYPE).extract().asByteArray();

    assertThat(op.toString(), not(emptyOrNullString()));

  }

  @Test
  @Order(7)
  @Description("Failure: Process does not Exist")
  public void testExecuteFailProcessNotPresent() {
    LOGGER.debug("Testing Failure: Process does not Exist");
    String invalidProcessId = "b118b4d4-0bc1-4d0b-b137-fdf5b0558c1b";
    Response response = sendExecutionRequest(invalidProcessId, getToken(), requestBody());
    response.then().statusCode(404).body(CODE_KEY, is(NOT_FOUND));
  }

  @Test
  @Order(8)
  @Description("Failure: Invalid input")
  public void testExecuteFailInvalidInput() {
    LOGGER.debug("Testing Failure: Invalid input");
    JsonObject invalidRequest =
        new JsonObject().put("inputs", requestBody().getJsonObject("inputs").remove("resourceId"));
    Response response = sendExecutionRequest(processId, getToken(), invalidRequest);
    response.then().statusCode(400).body(CODE_KEY, is("Bad Request"));
  }

  @Test
  @Order(9)
  @Description("Failure: Ownership Error")
  public void testExecuteFailOwnershipError() {
    LOGGER.debug("Failure: Ownership Error");

    String token = new FakeTokenBuilder().withSub(UUID.randomUUID()).withResourceServer()
        .withRoleProvider().withCons(new JsonObject()).build();

    String jobId = sendExecutionRequest(processId, token, requestBody()).body().path("jobId");

    try {
      // Use Awaitility to wait for the job status response
      await().atMost(25, TimeUnit.SECONDS).until(() -> {
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        return getJobStatus.body().path("message").equals(RESOURCE_OWNERSHIP_ERROR);
      });
    } catch (ConditionTimeoutException e) {
      fail(
          "Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:"
              + " " + e.getMessage());
    }
  }

  @Test
  @Order(10)
  @Description("Failure: Collection not an OGC Feature")
  public void testFailCollectionNotAFeature() {

    String token = getToken();
    JsonObject requestBody = requestBody();
    requestBody.getJsonObject("inputs").put("resourceId", NON_FEATURE_RESOURCE_ID);

    String jobId = sendExecutionRequest(processId, token, requestBody).body().path("jobId");

    try {
      // Use Awaitility to wait for the job status response
      await().atMost(25, TimeUnit.SECONDS).until(() -> {
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        return getJobStatus.body().path("message").equals(TILES_ALREADY_ONBOARDED_MESSAGE);
      });
    } catch (ConditionTimeoutException e) {
      fail(
          "Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:"
              + " " + e.getMessage());
    }
  }

  @Test
  @Order(11)
  @Description("Failure: No S3 config found for bucket ID")
  public void testFailNoS3ConfigFoundForBucketId() {

    String randBucketId = UUID.randomUUID().toString();
    String token = getToken();

    JsonObject requestBody = requestBody();
    requestBody.getJsonObject("inputs")
        .put(ProcessesRunnerImpl.S3_BUCKET_IDENTIFIER_PROCESS_INPUT_KEY, randBucketId);

    sendExecutionRequest(processId, token, requestBody).then().statusCode(403).and()
        .body("description", equalTo(NO_S3_CONF_FOUND_FOR_BUCKET_ID + randBucketId));
  }

}
