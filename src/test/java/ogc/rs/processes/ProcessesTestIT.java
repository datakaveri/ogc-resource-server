package ogc.rs.processes;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jdk.jfr.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static junit.framework.Assert.assertEquals;

/** Integration tests for the Processes endpoint. */
@ExtendWith(RestAssuredConfigExtension.class)
public class ProcessesTestIT {
  String endpoint = "/processes";
  RequestSpecification requestSpecification = RestAssured.given();

  /** Test to verify successful retrieval of processes. */
  @Test
  @Description("Success: get processes")
  public void testGetProcessesSuccess() {
    Response response = requestSpecification.get(endpoint);
    System.out.println("sattys " + response.statusCode());
    assertEquals(200, response.statusCode());
  }

  /**
   * Test to verify failure when trying to retrieve a process with an invalid endpoint. Endpoint is
   * intentionally made invalid to simulate a failure scenario.
   */
  @Test
  @Description("Failure: retrieve processes with an invalid endpoint")
  public void testGetProcessesFail() {
    endpoint = endpoint.concat("makingEndpointInvalid");
    Response response = requestSpecification.get(endpoint);
    String typeFromResponse = response.body().path("description");
    assertEquals(404, response.statusCode());
    assertEquals("API / Collection not found", typeFromResponse);
  }

  /** Test to verify successful retrieval of a specific process. */
  @Test
  @Description("Success: get a process")
  public void testGetProcessSuccess() {
    String EXISTING_ID = "cc0eb191-7f66-4663-8afa-cfd644de5839";
    endpoint = endpoint.concat("/").concat(EXISTING_ID);
    Response response = requestSpecification.get(endpoint);
    String idFromResponse = response.then().extract().body().path("processes[0].id");

    assertEquals(200, response.statusCode());
    assertEquals("cc0eb191-7f66-4663-8afa-cfd644de5839", idFromResponse);
  }

  /** Test to verify failure when trying to retrieve a non-existing process. */
  @Test
  @Description("Failure: get a process")
  public void testGetProcessFail() {
    String NON_EXISTING_ID = "cc0eb191-7f66-4663-8afa-cfd644de5830";
    endpoint = endpoint.concat("/").concat(NON_EXISTING_ID);
    Response response = requestSpecification.get(endpoint);
    String typeFromResponse = response.body().path("type");
    assertEquals(404, response.statusCode());
    assertEquals("Not Found", typeFromResponse);
  }
}
