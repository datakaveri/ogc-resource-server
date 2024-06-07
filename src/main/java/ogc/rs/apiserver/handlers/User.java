package ogc.rs.apiserver.handlers;

import io.vertx.core.json.JsonObject;
import ogc.rs.apiserver.util.OgcException;

import java.util.UUID;

public class User {

  private UUID userId;
  private RoleEnum role;
  private JsonObject constraints;
  private RoleEnum delegatorRole;
  private UUID delegatorUserId;
  private UUID resourceId;
  private boolean isRsToken;

  public enum RoleEnum {
    provider,
    consumer,
    admin,
    delegate
  }

  public static User createUser(JsonObject tokenDetails) throws OgcException {
    User user = new User();
    user.userId = UUID.fromString(tokenDetails.getString("sub"));
    user.role = RoleEnum.valueOf(tokenDetails.getString("role"));
    user.constraints = tokenDetails.getJsonObject("cons");
    if (tokenDetails.getString("role").equals("delegate")) {
      user.delegatorRole = RoleEnum.valueOf(tokenDetails.getString("drl"));
      user.delegatorUserId = UUID.fromString(tokenDetails.getString("did"));
    }
    String iid = tokenDetails.getString("iid");
    user.resourceId = iid.startsWith("ri:") ? UUID.fromString(iid.substring(3)) : null;
    user.isRsToken = iid.startsWith("rs:");

    // Audience validation
    if (user.isRsToken && !iid.contains(tokenDetails.getString("aud"))) {
      throw new OgcException(401, "Invalid Token", "Invalid Token");
    }
    return user;
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
        + '}';
  }
}
