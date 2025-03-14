package ogc.rs.restAssuredTest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfigExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class StacCollectionOnboardingIT {

    @Order(1)
    @Test
    @Description("Success: Stac Collection creation using open provider token")
    public void testCreateStacCollectionProviderTokenSuccess() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections";
        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-06-23T00:00:00Z").add("2019-07-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("id", "ac14db94-4e9a-4336-9bec-072d37c0360e")
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT Test Suite")
                        .put("description", "IT Test Suite")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(201);
        Thread.sleep(2000);


        given()
                .header("Accept", "application/json")
                .auth().oauth2(token)
                .when()
                .get(endpoint+"/ac14db94-4e9a-4336-9bec-072d37c0360e")
                .then()
                .statusCode(200)
                .body("title", equalTo("IT Test Suite"))
                .body("description", equalTo("IT Test Suite"))
                .body("license", equalTo("proprietary"))
                .body("extent.spatial.bbox[0]", contains(-180, -56, 180, 83))
                .body("extent.temporal.interval[0]", hasItems("2015-06-23T00:00:00Z", "2019-07-10T13:44:56Z"));
        Thread.sleep(2000);


    }

    @Order(2)
    @Test
    @Description("Success: Stac Collection updation using open provider token")
    public void testUpdateStacCollectionProviderTokenSuccess() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections/ac14db94-4e9a-4336-9bec-072d37c0360e";

        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-181).add(-50).add(181).add(82))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-07-23T00:00:00Z").add("2019-08-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("id", "ac14db94-4e9a-4336-9bec-072d37c0360e")
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT test Suite Updated")
                        .put("description", "IT test Suite Updated")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .put(endpoint)
                .then()
                .statusCode(200);

        given()
                .header("Accept", "application/json")
                .auth().oauth2(token)
                .when()
                .get(endpoint)
                .then()
                .log().all()
                .statusCode(200)
                .body("title", equalTo("IT test Suite Updated"))
                .body("description", equalTo("IT test Suite Updated"))
                .body("license", equalTo("proprietary"))
                .body("extent.spatial.bbox[0]", contains(-181, -50, 181, 82))
                .body("extent.temporal.interval[0]", hasItems("2015-07-23T00:00:00Z", "2019-08-10T13:44:56Z"));
        Thread.sleep(2000);


    }

    @Order(3)
    @Test
    @Description("Success: Stac Collection creation using open delegate token")
    public void testCreateStacCollectionDelegateTokenSuccess() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withDelegate(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"), "provider")
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections";
        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-06-23T00:00:00Z").add("2019-07-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("id", "0473a68a-c66a-42fb-93e3-ae9fd4c6e7dd")
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT Test Suite")
                        .put("description", "IT Test Suite")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(201);
        Thread.sleep(2000);


        given()
                .header("Accept", "application/json")
                .auth().oauth2(token)
                .when()
                .get(endpoint+"/0473a68a-c66a-42fb-93e3-ae9fd4c6e7dd")
                .then()
                .log().all()
                .statusCode(200)
                .body("title", equalTo("IT Test Suite"))
                .body("description", equalTo("IT Test Suite"))
                .body("license", equalTo("proprietary"))
                .body("extent.spatial.bbox[0]", contains(-180, -56, 180, 83))
                .body("extent.temporal.interval[0]", hasItems("2015-06-23T00:00:00Z", "2019-07-10T13:44:56Z"));
        Thread.sleep(2000);
    }


    @Order(4)
    @Test
    @Description("Success: Stac Collection updation using open provider token")
    public void testUpdateStacCollectionDelegateTokenSuccess() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections/0473a68a-c66a-42fb-93e3-ae9fd4c6e7dd";

        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-07-23T00:00:00Z").add("2019-08-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("id", "0473a68a-c66a-42fb-93e3-ae9fd4c6e7dd")
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT test Suite Updated")
                        .put("description", "IT test Suite Updated")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .put(endpoint)
                .then()
                .statusCode(200);

        given()
                .header("Accept", "application/json")
                .auth().oauth2(token)
                .when()
                .get(endpoint)
                .then()
                .log().all()
                .statusCode(200)
                .body("title", equalTo("IT test Suite Updated"))
                .body("description", equalTo("IT test Suite Updated"))
                .body("license", equalTo("proprietary"))
                .body("extent.spatial.bbox[0]", contains(-180, -56, 180, 83))
                .body("extent.temporal.interval[0]", hasItems("2015-07-23T00:00:00Z", "2019-08-10T13:44:56Z"));
        Thread.sleep(2000);


    }

    @Order(5)
    @Test
    @Description("Failure: Request Body doesn't have Id")
    public void testStacCollectionIdNotPresent() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections";
        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-06-23T00:00:00Z").add("2019-07-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT Test Suite")
                        .put("description", "IT Test Suite")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(400)
                .body("code", equalTo("Bad Request"))
                .body("description", containsString("Validation error for body application/json"))
                .body("description", containsString("[Bad Request] Validation error for body application/json: No schema matches"));
    }

    @Order(6)
    @Test
    @Description("Failure: Request Body has invalid Id")
    public void testStacCollectionIdInvalidPresent() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections";
        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-06-23T00:00:00Z").add("2019-07-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("id", 123)
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT Test Suite")
                        .put("description", "IT Test Suite")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(400)
                .body("code", equalTo("Bad Request"))
                .body("description", containsString("Validation error for body application/json"));
    }

    @Order(7)
    @Test
    @Description("Failure: Token not provider or delegate")
    public void testStacCollectionIdInvalidToken() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer()
                        .withRoleConsumer()
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections";
        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-06-23T00:00:00Z").add("2019-07-10T13:44:56Z"))
                        )
                );
        System.out.println(token);
        JsonObject requestBody =
                new JsonObject()
                        .put("id", "33df0304-85b8-4566-b30c-872fbba2061f")
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT Test Suite")
                        .put("description", "IT Test Suite")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(401)
                .body("code", equalTo("Not Authorized"))
                .body("description", containsString("Role Not Provider or delegate"));
    }

    @Order(8)
    @Test
    @Description("Failure: Owner User Id is different for item using provider token")
    public void testCreateStacCollectionProviderTokenFailure() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("33df0304-85b8-4566-b30c-872fbba2061f"))
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections";
        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-06-23T00:00:00Z").add("2019-07-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("id", "ac14db94-4e9a-4336-9bec-072d37c0360e")
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT Test Suite")
                        .put("description", "IT Test Suite")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(401)
                .body("code", equalTo("Not Authorized"))
                .body("description", containsString("Item belongs to different provider"));


    }

    @Order(9)
    @Test
    @Description("Failure: Owner User Id is different for item using delegate token")
    public void testCreateStacCollectionDelagateTokenFailure() {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withDelegate(UUID.fromString("33df0304-85b8-4566-b30c-872fbba2061f"), "provider")
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections";
        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-06-23T00:00:00Z").add("2019-07-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("id", "ac14db94-4e9a-4336-9bec-072d37c0360e")
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT Test Suite")
                        .put("description", "IT Test Suite")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(401)
                .body("code", equalTo("Not Authorized"))
                .body("description", containsString("Item belongs to different provider"));
    }

    @Order(10)
    @Test
    @Description("Failure: Token is not OPEN and is provider token")
    public void testCreateStacCollectionProviderNotOpenTokenFailure() throws InterruptedException {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceAndRg(UUID.fromString("1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a"), UUID.randomUUID())
                        .withRoleProvider()
                        .withCons(new JsonObject().put("access", new JsonArray().add("api")))
                        .build();
        String endpoint = "/stac/collections";
        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-06-23T00:00:00Z").add("2019-07-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("id", "ac14db94-4e9a-4336-9bec-072d37c0360e")
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", "proprietary")
                        .put("title", "IT Test Suite")
                        .put("description", "IT Test Suite")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(401)
                .body("code", equalTo("Not Authorized"))
                .body("description", containsString("open token should be used"));
    }

    @Order(11)
    @Test
    @Description("Failure: Stac Collection creation using invalid request body")
    public void testCreateStacInvalidReqBodyFailure() {
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        String endpoint = "/stac/collections";
        JsonObject extent = new JsonObject()
                .put("spatial", new JsonObject()
                        .put("bbox", new JsonArray()
                                .add(new JsonArray().add(-180).add(-56).add(180).add(83))
                        )
                )
                .put("temporal", new JsonObject()
                        .put("interval", new JsonArray()
                                .add(new JsonArray().add("2015-06-23T00:00:00Z").add("2019-07-10T13:44:56Z"))
                        )
                );
        JsonObject requestBody =
                new JsonObject()
                        .put("id", "ac14db94-4e9a-4336-9bec-072d37c0360e")
                        .put("crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84")
                        .put("license", 123)
                        .put("title", "IT Test Suite")
                        .put("description", "IT Test Suite")
                        .put("extent", extent)
                        .put("datetimeKey", "2023-11-10T14:30:00Z");
        given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json") // Add this
                .auth().oauth2(token)
                .body(requestBody.encode())
                .when()
                .post(endpoint)
                .then()
                .statusCode(400)
                .body("code", equalTo("Bad Request"))
                .body("description", containsString("[Bad Request] Validation error for body"));
    }


}
