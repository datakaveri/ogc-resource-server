package ogc.rs.audit.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ogc.rs.apiserver.authorization.model.Asset;
import ogc.rs.audit.model.OgcRsAuditLog;
import ogc.rs.auditing.model.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;


public class OgcRsAuditHelper {

  public static final String ACTOR_TYPE_SELF = "SELF";
  public static final String ACTOR_TYPE_DELEGATE = "DELEGATE";
  public static final String ACTOR_TYPE_APP = "APP";

  /**
   * Who actually performed the action. Delegation takes precedence, then app credentials,
   * defaulting to SELF (same order as controlplane's AuditContextExtractor.getActorType).
   */
  public static String resolveActorType(JsonObject principal, String delegateId) {
    if (delegateId != null) return ACTOR_TYPE_DELEGATE;
    if (principal.getString("app_id") != null) return ACTOR_TYPE_APP;
    return ACTOR_TYPE_SELF;
  }

  /** Effective role of the acting-as user (the delegator's roles after delegation). */
  public static String resolveRole(JsonObject principal) {
    if (principal.getString("app_id") != null) return "consumer";
    JsonArray roles =
        principal
            .getJsonObject("realm_access", new JsonObject())
            .getJsonArray("roles", new JsonArray());
    if (roles.contains("provider")) return "provider";
    return "consumer";
  }

  public static AuditLog createAuditingLogs(
      Asset asset,
      String apiEndpoint,
      String method,
      UUID userId,
      String organizationName,
      String serverName,
      String role,
      String operation,
      String iss,
      String delegateId,
      String actorType,
      String ipAddress,
      String userAgent,
      long sizeBytes) {
    UUID id = UUID.randomUUID();
    return new OgcRsAuditLog(
        id,
        UUID.fromString(asset.getItemId()),
        "ASSET",
        operation,
        LocalDateTime.now().toString(),
        apiEndpoint,
        method,
        sizeBytes,
        role,
        userId,
        serverName,
        asset.getOrganizationId(),
        organizationName,
        iss,
        delegateId,
        actorType,
        ipAddress,
        userAgent);
  }

}
