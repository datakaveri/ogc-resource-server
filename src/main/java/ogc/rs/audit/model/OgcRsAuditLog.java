package ogc.rs.audit.model;

import io.vertx.core.json.JsonObject;
import ogc.rs.auditing.model.AuditLog;

import java.util.UUID;

public class OgcRsAuditLog implements AuditLog {
  private final UUID id;
  private final UUID assetId;
  private final String logType;
  private final String operation; // CRUD
  private final String createdAt;
  private final String api;
  private final String method;
  private final Long size;
  private final String role; //token ask gokul keycloak can have multiple roles
  private final UUID userId;  //token
  private final String originServer; // ananjay se puco ya tabke me enum dekh lena
  private final String iss;
  private final String delegationId; // token ya control plane
  private final String
      organizationId; // Optional field  in auditing server [organization id of consumer, provider].
  // Can be null sometimes
  private final String
      organizationName; // Optional field in auditing server [organization name of consumer,
  String ipAddress;

  String userAgent;
  // provider]. Can be null sometimes

  // change this to AuditingHandler and it can be a helper
  public OgcRsAuditLog(
      UUID id,
      UUID assetId,
      String logType,
      String operation,
      String createdAt,
      String api,
      String method,
      Long size,
      String role,
      UUID userId,
      String originServer,
      String organizationId,
      String organizationName,
      String iss,
      String delegateId,String ipAddress,String userAgent) {
    this.id = id;
    this.assetId = assetId;
    this.logType = logType;
    this.operation = operation;
    this.createdAt = createdAt;
    this.api = api;
    this.method = method;
    this.size = size;
    this.role = role;
    this.userId = userId;
    this.originServer = originServer;
    this.organizationId = organizationId;
    this.organizationName = organizationName;
    this.iss = iss;
    this.delegationId = delegateId;
    this.ipAddress=ipAddress;
    this.userAgent=userAgent;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("id", id.toString());
    json.put("asset_id", assetId.toString());
    json.put("log_type", logType);
    json.put("action", operation);
    json.put("created_at", createdAt);
    json.put("api", api);
    json.put("method", method);
    json.put("size_byte", size);
    json.put("role", role);
    json.put("user_id", userId.toString());
    json.put("origin_server", originServer);
    json.put("org_id", organizationId);
    json.put("org_name", organizationName);
    json.put("issuer", iss);
    json.put("delegationId", delegationId);
    json.put("ip_address", ipAddress);
    json.put("user_agent",userAgent);
    return json;
  }
}
