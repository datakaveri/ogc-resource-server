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
import static ogc.rs.processes.collectionOnboarding.Constants.*;
import static ogc.rs.processes.util.Status.ACCEPTED;
import static ogc.rs.restAssuredTest.Constant.*;
import static org.hamcrest.Matchers.is;

@ExtendWith(RestAssuredConfigExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CollectionOnboardingProcessIT {
    private static final Logger LOGGER = LogManager.getLogger(CollectionOnboardingProcessIT.class);
    String executionEndpoint = "/processes/{processId}/execution";
    String jobStatusEndpoint = "/jobs/{jobId}";
    String processId = "cc0eb191-7f66-4663-8afa-cfd644de5839";


    /**
     * Sets up a mock S3 environment by uploading a sample file to a local server.
     * This method is executed once before all test methods in the test class.
     * The uploaded file is used for simulating interactions with an S3-like service.
     * Asset_Path is the href link in database to access an asset
     * Make sure the test server is running at http://localhost:9090 before executing tests.
     */
    @BeforeAll
    public static void setup() throws IOException {
        // put the file in /fileName path for Ogr
        File validFile = new File("src/test/resources/processFiles/hydro_1000_features.gpkg");
        given().port(PORT).body(validFile).when().put(BUCKET_PATH + "hydro_1000_features.gpkg")
                .then().statusCode(200);
        // put the file in bucket1/fileName to read file directly from S3
        given().port(PORT).body(validFile).when().put(BUCKET_PATH_FOR_S3 + "hydro_1000_features.gpkg")
                .then().statusCode(200);

        File layersWith2GeomFile = new File("src/test/resources/processFiles/2LayersWithDiffGeom.gpkg");
        given().port(PORT).body(layersWith2GeomFile).when().put(BUCKET_PATH + "2LayersWithDiffGeom.gpkg")
                .then().statusCode(200);

        given().port(PORT).body(layersWith2GeomFile).when().put(BUCKET_PATH_FOR_S3 + "2LayersWithDiffGeom.gpkg")
                .then().statusCode(200);

        File notEPSG = new File("src/test/resources/processFiles/not_EPSG_crs.gpkg");
        given().port(PORT).body(notEPSG).when().put(BUCKET_PATH + "not_EPSG_crs.gpkg")
                .then().statusCode(200);

        File invalidEPSGFile = new File("src/test/resources/processFiles/not_registered_EPSG.gpkg");
        given().port(PORT).body(invalidEPSGFile).when().put(BUCKET_PATH + "not_registered_EPSG.gpkg")
                .then().statusCode(200);

        File emptyFile = new File("src/test/resources/processFiles/no_features.gpkg");
        given().port(PORT).body(emptyFile).when().put(BUCKET_PATH + "no_features.gpkg")
                .then().statusCode(200);
    }

    private JsonObject requestBody() {
        JsonObject requestBody = new JsonObject();

        JsonObject inputs = new JsonObject();
        inputs.put("fileName", "not_registered_EPSG.gpkg");
        inputs.put("title", "Invalid File");
        inputs.put("description", "Invalid file for testing.");
        inputs.put("resourceId", "2cfc08b8-a43d-40d4-ba98-c6fdfa76a0c1");
        inputs.put("dateTimeKey", "Type");
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
        String invalidProcessId = "cc0eb191-7f66-4663-8afa-cfd644de5830";
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
    @Description("Failure: Item not present in cat")
    public void testExecuteFailItemNotPresent() throws InterruptedException {
        LOGGER.debug("Failure: Item not present in cat");
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("resourceId", "2dfc08b8-a43d-40d4-ba98-c6fdfa76a0c1");

        String token = getToken();
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(4000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);

        getJobStatus.then().statusCode(200).body("message", is(ITEM_NOT_PRESENT_ERROR));
    }

    @Test
    @Order(6)
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
    @Order(7)
    @Description("Failure: Invalid organization")
    public void testExecuteFailInvalidOrganization() throws InterruptedException {
        LOGGER.debug("Failure: Invalid organization");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "not_EPSG_crs.gpkg")
                .put("title", "Invalid Organization test").put("description", "File with invalid Organization for testing.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        Thread.sleep(3000);
        String jobId = sendExecutionRequest.body().path("jobId");
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(CRS_ERROR));
    }

    @Test
    @Order(8)
    @Description("Failure: Invalid EPSG")
    public void testExecuteFailInvalidEpsg() throws InterruptedException {
        LOGGER.debug("Failure: Invalid EPSG");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "not_registered_EPSG.gpkg")
                .put("title", "Invalid EPSG test").put("description", "File with invalid EPSG for testing.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(3000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(CRS_FETCH_FAILED));
    }

    @Test
    @Order(9)
    @Description("Failure: Invalid DateTimeKey")
    public void testExecuteInvalidDateTimeKey() throws InterruptedException {
        LOGGER.debug("Success: Onboarding a collection");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "bucket1/hydro_1000_features.gpkg")
                .put("title", "Valid Hydro File").put("description", "Valid hydro file with 1000 features")
                .put("dateTimeKey", "hydro");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(40000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(DB_CHECK_RESPONSE));
    }

    @Test
    @Order(10)
    @Description("Failure: Fail to get Meta data from S3 by passing an empty file.")
    public void testExecuteS3MetaDataFail() throws InterruptedException {
        LOGGER.debug("Failure: Fail to get Meta data from S3 by passing an empty file.");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "no_features.gpkg").put("title", "Invalid File test")
                .put("description", "Empty File for S3 failure testing.").put("dateTimeKey", "country_code");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(3000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is("File not found."));
    }

    @Test
    @Order(11)
    @Description("Failure: Invalid file having 2 layers with different geometry")
    public void testExecuteFail2DiffGeo() throws InterruptedException {
        LOGGER.debug("Failure: Invalid file having 2 layers with different geometry");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "bucket1/2LayersWithDiffGeom.gpkg")
                .put("title", "Invalid Geometry test").put("description", "File with 2 layers with different geometry.")
                .put("dateTimeKey", "flow_direction");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(6000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(OGR_2_OGR_FAILED));
    }

    @Test
    @Order(12)
    @Description("Success: Onboarding a collection")
    public void testExecuteOnboardingSuccess() throws InterruptedException {
        LOGGER.debug("Success: Onboarding a collection");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "bucket1/hydro_1000_features.gpkg")
                .put("title", "Valid Hydro File").put("description", "Valid hydro file with 1000 features")
                .put("dateTimeKey", "hydro_node_category");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(40000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(DB_CHECK_RESPONSE));
    }

    @Test
    @Order(13)
    @Description("Failure: Collection already present")
    public void testExecuteFailItemAlreadyPresent() throws InterruptedException {
        LOGGER.debug("Failure: Item already present in cat");

        String token = getToken();
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody());
        String jobId = sendExecutionRequest.body().path("jobId");
        Thread.sleep(6000);
        Response getJobStatus = sendJobStatusRequest(jobId, token);
        getJobStatus.then().statusCode(200).body("message", is(COLLECTION_PRESENT_ERROR));
    }
}
