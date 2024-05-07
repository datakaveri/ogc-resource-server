package ogc.rs.authenticator;

import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtAuthServiceTest {
  @BeforeAll
  public static void setup() throws IOException {
    String configFilePath = "secrets/configs/dev.json";
    byte[] jsonData = Files.readAllBytes(Paths.get(configFilePath));
    JsonObject configJson = new JsonObject(new String(jsonData, "UTF-8"));
    String hostName = configJson.getJsonObject("commonConfig").getString("hostName");
    int httpPort = configJson.getJsonObject("commonConfig").getInteger("httpPort");
    RestAssured.baseURI = hostName;
    RestAssured.port = httpPort;
  }

  @Test
  @Description("Fail: Provider token when asset does not exist")
  public void testGetAssetNotExists() {
    UUID resourceId = UUID.fromString("a5a6e26f-d252-446d-b7dd-4d50ea945102");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(resourceId, UUID.randomUUID())
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/4d69a28c-6717-4b80-83c8-308cfa40c932";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(404)
        .body(
            "description",
            equalTo(
                "Asset not found"));
  }

  @Test
  @Description("Success: Provider token with secure resource")
  public void testGetAssetForSecureProviderSuccess() {
    UUID resourceId = UUID.fromString("a5a6e26f-d252-446d-b7dd-4d50ea945102");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(resourceId, UUID.randomUUID())
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/4d69a28c-6717-4b80-83c8-308cfa40c931";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: Secure provider asset not associated with collection id")
  public void testGetAssetForSecureProviderFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(UUID.randomUUID(), UUID.randomUUID())
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/4d69a28c-6717-4b80-83c8-308cfa40c931";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo("Invalid collection id"));
    ;
  }

  @Test
  @Description("Fail: provider identity token for secure asset")
  public void testGetAssetProviderIdentitySecureAsset() {
    UUID resourceId = UUID.fromString("9d2dcaf2-376c-403c-a255-24ac4f5c4559");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResource(resourceId)
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/c4b88cd0-84bf-40a0-bcd9-867a06b070d7";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo("Resource is OPEN. Token is SECURE of role provider"));
  }

  @Test
  @Description("Success: Provider token and open asset")
  public void testProviderOpenTokenSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceServer()
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/c4b88cd0-84bf-40a0-bcd9-867a06b070d7";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: consumer token when asset does not exist")
  public void testConsumerAssetNotExistFail() {
    UUID resourceId = UUID.fromString("a5a6e26f-d252-446d-b7dd-4d50ea945102");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(resourceId, UUID.randomUUID())
            .withRoleConsumer()
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/assets/4d69a28c-6717-4b80-83c8-308cfa40c932";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(404)
        .body(
            "description",
            equalTo(
                "Asset not found")); // Assuming HTTP status 200 for success  // Add assertions
                                     // based on expected response JSON structure
  }

  @Test
  @Description("Success: Consumer token with secure resource")
  public void testGetAssetForSecureConsumerSuccess() {
    UUID resourceId = UUID.fromString("a5a6e26f-d252-446d-b7dd-4d50ea945102");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(resourceId, UUID.randomUUID())
            .withRoleConsumer()
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/assets/4d69a28c-6717-4b80-83c8-308cfa40c931";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: Consumer asset not associated with collection id ")
  public void testGetAssetForSecureConsumerFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(UUID.randomUUID(), UUID.randomUUID())
            .withRoleConsumer()
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/assets/4d69a28c-6717-4b80-83c8-308cfa40c931";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo("Invalid collection id"));
  }

  @Test
  @Description("Fail: Consumer token has no constraints ")
  public void testAssetInvalidConsumerConsFail() {
    UUID resourceId = UUID.fromString("a5a6e26f-d252-446d-b7dd-4d50ea945102");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(resourceId, UUID.randomUUID())
            .withRoleConsumer()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/4d69a28c-6717-4b80-83c8-308cfa40c931";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo("User is not authorised. Please contact IUDX AAA "));
  }

  @Test
  @Description("Success: Consumer token and open asset")
  public void testConsumerOpenTokenSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceServer()
            .withRoleConsumer()
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/assets/c4b88cd0-84bf-40a0-bcd9-867a06b070d7";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: delegate provider token when asset does not exist")
  public void testGetAssetDelegateProviderNotExists() {
    UUID resourceId = UUID.fromString("a5a6e26f-d252-446d-b7dd-4d50ea945102");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(resourceId, UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "provider")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/4d69a28c-6717-4b80-83c8-308cfa40c932";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(404)
        .body(
            "description",
            equalTo(
                "Asset not found")); // Assuming HTTP status 200 for success  // Add assertions
                                     // based on expected response JSON structure
  }

  @Test
  @Description("Fail: consumer identity token for secure asset")
  public void testGetAssetConsumerIdentitySecureAsset() {
    UUID resourceId = UUID.fromString("9d2dcaf2-376c-403c-a255-24ac4f5c4559");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResource(resourceId)
            .withRoleConsumer()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/c4b88cd0-84bf-40a0-bcd9-867a06b070d7";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo("Resource is OPEN. Token is SECURE of role consumer"));
  }
}
