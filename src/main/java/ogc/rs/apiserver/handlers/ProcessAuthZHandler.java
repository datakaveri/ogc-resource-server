package ogc.rs.apiserver.handlers;

import static ogc.rs.apiserver.util.Constants.ECHO_PROCESS_EXECUTION_ENDPOINT;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.OgcException;
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
    /* Scope check (own-asset-management) is done at route level via AuthorizationHandler.forScopes */
    routingContext.next();
  }
}
