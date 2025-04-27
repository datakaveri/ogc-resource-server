package ogc.rs.restAssuredTest;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Description;
import ogc.rs.util.FakeTokenBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.UUID;
import java.time.Instant;
import static io.restassured.RestAssured.given;
import static ogc.rs.apiserver.util.Constants.API_CALLS_LIMIT_EXCEEDED;
import static ogc.rs.apiserver.util.Constants.DATA_USAGE_LIMIT_EXCEEDED;
import static ogc.rs.common.Constants.CODE_KEY;
import static ogc.rs.common.Constants.DESCRIPTION_KEY;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfigExtension.class)
public class UsageLimitEnforcementHandlerIT {

    private static final Logger LOGGER = LogManager.getLogger(UsageLimitEnforcementHandlerIT.class);
    String privateVectorDataEndpoint = "/collections/{collectionId}/items";
    String collectionId = "a5a6e26f-d252-446d-b7dd-4d50ea945102";
    Instant sixMonthsAgo = Instant.now().minusSeconds(6 * 30L * 24L * 60L * 60L); // 6 months ago

    private Response sendRequest(String collectionId, String token) {
        return given().pathParam("collectionId", collectionId)
                .auth().oauth2(token)
                .contentType("application/json")
                .when().get(privateVectorDataEndpoint);
    }

    @Test
    @Description("Test if API hit limit is properly enforced for a token")
    public void testApiHitLimitEnforced() {
        LOGGER.info("Testing if API hit limit is enforced for a token with a valid limit.");
        String token = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceAndRg(UUID.fromString(collectionId), UUID.randomUUID())
                .withRoleProvider()
                .withCons(new JsonObject().put("limits", new JsonObject()
                        .put("apiHits", 5000)
                        .put("iat", sixMonthsAgo.getEpochSecond())))
                .build();
        Response response = sendRequest(collectionId, token);
        response.then().statusCode(200);
    }

    @Test
    @Description("Test if data usage limit is properly enforced for a token")
    public void testDataUsageLimitEnforced() {
        LOGGER.info("Testing if data usage limit is enforced for a token with a valid limit.");
        String token = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceAndRg(UUID.fromString(collectionId), UUID.randomUUID())
                .withRoleProvider()
                .withCons(new JsonObject().put("limits", new JsonObject()
                        .put("dataUsage", "100:gb")
                        .put("iat", sixMonthsAgo.getEpochSecond())))
                .build();
        Response response = sendRequest(collectionId, token);
        response.then().statusCode(200);
    }

    @Test
    @Description("Test if no usage limit is enforced when no limits are set for a token")
    public void testNoUsageLimitEnforced() {
        LOGGER.info("Testing if no usage limit is enforced for a token without limits.");
        String token = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceAndRg(UUID.fromString(collectionId), UUID.randomUUID())
                .withRoleProvider()
                .withCons(new JsonObject()) // No usage limit
                .build();
        Response response = sendRequest(collectionId, token);
        response.then().statusCode(200);
    }

    @Test
    @Description("Test if API hit limit exceeded for a token")
    public void testApiHitLimitExceeded() {
        LOGGER.info("Testing if API hit limit is exceeded for a token.");
        String token = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceAndRg(UUID.fromString(collectionId), UUID.randomUUID())
                .withRoleProvider()
                .withCons(new JsonObject().put("limits", new JsonObject()
                        .put("apiHits", 0)
                        .put("iat", sixMonthsAgo.getEpochSecond())))
                .build();
        Response response = sendRequest(collectionId, token);
        response.then().statusCode(429).body(DESCRIPTION_KEY, is(API_CALLS_LIMIT_EXCEEDED));
    }

    @Test
    @Description("Test if data usage limit exceeded for a token")
    public void testDataUsageLimitExceeded() {
        LOGGER.info("Testing if data usage limit is exceeded for a token.");
        String token = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceAndRg(UUID.fromString(collectionId), UUID.randomUUID())
                .withRoleProvider()
                .withCons(new JsonObject().put("limits", new JsonObject()
                        .put("dataUsage", "0:kb")
                        .put("iat", sixMonthsAgo.getEpochSecond())))
                .build();
        Response response = sendRequest(collectionId, token);
        response.then().statusCode(429).body(DESCRIPTION_KEY, is(DATA_USAGE_LIMIT_EXCEEDED));
    }

    @Test
    @Description("Test handling of unsupported data usage unit in token constraints.")
    public void testInvalidDataUsageUnit() {
        LOGGER.info("Testing invalid data unit for a token.");
        String token = new FakeTokenBuilder()
                .withSub(UUID.fromString("0ff3d306-9402-4430-8e18-6f95e4c03c97"))
                .withResourceAndRg(UUID.fromString(collectionId), UUID.randomUUID())
                .withRoleProvider()
                .withCons(new JsonObject().put("limits", new JsonObject()
                        .put("dataUsage", "0:pb")
                        .put("iat", sixMonthsAgo.getEpochSecond())))
                .build();
        Response response = sendRequest(collectionId, token);
        response.then().statusCode(400).body(CODE_KEY, is("Bad Request"));
    }
}
