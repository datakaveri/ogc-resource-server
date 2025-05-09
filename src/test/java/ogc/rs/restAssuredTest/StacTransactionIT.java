package ogc.rs.restAssuredTest;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfigExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class StacTransactionIT {

  String hostname;
  String openCollectionCreateItemEndpoint = "/stac/collections/0bfd0f9a-a31c-4ee9-9728-0a97248a46375/items";
  String secureCollectionCreateItemEndpoint = "/stac/collections/6f95f983-a826-42e0-8e97-e224a546fe32/items";
  String secureCollectionUpdateItemEndpoint = "/stac/collections/6f95f983-a826-42e0-8e97-e224a546fe32/items" +
      "/testing_stac_item_1";
  JsonObject standardRequestBody =  new JsonObject();
  JsonObject standardRequestBodyFeatures = new JsonObject();
  
  static String token;

  @BeforeAll
  public static void createFakeProviderToken() {
    token =
        new FakeTokenBuilder()
            .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
            .withResourceServer()
            .withRoleProvider()
            .withCons(new JsonObject())
            .build();
  }

  @BeforeEach
  public void initialiseRequestBodyAndToken() {

    JsonObject geometry = new JsonObject().put("type", "Point").put("coordinates", new JsonArray().add(-110).add(39.5));
    JsonArray bbox = new JsonArray().add(-110).add(39.5).add(-105).add(40.5);

    JsonObject validProperties = new JsonObject().put("datetime", "2018-02-12T00:00:00Z").put("anotherProperty", "some" +
        "-value");
    standardRequestBody
        .put("id", "testing_stac_item_1")
        .put("bbox", bbox)
        .put("geometry", geometry)
        .put("properties", validProperties)
        .put("type", "Feature");

    standardRequestBodyFeatures = new JsonObject()
        .put("type", "FeatureCollection")
        .put("features", new JsonArray()
            .add(new JsonObject()
                .put("id", "testing_stac_item_3")
                .put("bbox", bbox)
                .put("geometry", geometry)
                .put("properties", validProperties)
                .put("type", "Feature"))
            .add(new JsonObject()
                .put("id", "testing_stac_item_4")
                .put("bbox", new JsonArray().add(-110).add(39.5).add(-105).add(40.5))
                .put("geometry", geometry)
                .put("properties", validProperties)
                .put("type", "Feature")));
  }

    @Order(1)
    @Test
    @Description("Success: Stac Item creation using open provider token")
    public void testCreateStacItemProviderTokenSuccess() throws InterruptedException {

        given()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .auth().oauth2(token)
            .body(standardRequestBody.encode())
            .when()
            .post(secureCollectionCreateItemEndpoint)
            .then()
            .statusCode(201)
//            .header("LOCATION",)
            .body("id", equalTo("testing_stac_item_1"))
            .body("properties", equalTo(standardRequestBody.getJsonObject("properties")))
            .body("geometry", equalTo(standardRequestBody.getJsonObject("geometry")))
            .body("bbox", contains(-180, -56, 180, 83))
            .body("type", equalTo("Feature"));

        Thread.sleep(1000);
    }

    @Order(2)
    @Test
    @Description("Success: Stac Item update using open provider token")
    public void testUpdateStacItemProviderTokenSuccess() throws InterruptedException {
        UUID randomAssetId = UUID.randomUUID();
        JsonObject assets = new JsonObject()
            .put(String.valueOf(randomAssetId), new JsonObject()
                .put("href","http://cool-sat.com/catalog/collections/cs/items/CS3-20160503_132130_04/thumb.png")
                .put("title", "Asset title")
                .put("type", "image/png")
                .put("roles", new JsonArray().add("thumbnail"))
                .put("size", 1024));

        standardRequestBody.put("assets", assets);

        given()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .auth().oauth2(token)
            .body(standardRequestBody.encode())
            .when()
            .patch(secureCollectionUpdateItemEndpoint)
            .then()
            .statusCode(200)
            .body("id", equalTo("testing_stac_item_1"))
            .body("bbox", equalTo(standardRequestBody.getJsonArray("bbox")))
            .body("assets."+randomAssetId+".href"
                , equalTo("http://cool-sat.com/catalog/collections/cs/items/CS3-20160503_132130_04/thumb.png"))
            .body("assets."+randomAssetId+".href", equalTo(1024));

        Thread.sleep(1000);
    }

    @Order(3)
    @Test
    @Description("Success: Stac Item creation using open provider-delegate token")
    public void testCreateStacItemProviderDelegateTokenSuccess() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withDelegate(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"), "provider")
                        .withCons(new JsonObject())
                        .build();

        standardRequestBody.put("id", "testing_feature_item_2");

        given()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json") 
            .auth().oauth2(token)
            .body(standardRequestBody.encode())
            .when()
            .post(openCollectionCreateItemEndpoint)
            .then()
            .statusCode(201)
            .body("id", equalTo("testing_stac_item_1"))
            .body("properties", equalTo(standardRequestBody.getJsonObject("properties")))
            .body("geometry", equalTo(standardRequestBody.getJsonObject("geometry")))
            .body("bbox", contains(-180, -56, 180, 83))
            .body("type", equalTo("Feature"));

        Thread.sleep(1000);
    }


    @Order(4)
    @Test
    @Description("Success: Stac Item update using open provider-delegate token")
    public void testUpdateStacItemDelegateTokenSuccess() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withDelegate(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"), "provider")
                        .withCons(new JsonObject())
                        .build();

        standardRequestBody.put("properties", new JsonObject().put("some-property-key", "some-property-value"));

        given()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .auth().oauth2(token)
            .body(standardRequestBody.encode())
            .when()
            .patch(secureCollectionUpdateItemEndpoint)
            .then()
            .statusCode(200)
            .log().all()
            .body("id", equalTo("testing_stac_item_1"))
            .body("properties", equalTo(new JsonObject().put("some-property-key", "some-property-value")));
        Thread.sleep(1000);
    }

    @Order(5)
    @Test
    @Description("Failure: Request Body doesn't have Item Id")
    public void testCreateStacItemIdNotPresent() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        standardRequestBody.remove("id");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .auth().oauth2(token)
                .body(standardRequestBody.encode())
                .when()
                .post(openCollectionCreateItemEndpoint)
                .then()
                .statusCode(400)
                .body("code", equalTo("Bad Request"))
                .body("description", containsString("Validation error for body application/json"));
//                .body("description", containsString("should contain property id"));
    }

    @Order(6)
    @Test
    @Description("Failure: Token not provider or provider-delegate")
    public void testCreateStacItemInvalidToken() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer()
                        .withRoleConsumer()
                        .withCons(new JsonObject())
                        .build();

        standardRequestBody.put("id", "testing_stac_item_2");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") 
                .auth().oauth2(token)
                .body(standardRequestBody.encode())
                .when()
                .post(openCollectionCreateItemEndpoint)
                .then()
                .statusCode(401)
                .body("code", equalTo("Not Authorized"))
                .body("description", containsString("Role Not Provider or delegate"));
    }

  @Order(7)
  @Test
  @Description("Failure: Token is from consumer-delegate")
  public void testStacCollectionIdInvalidToken() throws InterruptedException {
    String token =
        new FakeTokenBuilder()
            .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
            .withResourceServer()
            .withDelegate(UUID.randomUUID(), "consumer")
            .withCons(new JsonObject())
            .build();

    standardRequestBody.put("id", "testing_stac_item_2");
    given()
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .auth().oauth2(token)
        .body(standardRequestBody.encode())
        .when()
        .post(openCollectionCreateItemEndpoint)
        .then()
        .statusCode(401)
        .body("code", equalTo("Not Authorized"))
        .body("description", containsString("Role Not Provider or delegate"));
  }

    @Order(8)
    @Test
    @Description("Failure: Collection Owner is different from the provider presenting the token")
    public void testCreateStacItemDifferentOwnerTokenFailure() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();

        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .auth().oauth2(token)
                .body(standardRequestBody.encode())
                .when()
                .post(openCollectionCreateItemEndpoint)
                .then()
                .statusCode(401)
                .body("code", equalTo("Not Authorized"))
                .body("description", containsString("Item belongs to different provider"));


    }

    @Order(9)
    @Test
    @Description("Failure: Collection Owner is different from the provider-delegate presenting the token")
    public void testCreateStacItemDifferentOwnerDelegateTokenFailure() {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withDelegate(UUID.randomUUID(), "provider")
                        .withCons(new JsonObject())
                        .build();

        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .auth().oauth2(token)
                .body(standardRequestBody.encode())
                .when()
                .post(openCollectionCreateItemEndpoint)
                .then()
                .statusCode(401)
                .body("code", equalTo("Not Authorized"))
                .body("description", containsString("Item belongs to different provider"));
    }

    @Order(10)
    @Test
    @Description("Failure: Non-identity token from Provider")
    public void testCreateStacCollectionProviderNotOpenTokenFailure() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceAndRg(UUID.fromString("6f95f983-a826-42e0-8e97-e224a546fe32"), UUID.randomUUID())
                        .withRoleProvider()
                        .withCons(new JsonObject().put("access", new JsonArray().add("api")))
                        .build();

        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .auth().oauth2(token)
                .body(standardRequestBody.encode())
                .when()
                .post(secureCollectionCreateItemEndpoint)
                .then()
                .statusCode(401)
                .body("code", equalTo("Not Authorized"))
                .body("description", containsString("open token should be used"));
    }

    @Order(11)
    @Test
    @Description("Failure: datetime not is ISO 8601")
    public void testCreateStacItemInvalidDatetimeFailure() {
    
        JsonObject request = standardRequestBody;
        request.put("properties", new JsonObject().put("datetime","invalid-datetime-format"));

        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .auth().oauth2(token)
                .body(request.encode())
                .when()
                .post(openCollectionCreateItemEndpoint)
                .then()
                .statusCode(400)
                .body("code", equalTo("Bad Request"))
                .body("description", containsString("[Bad Request] Validation error for body"));
    }

    @Order(12)
    @Test
    @Description("Failure: Stac Collection does not exist")
    public void testCreateStacItemCollectionDoesNotExistFailure() {

        String endpoint = "/stac/collections/"+UUID.randomUUID()+"/items";

        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .auth().oauth2(token)
                .body(standardRequestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(404)
                .body("code", equalTo("Collection Not Found"))
                .body("description", containsString("Collection does not exist"));
    }


  @Order(13)
  @Test
  @Description("Success: Stac Items creation using open provider token")
  public void testCreateStacItemsProviderTokenSuccess() throws InterruptedException {

    given()
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .auth().oauth2(token)
        .body(standardRequestBodyFeatures.encode())
        .when()
        .post(secureCollectionCreateItemEndpoint)
        .then()
        .statusCode(201)
//            .header("LOCATION",)
        .body("code", contains("Items are created"))
        .body("stac_version", equalTo("1.0.0"));

    Thread.sleep(1000);


  }

    @Order(14)
    @Test
    @Description("Failure: Duplicate Item Ids in the request")
    public void testCreateStacItemsDuplicateIdsFailure() throws InterruptedException {

      standardRequestBodyFeatures.getJsonArray("features")
          .add(new JsonObject()
              .put("id", "testing_stac_item_3")
              .put("bbox", new JsonArray().add(-110).add(39.5).add(-105).add(40.5))
              .put("geometry", new JsonObject()
                  .put("type", "Point")
                  .put("coordinates", new JsonArray().add(-110).add(39.5)))
              .put("properties", new JsonObject().put("datetime", "2018-02-12T00:00:00Z").put("anotherProperty", "some" +
                  "-value"))
              .put("type", "Feature"));

      given()
          .header("Accept", "application/json")
          .header("Content-Type", "application/json")
          .auth().oauth2(token)
          .body(standardRequestBodyFeatures.encode())
          .when()
          .post(secureCollectionCreateItemEndpoint)
          .then()
          .statusCode(409)
//            .header("LOCATION",)
          .body("code", equalTo("Conflict"))
          .body("description", contains("duplicate ids are present"));

      Thread.sleep(1000);
    }

  @Order(15)
  @Test
  @Description("Failure: Stac Item already exist for a collection")
  public void testCreateStacItemAlreadyExistFailure() throws InterruptedException {

    given()
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .auth().oauth2(token)
        .body(standardRequestBody.encode())
        .when()
        .post(secureCollectionCreateItemEndpoint)
        .then()
        .statusCode(409)
//            .header("LOCATION",)
        .body("code", equalTo("Conflict"))
        .body("description", contains("item already exists"));

    Thread.sleep(1000);
  }

  @Order(16)
  @Test
  @Description("Failure: Item type is invalid")
  public void testCreateStacItemInvalidItemTypeFailure() throws InterruptedException {

    standardRequestBody.put("type","invalid-type");

    given()
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .auth().oauth2(token)
        .body(standardRequestBody.encode())
        .when()
        .post(secureCollectionCreateItemEndpoint)
        .then()
        .statusCode(400)
//            .header("LOCATION",)
        .body("code", equalTo("Bad Request"))
        .body("description", contains("Validation error for body application/json"));

    Thread.sleep(1000);
  }

    // update item, id don't match

  @Order(17)
  @Test
  @Description("Failure: Updating a STAC Item, id don't match")
  public void testUpdateStacItemIdDontMatchFailure() throws InterruptedException {

    standardRequestBody.put("id","some-other-id");

    given()
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .auth().oauth2(token)
        .body(standardRequestBody.encode())
        .when()
        .post(secureCollectionCreateItemEndpoint)
        .then()
        .statusCode(400)
//            .header("LOCATION",)
        .body("code", equalTo("Bad Request"))
        .body("description", contains("id don't match with uri"));

    Thread.sleep(1000);
  }

    // invalid geometry

  @Order(17)
  @Test
  @Description("Failure: Creating a STAC Item with invalid geometry")
  public void testCreateStacItemInvalidGeometryFailure() throws InterruptedException {
    
    standardRequestBody.put("geometry","invalid-geometry");

    given()
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .auth().oauth2(token)
        .body(standardRequestBody.encode())
        .when()
        .post(secureCollectionCreateItemEndpoint)
        .then()
        .statusCode(400)
//            .header("LOCATION",)
        .body("code", equalTo("Bad Request"))
        .body("description", contains("Validation error for body application/json"));

    Thread.sleep(1000);
  }

    @Order(18)
    @Test
    @Description("Failure: Creating a STAC Item with Asset Href missing")
    public void testCreateStacItemAssetHrefIsMissingFailure() throws InterruptedException {
      JsonObject assets = new JsonObject()
            .put(String.valueOf(UUID.randomUUID()), new JsonObject()
                .put("title", "Asset title")
                .put("type", "image/png")
                .put("roles", new JsonArray().add("thumbnail"))
                .put("size", 1024));
        
      standardRequestBody.put("assets",assets);
  
        given()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .auth().oauth2(token)
            .body(standardRequestBody.encode())
            .when()
            .post(secureCollectionCreateItemEndpoint)
            .then()
            .statusCode(400)
  //            .header("LOCATION",)
            .body("code", equalTo("Bad Request"))
            .body("description", contains("Validation error for body application/json"));
  
        Thread.sleep(1000);
    }

    @Order(19)
    @Test
    @Description("Success: Creating a STAC Item with same Id but for different STAC Collection")
    public void testCreateItemWithSameId() throws InterruptedException {

      given()
          .header("Accept", "application/json")
          .header("Content-Type", "application/json")
          .auth().oauth2(token)
          .body(standardRequestBody.encode())
          .when()
          .post(openCollectionCreateItemEndpoint)
          .then()
          .statusCode(400)
          //            .header("LOCATION",)
          .body("code", equalTo("Bad Request"))
          .body("description", contains("Validation error for body application/json"));

      Thread.sleep(1000);
    }
}
