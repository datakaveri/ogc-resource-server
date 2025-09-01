package ogc.rs.apiserver.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

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

  public static AuthInfo fromKeycloakOrAuthV2Token(JsonObject tokenDetails){
    AuthInfo user = new AuthInfo();
    user.userId = UUID.fromString(tokenDetails.getString("sub"));
    boolean isProvider = (tokenDetails.getJsonObject("realm_access").getJsonArray("roles").contains("provider"));
    if(isProvider){
      user.role = RoleEnum.provider;
    } else {
      user.role = RoleEnum.consumer;
    }
    /*TODO: Set required constraints here*/
    user.constraints = new JsonObject().put("access", new JsonArray().add("api")) ;
//    user.constraints = new JsonObject().put("access", new JsonArray().add("api"));
    user.expiry = tokenDetails.containsKey("exp") ? tokenDetails.getLong("exp") : 0L;
    // No iid present in the token to extract resourceId or isRsToken
    user.isRsToken = false;
    // No audience validation as audience is CLAIM_AUDIENCE in Keycloak token

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
