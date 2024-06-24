package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.AuthInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;

public class MeteringAuthZHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(MeteringAuthZHandler.class);

  /**
   * Validate access to metering APIs.
   * Handler for authorizing access to metering APIs based on user roles and tokens.
   *
   * @param routingContext the routing context of the request
   *
   * +--------------------+--------------+--------------+------------+-------------------+------------+
   * | API name           | Delegate     | Consumer     | Admin      | Provider/Delegate | Consumer   |
   * |                    | Secure Token | Secure Token | Open Token | Open Token        | Open Token |
   * +--------------------+--------------+--------------+------------+-------------------+------------+
   * | Provider Audit API | yes          | no           | yes        | yes               | no         |
   * +--------------------+--------------+--------------+------------+-------------------+------------+
   * | Consumer Audit API | yes          | yes          | yes        | yes               | yes        |
   * +--------------------+--------------+--------------+------------+-------------------+------------+
   * | Overview API       | yes          | yes          | yes        | no                | yes        |
   * +--------------------+--------------+--------------+------------+-------------------+------------+
   * | Summary API        | yes          | yes          | yes        | no                | yes        |
   * +--------------------+--------------+--------------+------------+-------------------+------------+
   */
  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("Metering Authorization");
    AuthInfo user = routingContext.get(USER_KEY);
    JsonObject results = new JsonObject();
    results.put("userid", user.getUserId().toString());
    results.put("role", user.getRole());
    if (!user.isRsToken()) {
      results.put("iid", user.getResourceId().toString());
    }
    String path = routingContext.normalizedPath();
    switch (path) {
      case "/ngsi-ld/v1/consumer/audit":
        routingContext.data().put("authInfo", results);
        routingContext.next();
        break;

      case "/ngsi-ld/v1/provider/audit":
        if (user.getRole() == AuthInfo.RoleEnum.consumer) {
          routingContext.fail(
              new OgcException(401, "Not Authorized", "User with consumer role cannot access API"));
        } else {
          routingContext.data().put("authInfo", results);
          routingContext.next();
        }
        break;

      case "/ngsi-ld/v1/overview": // uses similar authZ to summary
      case "/ngsi-ld/v1/summary":
        if (user.isRsToken()
            && (user.getRole() == AuthInfo.RoleEnum.provider
                || user.getRole() == AuthInfo.RoleEnum.delegate)) {
          routingContext.fail(
              new OgcException(
                  401,
                  "Not Authorized",
                  "User with provider/delegate role cannot access API with RS token"));
        } else {
          routingContext.data().put("authInfo", results);
          routingContext.next();
        }
        break;

      default:
        LOGGER.error("No AuthZ defined for metering API {}", path);
        routingContext.fail(new OgcException(401, "Not Authorized", "User is not authorised."));
    }
  }
}
