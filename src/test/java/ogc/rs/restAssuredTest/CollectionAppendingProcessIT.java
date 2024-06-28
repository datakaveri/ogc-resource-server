package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static ogc.rs.common.Constants.*;
import static ogc.rs.processes.collectionAppending.Constants.*;
import static ogc.rs.processes.util.Status.ACCEPTED;
import static org.hamcrest.Matchers.is;

@ExtendWith(RestAssuredConfigExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CollectionAppendingProcessIT {
    private static final Logger LOGGER = LogManager.getLogger(CollectionAppendingProcessIT.class);
    String executionEndpoint = "/processes/{processId}/execution";
    String jobStatusEndpoint = "/jobs/{jobId}";
    String processId = "b118b4d4-0bc1-4d0b-b137-fdf5b0558c1d";
    public static final int PORT = 9090;
    public static final String BUCKET_PATH_FOR_S3="/bucket1/bucket1/";
    public static final String BUCKET_PATH="/bucket1/";

    @BeforeAll
    public static void setup() throws IOException {
        // Set up files for the tests

        // put the file in /fileName path for Ogr
        File validFile = new File("src/test/resources/processFiles/append_1000_point_features.json");
        given().port(PORT).body(validFile).when().put(BUCKET_PATH + "append_1000_point_features.json")
                .then().statusCode(200);

        // put the file in bucket1/fileName to read file directly from S3
        given().port(PORT).body(validFile).when().put(BUCKET_PATH_FOR_S3 + "append_1000_point_features.json")
                .then().statusCode(200);

        File notEPSGFile = new File("src/test/resources/processFiles/not_EPSG_crs.json");
        given().port(PORT).body(notEPSGFile).when().put(BUCKET_PATH + "not_EPSG_crs.json")
                .then().statusCode(200);

        File invalidSRIDFile = new File("src/test/resources/processFiles/not_registered_EPSG.json");
        given().port(PORT).body(invalidSRIDFile).when().put(BUCKET_PATH + "not_registered_EPSG.json")
                .then().statusCode(200);

        File invalidSchemaFile = new File("src/test/resources/processFiles/invalid_schema_file.json");
        given().port(PORT).body(invalidSchemaFile).when().put(BUCKET_PATH + "invalid_schema_file.json")
                .then().statusCode(200);

        File layersWithDiffGeomFile = new File("src/test/resources/processFiles/2LayersWithDifferentGeom.json");
        given().port(PORT).body(layersWithDiffGeomFile).when().put(BUCKET_PATH + "2LayersWithDifferentGeom.json");

    }

    private JsonObject requestBody() {
        JsonObject requestBody = new JsonObject();

        JsonObject inputs = new JsonObject();
        inputs.put("fileName", "append_1000_point_features.json");
        inputs.put("title", "Valid Append File");
        inputs.put("description", "Valid file for appending test.");
        inputs.put("resourceId", "ce64aa01-cef0-4d44-aaca-0d5cfdd5f20d");
        inputs.put("version", "1.0.0");

        requestBody.put("inputs", inputs);

        requestBody.put("response", "raw");
        return requestBody;
    }

    private String getToken() {
        return new FakeTokenBuilder().withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceServer().withRoleProvider().withCons(new JsonObject()).build();
    }

    private Response sendExecutionRequest(String processId, String token, JsonObject requestBody) {
        return RestAssured.given().pathParam("processId", processId).header("token", token)
                .contentType("application/json").body(requestBody.toString()).when().post(executionEndpoint);
    }

    private Response sendJobStatusRequest(String jobId, String token) {
        return RestAssured.given().pathParam("jobId", jobId).header("token", token)
                .get(jobStatusEndpoint);
    }

    @Test
    @Order(1)
    @Description("Failure: Unauthorized user.")
    public void testExecuteUnauthorizedFail() {
        LOGGER.debug("Testing Failure: Unauthorized user.");
        String invalidToken = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceServer().withRoleConsumer().withCons(new JsonObject()).build();
        Response response = sendExecutionRequest(processId, invalidToken, requestBody());
        response.then().statusCode(401).body(CODE_KEY, is("Not Authorized"));
    }

    @Test
    @Order(2)
    @Description("Success: Process Accepted")
    public void testExecuteProcess() {
        LOGGER.debug("Testing Success: Process Accepted");
        String token = getToken();
        Response response = sendExecutionRequest(processId, token, requestBody());
        response.then().statusCode(201).body("status", is(ACCEPTED.toString()));
    }

    @Test
    @Order(3)
    @Description("Failure: Process does not Exist")
    public void testExecuteFailProcessNotPresent() {
        LOGGER.debug("Testing Failure: Process does not Exist");
        String invalidProcessId = "b118b4d4-0bc1-4d0b-b137-fdf5b0558c1b";
        Response response = sendExecutionRequest(invalidProcessId, getToken(), requestBody());
        response.then().statusCode(404).body(TYPE_KEY, is(NOT_FOUND));
    }

    @Test
    @Order(4)
    @Description("Failure: Invalid input")
    public void testExecuteFailInvalidInput() {
        LOGGER.debug("Testing Failure: Invalid input");
        JsonObject invalidRequest = new JsonObject()
                .put("inputs", requestBody().getJsonObject("inputs").remove("fileName"));
        Response response = sendExecutionRequest(processId, getToken(), invalidRequest);
        response.then().statusCode(400).body(CODE_KEY, is("Bad Request"));
    }

    @Test
    @Order(5)
    @Description("Failure: Ownership Error")
    public void testExecuteFailOwnershipError() throws InterruptedException {
        LOGGER.debug("Failure: Ownership Error");

        String token = new FakeTokenBuilder().withSub(UUID.fromString("0ff3d301-9402-4430-8e18-6f95e4c03c97"))
                .withResourceServer().withRoleProvider().withCons(new JsonObject()).build();
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody());
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(3000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(RESOURCE_OWNERSHIP_ERROR));

    }

    @Test
    @Order(6)
    @Description("Failure: Check if collection is present in collection_details table")
    public void testFailCheckIfCollectionPresent() throws InterruptedException {
        LOGGER.debug("Failure: checkIfCollectionPresent");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("resourceId", "61f2187e-affe-4f28-be0e-fe1cd37dbd4e")
                .put("title", "Non-existing Collection test").put("description", "Invalid collection id for testing");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(3000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(COLLECTION_NOT_FOUND_MESSAGE));

    }

    @Test
    @Order(7)
    @Description("Failure: Invalid organization")
    public void testExecuteFailInvalidOrganization() throws InterruptedException {
        LOGGER.debug("Failure: Invalid organization");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "not_EPSG_crs.json")
                .put("title", "Invalid Organization test").put("description", "File with invalid Organization for testing.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        Thread.sleep(3000);
        String jobId = sendExecutionRequest.body().path("jobId");
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(INVALID_ORGANISATION_MESSAGE));

    }

    @Test
    @Order(8)
    @Description("Failure: Invalid CRS- SRID")
    public void testExecuteFailInvalidCode() throws InterruptedException {
        LOGGER.debug("Failure: Invalid SRID");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "not_registered_EPSG.json")
                .put("title", "Invalid CRS- SRID test").put("description", "File with invalid CRS- SRID for testing.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(3000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(INVALID_SR_ID_MESSAGE));

    }

    @Test
    @Order(9)
    @Description("Failure: checkSchema")
    public void testFailCheckSchema() throws InterruptedException {
        LOGGER.debug("Failure: checkSchema");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "invalid_schema_file.json");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(3000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(SCHEMA_VALIDATION_FAILURE_MESSAGE));

    }

    @Test
    @Order(10)
    @Description("Failure: Invalid file having 2 layers with different geometry")
    public void testExecuteFail2DiffGeo() throws InterruptedException {
        LOGGER.debug("Failure: Invalid file having 2 layers with different geometry");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "2LayersWithDifferentGeom.json")
                .put("title", "Invalid Geometry test").put("description", "File having 2 layers with different geometry.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(6000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(OGR_2_OGR_FAILED_MESSAGE));
    }

    @Test
    @Order(11)
    @Description("Success: Appending data to collection")
    public void testExecuteAppendingSuccess() throws InterruptedException {
        LOGGER.debug("Success: Appending data to collection");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "bucket1/append_1000_point_features.json")
                .put("title", "Valid Append File").put("description", "Valid file for appending test.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(40000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(BBOX_UPDATE_MESSAGE));

    }

}
