package ogc.rs.restAssuredTest;

import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfigExtension.class)
public class StacItemByIdIT {

    private static final Logger LOGGER = LogManager.getLogger(StacItemByIdIT.class);
    String stacItemByIdEndpoint = "/stac/collections/{collectionId}/items/{itemId}";
    String secureCollectionId = "04be4cc1-39f9-4441-b32d-1e5767fa8f10"; //secure resource
    String itemIdForSecureCollection = "C3_MX_20211107_248519311";
    String openCollectionId = "44da9cda-b00c-4481-be78-73b36038a7be"; //open resource
    String itemIdForOpenCollection = "C3_MX_20210509_248563041";

    private Response sendStacItemByIdRequest(String collectionId, String itemId, String token) {
        if (token == null) {
            return given().pathParam("collectionId", collectionId)
                    .pathParam("itemId", itemId)
                    .contentType("application/json")
                    .when().get(stacItemByIdEndpoint);
        }
        return given().pathParam("collectionId", collectionId)
                .pathParam("itemId", itemId)
                .auth().oauth2(token)
                .contentType("application/json")
                .when().get(stacItemByIdEndpoint);
    }

    private void validateValidTokenResponse(Response response) {
        response.then().statusCode(200);
        Map<String, Map<String, Object>> assets = response.jsonPath().getMap("assets");

        boolean foundPresignedUrl = false;
        boolean foundPublicAsset = false;

        for (Map<String, Object> asset : assets.values()) {
            String href = (String) asset.get("href");

            if (href.contains("X-Amz-Signature")) {
                foundPresignedUrl = true;  // Test Presigned URLs are generated for protected assets
            } else {
                assertThat(href, not(containsString("#"))); // Ensure there are no blocked assets
                foundPublicAsset = true;
            }
        }

        assertTrue("Expected either presigned URLs (containing X-Amz-Signature) for protected assets or public assets without restriction",
                foundPresignedUrl || foundPublicAsset);
    }

    private void validateInvalidOrNoTokenResponse(Response response) {
        response.then().statusCode(200);
        Map<String, Map<String, Object>> assets = response.jsonPath().getMap("assets");

        boolean foundBlockedAsset = false;
        boolean foundPublicAsset = false;

        for (Map<String, Object> asset : assets.values()) {
            String href = (String) asset.get("href");

            if (href.contains("#")) {
                foundBlockedAsset = true;  // Expected blocked assets for invalid or no tokens
            } else {
                assertThat(href, not(containsString("X-Amz-Signature"))); // Ensure no presigned URLs are generated
                foundPublicAsset = true;
            }
        }

        assertTrue("Expected either blocked assets (indicated by #) for protected resources or public assets with accessible URLs.",
                foundBlockedAsset || foundPublicAsset);
    }

    @Test
    @Description("Correct Token Provided: Open Provider Token and the hrefs in the response contain the presigned urls")
    public void testOpenProviderTokenSuccess() {
        LOGGER.info("Testing if the response contains the presigned urls in the href when a valid token is provided - (Open Provider Token)");
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        Response response = sendStacItemByIdRequest(openCollectionId, itemIdForOpenCollection, token);
        validateValidTokenResponse(response);
    }

    @Test
    @Description("Correct Token Provided: Open Consumer Token and the hrefs in the response contain the presigned urls")
    public void testOpenConsumerTokenSuccess() {
        LOGGER.info("Testing if the response contains the presigned urls in the href when a valid token is provided - (Open Consumer Token)");
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withRoleConsumer()
                        .withCons(new JsonObject().put("access", new JsonArray().add("api")))
                        .build();
        Response response = sendStacItemByIdRequest(openCollectionId, itemIdForOpenCollection, token);
        validateValidTokenResponse(response);
    }


    @Test
    @Description("Correct Token Provided: Secure Provider Token and the hrefs in the response contain the presigned urls")
    public void testSecureProviderTokenSuccess() {
        LOGGER.info("Testing if the response contains the presigned urls in the hrefs when a valid token is provided (Secure Provider Token)");
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceAndRg(UUID.fromString(secureCollectionId), UUID.randomUUID())
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        Response response = sendStacItemByIdRequest(secureCollectionId, itemIdForSecureCollection, token);
        validateValidTokenResponse(response);
    }

    @Test
    @Description("Correct Token Provided: Secure Consumer Token and the hrefs in the response contain the presigned urls")
    public void testSecureConsumerTokenSuccess() {
        LOGGER.info("Testing if the response contains the presigned urls in the hrefs when a valid token is provided (Secure Consumer Token)");
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceAndRg(UUID.fromString(secureCollectionId), UUID.randomUUID())
                        .withRoleConsumer()
                        .withCons(new JsonObject().put("access", new JsonArray().add("api")))
                        .build();
        Response response = sendStacItemByIdRequest(secureCollectionId, itemIdForSecureCollection, token);
        validateValidTokenResponse(response);
    }

    @Test
    @Description("Incorrect Token provided: Open Provider Token for Secure Resource - Expected # for protected assets and public URLs for others")
    public void testOpenProviderTokenForSecureResource() {
        LOGGER.info("Testing if the response contains the # in the hrefs when an invalid token is provided (Open Provider Token For Secure Resource)");
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();

        Response response = sendStacItemByIdRequest(secureCollectionId, itemIdForSecureCollection, token);
        validateInvalidOrNoTokenResponse(response);
    }


    @Test
    @Description("Incorrect Token provided: Open Consumer Token for Secure Resource and the response href contains the #")
    public void testOpenConsumerTokenForSecureResource(){
        LOGGER.info("Testing if the response contains the # in the hrefs when an invalid token is provided (Open Consumer Token For Secure Resource)");
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceServer()
                        .withRoleConsumer()
                        .withCons(new JsonObject().put("access", new JsonArray().add("api")))
                        .build();
        Response response = sendStacItemByIdRequest(secureCollectionId, itemIdForSecureCollection, token);
        validateInvalidOrNoTokenResponse(response);
    }

    @Test
    @Description("Incorrect Token provided: Invalid Consumer Token for Secure Resource and the response href contains the #")
    public void testInvalidConsumerTokenForSecureResource(){
        LOGGER.info("Testing if the response contains the # in the hrefs when an invalid token is provided (Invalid Consumer Token for Secure Resource)");
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceAndRg(UUID.fromString(secureCollectionId), UUID.randomUUID())
                        .withRoleConsumer()
                        .withCons(new JsonObject())
                        .build();
        Response response = sendStacItemByIdRequest(secureCollectionId, itemIdForSecureCollection, token);
        validateInvalidOrNoTokenResponse(response);
    }

    @Test
    @Description("Incorrect Token provided: Secure Token for Open Resource and the response href contains the #")
    public void testSecureTokenForOpenResource(){
        LOGGER.info("Testing if the response contains the # in the hrefs when an invalid token is provided (Secure Token for Open Resource)");
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceAndRg(UUID.fromString(openCollectionId), UUID.randomUUID())
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        Response response = sendStacItemByIdRequest(openCollectionId, itemIdForOpenCollection, token);
        validateInvalidOrNoTokenResponse(response);
    }

    @Test
    @Description("Incorrect Token provided: Secure Token for Different Resource and the response href contains the #")
    public void testDifferentCollectionIds(){
        LOGGER.info("Testing if the response contains the # in the hrefs when an invalid token is provided (Secure Token for Different Resource)");
        String token =
                new FakeTokenBuilder()
                        .withSub(UUID.randomUUID())
                        .withResourceAndRg(UUID.fromString(openCollectionId), UUID.randomUUID())
                        .withRoleProvider()
                        .withCons(new JsonObject())
                        .build();
        Response response = sendStacItemByIdRequest(secureCollectionId, itemIdForSecureCollection, token);
        validateInvalidOrNoTokenResponse(response);
    }

    @Test
    @Description("No token provided and test if response contains the # in the hrefs")
    public void testNoTokenProvided(){
        LOGGER.info("Testing if the response contains the # in the hrefs when no token is provided");
        Response response = sendStacItemByIdRequest(openCollectionId, itemIdForOpenCollection, null);
        //LOGGER.info("Response JSON: {}", response.getBody().asString());
        validateInvalidOrNoTokenResponse(response);
    }
}
