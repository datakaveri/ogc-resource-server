package ogc.rs.apiserver.authorization.util;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ogc.rs.apiserver.authentication.util.DxUser;
import ogc.rs.apiserver.authorization.model.Asset;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.auditing.model.AuditLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoutingContextHelper {
  private static final Logger LOGGER = LogManager.getLogger(RoutingContextHelper.class);

  public static String getToken(RoutingContext routingContext) {
    return routingContext.get("Authorization");
  }

  public static void addToken(RoutingContext routingContext, String token) {
    routingContext.put("Authorization", token);
  }


  public static DxUser fromPrincipal(RoutingContext ctx) {
    JsonObject principal = ctx.user().principal();

    // Handle nested JSON objects like realm_access
    List<String> roles = principal
        .getJsonObject("realm_access", new JsonObject())
        .getJsonArray("roles", new io.vertx.core.json.JsonArray())
        .getList();

    UUID userId;
    try {
      userId = UUID.fromString(principal.getString("sub"));
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new OgcException(401, "Unauthorized", "Invalid or missing 'sub' UUID in token");
    }
    Long tokenExpiry = ctx.user().attributes().getLong("exp");

    if (tokenExpiry == null) {
      throw new OgcException(401, "Unauthorized", "Missing 'exp' in token");
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
        null,
        tokenExpiry
    );

  }
    public static Optional<List<AuditLog>> getAuditingLog(RoutingContext routingContext) {
        return Optional.ofNullable(routingContext.get("auditingLog"));
    }

    public static void setAuditingLog(RoutingContext routingContext, AuditLog auditingLog) {
        List<AuditLog> logs = getAuditingLog(routingContext).orElseGet(ArrayList::new);
        logs.add(auditingLog);
        routingContext.put("auditingLog", logs);
    }

    public static String getRequestPath(RoutingContext routingContext) {
        return routingContext.request().path();
    }
    public static String getRequestMethod(RoutingContext routingContext) {
        return routingContext.request().method().toString();
    }

    public static void setAsset(RoutingContext routingContext, Asset asset) {
    routingContext.put("asset", asset);
  }

  public static Asset getAsset(RoutingContext routingContext) {
    return routingContext.get("asset");
  }


}