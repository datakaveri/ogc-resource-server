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

public class ProcessAuthZHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(ProcessAuthZHandler.class);

  /**
   * Handles the routing context to authorize access to process APIs.
   *
   * @param routingContext the routing context of the request
   */
  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("Process Authorization");
    AuthInfo user = routingContext.get(USER_KEY);
    UUID iid = user.getResourceId();
    LOGGER.debug("Validating access for execution ");
    if (!user.isRsToken()) {
      LOGGER.error("Resource Ids don't match!");
      routingContext.fail(
          new OgcException(
              401, "Not Authorized", "User is not authorised. Please contact IUDX AAA "));
      return;
    } else if ((user.getRole() == AuthInfo.RoleEnum.provider)) {
      JsonObject results = new JsonObject();
      results.put("iid", iid);
      results.put("userId", user.getUserId());
      results.put("role", user.getRole());
      routingContext.data().put("authInfo", results);
      routingContext.next();
    }
    else if ((user.getRole() == AuthInfo.RoleEnum.delegate && user.getDelegatorRole() == AuthInfo.RoleEnum.provider)) {
      JsonObject results = new JsonObject();
      results.put("iid", iid);
      results.put("userId", user.getDelegatorUserId());
      results.put("role", user.getDelegatorRole());
      routingContext.data().put("authInfo", results);
      routingContext.next();
    }
    else {
      LOGGER.debug("Not a provider or a provider delegate token. It is of role {} ", user.getRole());
      routingContext.fail(
          new OgcException(
              401, "Not Authorized", "User is not authorised. Please contact IUDX AAA "));
    }
  }
}
