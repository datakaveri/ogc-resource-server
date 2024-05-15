package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static ogc.rs.common.Constants.CODE_KEY;
import static ogc.rs.common.Constants.NOT_FOUND;
import static ogc.rs.common.Constants.TYPE_KEY;
import static ogc.rs.processes.util.Status.ACCEPTED;

@ExtendWith(RestAssuredConfigExtension.class)
public class CollectionOnboardingProcessIT {
  private static final Logger LOGGER = LogManager.getLogger(CollectionOnboardingProcessIT.class);
  String endpoint = "/processes/{processId}/execution";
  String processId = "cc0eb191-7f66-4663-8afa-cfd644de5839";
  RequestSpecification requestSpecification = RestAssured.given();

  private JsonObject requestBody() {
    JsonObject requestBody = new JsonObject();

    JsonObject inputs = new JsonObject();
    inputs.put("fileName", "Process-test/no_features.gpkg");
    inputs.put("title", "Empty test");
    inputs.put("description", "Empty file for testing.");
    inputs.put("resourceId", "2cfc08b8-a43d-40d4-ba98-c6fdfa76a0c1");
    inputs.put("version", "1.0.0");

    requestBody.put("inputs", inputs);

    requestBody.put("response", "raw");
    return requestBody;
  }

  private String getToken() {
    return new FakeTokenBuilder()
        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
        .withResourceServer()
        .withRoleProvider()
        .withCons(new JsonObject())
        .build();
  }

  private Response sendRequest(String processId, String token, JsonObject requestBody) {
    return requestSpecification
        .pathParam("processId", processId)
        .header("token", token)
        .contentType("application/json")
        .body(requestBody.toString())
        .when()
        .post(endpoint);
  }

  @Test
  @Description("Failure: Unauthorized user.")
  public void testExecuteUnauthorizedFail() {
    String invalidToken =
        new FakeTokenBuilder()
            .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
            .withResourceServer()
            .withRoleConsumer()
            .withCons(new JsonObject())
            .build();
    Response response = sendRequest(processId, invalidToken, requestBody());
    String codeFromResponse = response.body().path(CODE_KEY);
    assertEquals(401, response.statusCode());
    assertEquals("Not Authorized", codeFromResponse);
  }

  @Test
  @Description("Success: Process Accepted")
  public void testExecuteProcess() {
    Response response = sendRequest(processId, getToken(), requestBody());
    String statusFromResponse = response.body().path("status");
    assertEquals(201, response.statusCode());
    assertEquals(ACCEPTED.toString(), statusFromResponse);
  }

  @Test
  @Description("Failure: Process does not Exist")
  public void testExecuteFailProcessNotPresent() {
    String invalidProcessId = "cc0eb191-7f66-4663-8afa-cfd644de5830";
    Response response = sendRequest(invalidProcessId, getToken(), requestBody());
    String typeFromResponse = response.body().path(TYPE_KEY);
    assertEquals(404, response.statusCode());
    assertEquals(NOT_FOUND, typeFromResponse);
  }

  @Test
  @Description("Failure: Invalid input")
  public void testExecuteFailInvalidInput() {
    JsonObject invalidRequest =
        new JsonObject().put("inputs", requestBody().getJsonObject("inputs").remove("fileName"));
    Response response = sendRequest(processId, getToken(), invalidRequest);
    String codeFromResponse = response.body().path(CODE_KEY);
    assertEquals(400, response.statusCode());
    assertEquals("Bad Request", codeFromResponse);
  }
}
