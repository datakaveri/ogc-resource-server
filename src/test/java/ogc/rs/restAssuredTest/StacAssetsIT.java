package ogc.rs.restAssuredTest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static ogc.rs.restAssuredTest.Constant.ASSET_PATH;
import static ogc.rs.restAssuredTest.Constant.PORT;
import static ogc.rs.apiserver.util.Constants.*;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfigExtension.class)
public class StacAssetsIT {

  /**
   * Sets up a mock S3 environment by uploading a sample file to a local server.
   * This method is executed once before all test methods in the test class.
   * The uploaded file is used for simulating interactions with an S3-like service.
   * Asset_Path is the href link in database to access an asset
   * Make sure the test server is running at http://localhost:9090 before executing tests.
   */
  @BeforeAll
  public static void setup() throws IOException {
    File file = new File("src/test/resources/assets/AssetSample.txt");
    given().port(PORT).multiPart("file", file).when().put(ASSET_PATH).then().statusCode(200);
  }

  // Define constant UUIDs for test
  private static final UUID OPEN_RESOURCE = UUID.fromString("a5a6e26f-d252-446d-b7dd-4d50ea945102");
  private static final UUID SECURE_RESOURCE =
      UUID.fromString("1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a");

  @Test
  @Description("Fail: Provider token when asset does not exist")
  public void testGetAssetNotExists() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(OPEN_RESOURCE, UUID.randomUUID())
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
        .body("description", equalTo(ASSET_NOT_FOUND));
  }

  @Test
  @Description("Success: Provider token with secure resource")
  public void testGetAssetForSecureProviderSuccess() {
    UUID resourceId = UUID.fromString("1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(SECURE_RESOURCE, UUID.randomUUID())
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
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
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(INVALID_COLLECTION_ID));
  }

  @Test
  @Description("Fail: provider identity token for secure asset")
  public void testGetAssetProviderIdentitySecureAsset() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResource(OPEN_RESOURCE)
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
        .body("description", equalTo(RESOURCE_OPEN_TOKEN_SECURE + "provider"));
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
  @Description("Fail: consumer token when asset does not exist")
  public void testConsumerAssetNotExistFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(OPEN_RESOURCE, UUID.randomUUID())
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
        .body("description", equalTo("Asset not found"));
  }

  @Test
  @Description("Success: Consumer token with secure resource")
  public void testGetAssetForSecureConsumerSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(SECURE_RESOURCE, UUID.randomUUID())
            .withRoleConsumer()
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
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
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(INVALID_COLLECTION_ID));
  }

  @Test
  @Description("Fail: Consumer token has no constraints ")
  public void testAssetInvalidConsumerConsFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(SECURE_RESOURCE, UUID.randomUUID())
            .withRoleConsumer()
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(USER_NOT_AUTHORIZED));
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
  @Description("Fail: consumer identity token for secure asset")
  public void testGetAssetConsumerIdentitySecureAsset() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResource(OPEN_RESOURCE)
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
        .body("description", equalTo(RESOURCE_OPEN_TOKEN_SECURE + "consumer"));
  }

  @Test
  @Description("Fail: delegate provider token when asset does not exist")
  public void testGetAssetDelegateProviderNotExists() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(OPEN_RESOURCE, UUID.randomUUID())
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
        .body("description", equalTo(ASSET_NOT_FOUND));
  }

  @Disabled
  @Test
  @Description("Success: Delegate Provider token with secure resource")
  public void testGetAssetForSecureDelegateProviderSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(OPEN_RESOURCE, UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "provider")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: Secure Delegate provider asset not associated with collection id")
  public void testGetAssetForSecureDelegateProviderFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(UUID.randomUUID(), UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "provider")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(INVALID_COLLECTION_ID));
  }

  @Disabled
  @Test
  @Description("Fail: delegate provider identity token for secure asset")
  public void testGetAssetD4elegateProviderIdentitySecureAsset() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResource(OPEN_RESOURCE)
            .withDelegate(UUID.randomUUID(), "provider")
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
        .body("description", equalTo(RESOURCE_OPEN_TOKEN_SECURE));
  }

  @Test
  @Description("Success: Delegate Provider token and open asset")
  public void testDelegateProviderOpenTokenSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceServer()
            .withDelegate(UUID.randomUUID(), "provider")
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
  @Description("Fail: Delegate Consumer token when asset does not exist")
  public void testDelegateConsumerAssetNotExistFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(OPEN_RESOURCE, UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "consumer")
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
        .body("description", equalTo(ASSET_NOT_FOUND));
  }

  @Disabled
  @Test
  @Description("Success: Delegate Consumer token with secure resource")
  public void testGetAssetForSecureDelegateConsumerSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(SECURE_RESOURCE, UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "consumer")
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(200);
  }

  @Test
  @Description("Fail: Delegate Consumer asset not associated with collection id ")
  public void testGetAssetForSecureDelegateConsumerFail() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(UUID.randomUUID(), UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "consumer")
            .withCons(new JsonObject().put("access", new JsonArray().add("api")))
            .build();
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(INVALID_COLLECTION_ID));
  }

  @Disabled
  @Test
  @Description("Fail: Delegate Consumer token has no constraints ")
  public void testAssetInvalidDelegateConsumerConsFail() {
    UUID resourceId = UUID.fromString("1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a");
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceAndRg(resourceId, UUID.randomUUID())
            .withDelegate(UUID.randomUUID(), "consumer")
            .withCons(new JsonObject())
            .build();
    String endpoint = "/assets/462624da-2790-4ae7-8773-c1a6d726a035";
    given()
        .header("Accept", "application/json")
        .header("token", token)
        .when()
        .get(endpoint)
        .then()
        .statusCode(401)
        .body("description", equalTo(USER_NOT_AUTHORIZED));
  }

  @Test
  @Description("Success: Delegate Consumer token and open asset")
  public void testDelegateConsumerOpenTokenSuccess() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResourceServer()
            .withDelegate(UUID.randomUUID(), "consumer")
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

  @Disabled
  @Test
  @Description("Fail: Delegate Consumer identity token for secure asset")
  public void testGetAssetDelegateConsumerIdentitySecureAsset() {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.randomUUID())
            .withResource(OPEN_RESOURCE)
            .withDelegate(UUID.randomUUID(), "consumer")
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
        .body("description", equalTo(RESOURCE_OPEN_TOKEN_SECURE));
  }
}
