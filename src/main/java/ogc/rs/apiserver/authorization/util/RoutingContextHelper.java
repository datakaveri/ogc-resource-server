package ogc.rs.apiserver.authorization.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ogc.rs.apiserver.util.OgcException;

public class RoutingContextHelper {

  public static DxUser fromPrincipal(RoutingContext ctx) {
    JsonObject principal = ctx.user().principal();

    // Handle nested JSON objects like realm_access
    List<String> roles = principal
        .getJsonObject("realm_access", new JsonObject())
        .getJsonArray("roles", new JsonArray())
        .getList();

    UUID userId;
    try {
      userId = UUID.fromString(principal.getString("sub"));
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new OgcException(401, "Unauthorized" , "Invalid or missing 'sub' UUID in token");
    }

    return new DxUser(
        roles,
        principal.getString("organisation_id", null),
        principal.getString("organisation_name", null), // assuming this is correct and intentional
        userId,
        principal.getBoolean("email_verified", false),
        principal.getBoolean("kyc_verified", false),
        principal.getString("name"),
        principal.getString("preferred_username"),
        principal.getString("given_name"),
        principal.getString("family_name"),
        principal.getString("email"),
        new ArrayList<>(),
        new JsonObject(),
        null,
        new JsonObject(),
        "",
        "",
        "",
        null
    );

  }

  public static void addCollectionId(RoutingContext routingContext, String collectionId) {
    routingContext.put("collectionId", collectionId);
  }

  public static String getCollectionId(RoutingContext routingContext) {
    return routingContext.get("collectionId");
  }
}
