package ogc.rs.apiserver.handlers;

import static ogc.rs.apiserver.util.Constants.ECHO_PROCESS_EXECUTION_ENDPOINT;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import ogc.rs.apiserver.authentication.util.DxUser;
import ogc.rs.apiserver.authorization.model.DxRole;
import ogc.rs.apiserver.authorization.util.RoutingContextHelper;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessAuthZHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(ProcessAuthZHandler.class);
  // Check if fake token is enabled (set via JVM option -Dfake-token=true)
  private static final boolean FAKE_TOKEN_ENABLED =
      Boolean.parseBoolean(System.getProperty("fake-token", "false"));

  /**
   * Handles the routing context to authorize access to process APIs.
   *
   * @param routingContext the routing context of the request
   */
  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("Inside ProcessAuthZHandler");

    DxUser user = RoutingContextHelper.fromPrincipal(routingContext);
    List<String> roles = user.getRoles();
    // Block echo process unless fake-token=true
    String requestPath = routingContext.normalizedPath();
    if (requestPath.contains(ECHO_PROCESS_EXECUTION_ENDPOINT)) {
      if (!FAKE_TOKEN_ENABLED) {
        LOGGER.error("Attempted to run Echo process without fake-token=true");
        routingContext.fail(
            new OgcException(
                403,
                "ForbiddenProcess",
                "The echo process is only available for testing purposes with fake-token enabled"
            )
        );
        return;
      }
    }
    LOGGER.debug("Validating access for execution ");
    if (roles.contains(DxRole.PROVIDER.getRole())) {
      routingContext.next();
    } else {
      LOGGER.debug("Not a provider token. It is of role {} ", roles);
      routingContext.fail(
          new OgcException(
              401, "Not Authorized", "User is not authorised. Please contact DX AAA "));

    }

  }
}
