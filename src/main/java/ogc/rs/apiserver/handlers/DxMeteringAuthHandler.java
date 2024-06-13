package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;

public class DxMeteringAuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(DxMeteringAuthHandler.class);

    /**
     * Validate access to metering APIs.
     *
     *
    +--------------------+--------------+--------------+------------+-------------------+------------+
    | API name           | Delegate     | Consumer     | Admin      | Provider/Delegate | Consumer   |
    |                    | Secure Token | Secure Token | Open Token | Open Token        | Open Token |
    +--------------------+--------------+--------------+------------+-------------------+------------+
    | Provider Audit API | yes          | no           | yes        | yes               | no         |
    +--------------------+--------------+--------------+------------+-------------------+------------+
    | Consumer Audit API | yes          | yes          | yes        | yes               | yes        |
    +--------------------+--------------+--------------+------------+-------------------+------------+
    | Overview API       | yes          | yes          | yes        | no                | yes        |
    +--------------------+--------------+--------------+------------+-------------------+------------+
    | Summary API        | yes          | yes          | yes        | no                | yes        |
    +--------------------+--------------+--------------+------------+-------------------+------------+
     */
  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("Metering Authorization");
    User user = routingContext.get(USER_KEY);
    UUID iid = user.getResourceId();
    JsonObject results = new JsonObject();
    results.put("iid", iid);
    results.put("userid", user.getUserId().toString());
    results.put("role", user.getRole());
    String path = routingContext.normalizedPath();
    switch (path) {
      case "/ngsi-ld/v1/consumer/audit":
        results.put("isAuthorised", true);
        routingContext.data().put("authInfo", results);
        routingContext.data().put("isAuthorised", results.getBoolean("isAuthorised"));
        routingContext.next();
        break;

      case "/ngsi-ld/v1/provider/audit":
        if (user.getRole() == User.RoleEnum.consumer) {
          routingContext.fail(
              new OgcException(401, "Not Authorized", "User with consumer role cannot access API"));
        } else {
          results.put("isAuthorised", true);
          routingContext.data().put("authInfo", results);
          routingContext.data().put("isAuthorised", results.getBoolean("isAuthorised"));
          routingContext.next();
        }
        break;

      case "/ngsi-ld/v1/overview": // uses similar authZ to summary
      case "/ngsi-ld/v1/summary":
        if (user.isRsToken()
            && (user.getRole() == User.RoleEnum.provider
                || user.getRole() == User.RoleEnum.delegate)) {
          routingContext.fail(
              new OgcException(
                  401,
                  "Not Authorized",
                  "User with provider/delegate role cannot access API with RS token"));
        } else {
          results.put("isAuthorised", true);
          routingContext.data().put("authInfo", results);
          routingContext.data().put("isAuthorised", results.getBoolean("isAuthorised"));
          routingContext.next();
        }
        break;

      default:
        LOGGER.error("No AuthZ defined for metering API {}", path);
        routingContext.fail(new OgcException(401, "Not Authorized", "User is not authorised."));
    }
  }
}
