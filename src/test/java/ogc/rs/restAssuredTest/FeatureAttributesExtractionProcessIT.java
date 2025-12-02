package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.UUID;
import static ogc.rs.common.Constants.*;
import static ogc.rs.processes.featureAttributesExtraction.Constants.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfigExtension.class)
public class FeatureAttributesExtractionProcessIT {
    private static final Logger LOGGER = LogManager.getLogger(FeatureAttributesExtractionProcessIT.class);

    String executionEndpoint = "/processes/{processId}/execution";
    String processId = "e885d45d-5334-4755-aad2-67ed6ac1b989";
    String collectionId = "cfdecaed-54ae-49e2-bf49-43e7d2fe0338"; // World Administrative Country Boundaries CollectionId

    private JsonObject requestBody() {
        JsonObject requestBody = new JsonObject();
        JsonObject inputs = new JsonObject();
        inputs.put("collectionId", collectionId);
        inputs.put("attributes", new String[]{"NAME","CONTINENT"});
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

    @Test
    @Description("Failure: Process does not Exist")
    public void testExecuteFailProcessNotPresent() {
        LOGGER.debug("Testing Failure: Process does not Exist");
        String invalidProcessId = String.valueOf(UUID.randomUUID());
        Response response = sendExecutionRequest(invalidProcessId, getToken(), requestBody());
        response.then().statusCode(404).body(CODE_KEY, is(NOT_FOUND));
    }

    @Test
    @Description("Failure: Test feature attributes extraction process without authentication")
    public void testProcessFailureWithoutAuthentication() {
        LOGGER.debug("Testing failure of feature attributes extraction process without authentication");
        JsonObject requestBody = requestBody();
        Response sendExecutionRequest = RestAssured.given().pathParam("processId", processId)
                .contentType("application/json").body(requestBody.toString()).when().post(executionEndpoint);
        sendExecutionRequest.then().statusCode(401);
    }

    @Test
    @Description("Failure: Test feature attributes extraction process with missing input field")
    public void testProcessFailureWithMissingCollectionId() {
        LOGGER.debug("Testing failure of feature attributes extraction process with missing input field");
        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").remove("collectionId");
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        sendExecutionRequest.then().statusCode(400);
    }

    @Test
    @Description("Failure: Test feature attributes extraction process when invalid collectionId present in the input")
    public void testProcessFailureWhenInvalidCollectionIdPresent() {
        LOGGER.debug("Testing failure of feature attributes extraction process when invalid collectionId present in the input");
        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("collectionId", UUID.randomUUID());
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        sendExecutionRequest.then().statusCode(404).and().body(DESCRIPTION_KEY, is(COLLECTION_NOT_FOUND_MESSAGE));
    }

    @Test
    @Description("Failure: Test feature attributes extraction process when invalid attributes present in the input")
    public void testProcessFailureWhenInvalidAttributesPresent() {
        LOGGER.debug("Testing failure of feature attributes extraction process when invalid attributes present in the input");
        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("attributes", new String[]{"NAME", "INVALID_ATTRIBUTE", "CONTINENT"});
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        sendExecutionRequest.then().statusCode(400).and().body(DESCRIPTION_KEY, is(ATTRIBUTES_NOT_FOUND_MESSAGE));
    }

    @Test
    @Description("Failure: Test feature attributes extraction process when attributes array is empty")
    public void testProcessFailureWhenAttributesArrayIsEmpty() {
        LOGGER.debug("Testing failure of feature attributes extraction process when attributes array is empty");
        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("attributes", new String[]{});
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        sendExecutionRequest.then().statusCode(400).and().body(DESCRIPTION_KEY, is("Attributes array cannot be empty"));
    }

    @Test
    @Description("Success: Test feature attributes extraction process with 'geom' attribute (should be filtered out)")
    public void testProcessSuccessWithGeomAttribute() {
        LOGGER.debug("Testing success of feature attributes extraction process with 'geom' attribute");
        String token = getToken();
        JsonObject requestBody = requestBody();
        requestBody.getJsonObject("inputs").put("attributes", new String[]{"geom", "NAME", "CONTINENT"});
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        sendExecutionRequest.then().statusCode(200)
                .and().body("features", notNullValue())
                .and().body("status", equalTo("successful"))
                .and().body("features[0].properties.geom", nullValue())
                .and().body("features[0].geom", nullValue());
    }

    @Test
    @Description("Success: Test feature attributes extraction process")
    public void testFeatureAttributesExtractionProcessSuccess() {
        LOGGER.debug("Testing success of feature attributes extraction process");
        String token = getToken();
        JsonObject requestBody = requestBody();
        Response sendExecutionRequest = sendExecutionRequest(processId, token, requestBody);
        sendExecutionRequest.then().statusCode(200)
                .and().body("features", notNullValue())
                .and().body("features[0].id", notNullValue())
                .and().body("features[0].properties.NAME", notNullValue())
                .and().body("features[0].properties.CONTINENT", notNullValue())
                .and().body("status", equalTo("successful"));
    }

}
