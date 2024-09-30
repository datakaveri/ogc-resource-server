package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static ogc.rs.apiserver.util.Constants.USER_NOT_AUTHORIZED;
import static ogc.rs.restAssuredTest.Constant.*;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfigExtension.class)
public class CoveragesIT {

  @BeforeAll
  public static void setup() throws IOException {
    File file = new File("src/test/resources/coverage/coverageSample.txt");
    given().port(PORT).multiPart("file", file).when().put(COVERAGE_PATH).then().statusCode(200);
  }

  private static final UUID OPEN_RESOURCE = UUID.fromString("a5a6e26f-d252-446d-b7dd-4d50ea945102");
  private static final UUID SECURE_RESOURCE =
      UUID.fromString("1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a");

  @Test
  @Description("Success: provider secure resource")
  public void testGetCoverageForSecureProviderSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(SECURE_RESOURCE, UUID.randomUUID())
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: provider RI different from collection id")
  public void testGetCoverageInvalidProviderRiFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(OPEN_RESOURCE, UUID.randomUUID())
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(USER_NOT_AUTHORIZED));
  }

  @Test
  @Description("Success: Provider token and open collection id")
  public void testProviderOpenTokenSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceServer()
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/a5a6e26f-d252-446d-b7dd-4d50ea945102/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Success: delegate provider secure resource")
  public void testGetCoverageForSecureDelegateProviderSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(SECURE_RESOURCE, UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "provider")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: provider delegate RI different from collection id")
  public void testGetCoverageInvalidProviderDelegateRiFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(OPEN_RESOURCE, UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "provider")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(USER_NOT_AUTHORIZED));
  }

  @Test
  @Description("Success: Provider Delegate token and open collection id")
  public void testProviderDelegateOpenTokenSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceServer()
            .withDelegate(UUID.randomUUID(), "provider")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/a5a6e26f-d252-446d-b7dd-4d50ea945102/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: provider RI different from collection id")
  public void testGetCoverageInvalidProviderRiFaill() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResource(OPEN_RESOURCE)
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(USER_NOT_AUTHORIZED));
  }

  @Test
  @Description("Success: consumer secure resource")
  public void testGetCoverageForSecureConsumerSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(SECURE_RESOURCE, UUID.randomUUID())
            .withRoleProvider()
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: consumer RI different from collection id")
  public void testGetCoverageInvalidConsumerRiFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(OPEN_RESOURCE, UUID.randomUUID())
            .withRoleProvider()
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(USER_NOT_AUTHORIZED));
  }

  @Test
  @Description("Fail: consumer token has no constraints")
  public void testGetCoverageInvalidConsumerConsFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(SECURE_RESOURCE, UUID.randomUUID())
            .withRoleConsumer()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(USER_NOT_AUTHORIZED));
  }

  @Test
  @Description("Success: Consumer token and open collection id")
  public void testConsumerOpenTokenSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceServer()
            .withRoleConsumer()
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/collections/a5a6e26f-d252-446d-b7dd-4d50ea945102/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Success: Delegate Consumer secure resource")
  public void testGetCoverageForSecureDelegateConsumerSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(SECURE_RESOURCE, UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "consumer")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: Consumer Delegate RI different from collection id")
  public void testGetCoverageInvalidConsumerDelegateRiFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(OPEN_RESOURCE, UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "consumer")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(USER_NOT_AUTHORIZED));
  }

  @Test
  @Description("Success: Consumer Delegate token and open collection id")
  public void testConsumerDelegateOpenTokenSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceServer()
            .withDelegate(UUID.randomUUID(), "consumer")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/collections/a5a6e26f-d252-446d-b7dd-4d50ea945102/coverage";
    given()
        .header("Accept", "application/json")
        .auth()
        .oauth2(token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }
}
