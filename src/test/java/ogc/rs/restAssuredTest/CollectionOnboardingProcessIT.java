package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static io.restassured.RestAssured.given;
import static ogc.rs.common.Constants.*;
import static ogc.rs.processes.collectionOnboarding.Constants.*;
import static ogc.rs.processes.util.Status.ACCEPTED;
import static ogc.rs.restAssuredTest.Constant.*;


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
     * Make sure the test server is running at <a href="http://localhost:9090">...</a> before executing tests.
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
        return RestAssured.given().pathParam("processId", processId).auth().oauth2(token)
                .contentType("application/json").body(requestBody.toString()).when().post(executionEndpoint);
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
    @Description("Failure: Item is not present in catalogue")
    public void testExecuteFailItemNotPresent() {
        LOGGER.debug("Failure: Item is not present in catalogue");
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("resourceId", "2dfc08b8-a43d-40d4-ba98-c6fdfa76a0c1");

        String token = getToken();
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(25, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path("message").equals(ITEM_NOT_PRESENT_ERROR);
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }

    @Test
    @Order(6)
    @Description("Failure: Ownership Error")
    public void testExecuteFailOwnershipError() {
        LOGGER.debug("Failure: Ownership Error");

        String token = new FakeTokenBuilder().withSub(UUID.fromString("0ff3d301-9402-4430-8e18-6f95e4c03c97"))
                .withResourceServer().withRoleProvider().withCons(new JsonObject()).build();

        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody());
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(25, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path("message").equals(RESOURCE_OWNERSHIP_ERROR);
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }

    @Test
    @Order(7)
    @Description("Failure: Invalid organization")
    public void testExecuteFailInvalidOrganization() {
        LOGGER.debug("Failure: Invalid organization");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "not_EPSG_crs.gpkg")
                .put("title", "Invalid Organization test").put("description", "File with invalid Organization for testing.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(45, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path("message").equals(CRS_ERROR);
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }

    @Test
    @Order(8)
    @Description("Failure: Invalid EPSG")
    public void testExecuteFailInvalidEpsg() {
        LOGGER.debug("Failure: Invalid EPSG");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "not_registered_EPSG.gpkg")
                .put("title", "Invalid EPSG test").put("description", "File with invalid EPSG for testing.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(45, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path("message").equals(CRS_FETCH_FAILED);
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }

    @Test
    @Order(9)
    @Description("Failure: Fail to get Meta data from S3 by passing an empty file.")
    public void testExecuteS3MetaDataFail() {
        LOGGER.debug("Failure: Fail to get Meta data from S3 by passing an empty file.");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "no_features.gpkg").put("title", "Invalid File test")
                .put("description", "Empty File for S3 failure testing.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(45, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path("message").equals("File not found.");
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }

    @Test
    @Order(10)
    @Description("Failure: Invalid file having 2 layers with different geometry")
    public void testExecuteFail2DiffGeo() {
        LOGGER.debug("Failure: Invalid file having 2 layers with different geometry");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "bucket1/2LayersWithDiffGeom.gpkg")
                .put("title", "Invalid Geometry test").put("description", "File with 2 layers with different geometry.");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(60, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path("message").equals(OGR_2_OGR_FAILED);
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }

    @Test
    @Order(11)
    @Description("Success: Onboarding a collection")
    public void testExecuteOnboardingSuccess() {
        LOGGER.debug("Success: Onboarding a collection");

        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("fileName", "bucket1/hydro_1000_features.gpkg")
                .put("title", "Valid Hydro File").put("description", "Valid hydro file with 1000 features");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(120, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path("message").equals(DB_CHECK_RESPONSE);
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }

    @Test
    @Order(12)
    @Description("Failure: Collection already present")
    public void testExecuteFailItemAlreadyPresent() {
        LOGGER.debug("Failure: Collection already present in collection_details table");

        String token = getToken();
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody());
        String jobId = sendExecutionRequest.body().path("jobId");

        try {
            // Use Awaitility to wait for the job status response
            await().atMost(120, TimeUnit.SECONDS).until(() -> {
                Response getJobStatus = sendJobStatusRequest(jobId, token);
                return getJobStatus.body().path("message").equals(COLLECTION_PRESENT_ERROR);
            });
        } catch (ConditionTimeoutException e) {
            fail("Test failed due to timeout while waiting for job status indicating that the job status is not retrieved within time:" + " " +e.getMessage());
        }
    }
}
