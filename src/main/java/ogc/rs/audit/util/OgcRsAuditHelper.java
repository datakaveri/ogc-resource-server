package ogc.rs.audit.util;

import ogc.rs.apiserver.authorization.model.Asset;
import ogc.rs.audit.model.OgcRsAuditLog;
import ogc.rs.auditing.model.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;


public class OgcRsAuditHelper {
  public static AuditLog createAuditingLogs(
      Asset asset,
      String apiEndpoint,
      String method,
      UUID userId,
      String organizationName,
      String serverName,
      String role, String operation,String iss, String delegateId,String ipAddress,String userAgent) {
    UUID id = UUID.randomUUID();
    return new OgcRsAuditLog(
        id,
        UUID.fromString(asset.getItemId()),
        "ASSET",
        operation,
        LocalDateTime.now().toString(),
        apiEndpoint,
        method,
        0L,
        role,
        userId,
        serverName,
        asset.getOrganizationId(),
        organizationName,
        iss,
        delegateId,ipAddress,userAgent);
  }

}
