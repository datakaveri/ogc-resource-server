package ogc.rs.apiserver.util;

import io.vertx.core.json.JsonObject;

import java.util.UUID;
import java.util.logging.Logger;

public class AuthInfo {

  private UUID userId;
  private RoleEnum role;
  private JsonObject constraints;
  private RoleEnum delegatorRole;
  private UUID delegatorUserId;
  private UUID resourceId;
  private boolean isRsToken;
  private long expiry;

  public enum RoleEnum {
    provider,
    consumer,
    admin,
    delegate
  }

  public static AuthInfo createUser(JsonObject tokenDetails) throws OgcException {
    AuthInfo user = new AuthInfo();
    user.userId = UUID.fromString(tokenDetails.getString("sub"));
    user.role = RoleEnum.valueOf(tokenDetails.getString("role"));
    user.constraints = tokenDetails.getJsonObject("cons");
    user.expiry = tokenDetails.containsKey("exp") ? tokenDetails.getLong("exp", 0L) : 0L;
    if (tokenDetails.getString("role").equals("delegate")) {
      user.delegatorRole = RoleEnum.valueOf(tokenDetails.getString("drl"));
      user.delegatorUserId = UUID.fromString(tokenDetails.getString("did"));
    }
    String iid = tokenDetails.getString("iid");
    user.resourceId = iid.startsWith("ri:") ? UUID.fromString(iid.substring(3)) : null;
    user.isRsToken = iid.startsWith("rs:");

    // Audience validation
    if (user.isRsToken && !iid.contains(tokenDetails.getString("aud"))) {
      throw new OgcException(401, "Invalid Token", "Invalid Audience Value");
    }
    return user;
  }

  public UUID getUserId() {
    return userId;
  }

  public RoleEnum getRole() {
    return role;
  }

  public JsonObject getConstraints() {
    return constraints;
  }

  public RoleEnum getDelegatorRole() {
    return delegatorRole;
  }

  public UUID getDelegatorUserId() {
    return delegatorUserId;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  /**
   * Used to set resource ID when the token is a resource server token and the effective resource ID
   * is determined in authorization handlers.
   * 
   * @param resourceId
   */
  public void setResourceId(UUID resourceId) {
    this.resourceId = resourceId; 
  }

  public boolean isRsToken() {
    return isRsToken;
  }

  public long getExpiry() {
    return expiry;
  }

  @Override
  public String toString() {
    return "{"
        + "userId="
        + userId
        + ", role="
        + role
        + ", constraints="
        + constraints
        + ", delegatorRole="
        + delegatorRole
        + ", delegatorUserId="
        + delegatorUserId
        + ", resourceId="
        + resourceId
        + ", isRsToken="
        + isRsToken
            + ", expiry="
            + expiry +
            '}';
  }
}
