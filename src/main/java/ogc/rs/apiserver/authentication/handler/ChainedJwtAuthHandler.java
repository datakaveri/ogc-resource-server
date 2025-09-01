package ogc.rs.apiserver.authentication.handler;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.List;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Authentication handler that chains multiple AuthenticationHandlers.
 * Tries each handler in order until one succeeds or all fail.
 */
public class ChainedJwtAuthHandler implements AuthenticationHandler {
  private static final Logger LOGGER = LogManager.getLogger(ChainedJwtAuthHandler.class);
  private final List<AuthenticationHandler> handlers;

  public ChainedJwtAuthHandler(List<AuthenticationHandler> handlers) {
    this.handlers = handlers;
  }
  /**
   * Handles authentication by delegating to the next handler in the chain.
   *
   * @param ctx RoutingContext for the request
   */
  @Override
  public void handle(RoutingContext ctx) {
    verifyNext(ctx, 0);
  }

  /**
   * Recursively tries each handler in the chain.
   *
   * @param ctx   RoutingContext
   * @param index current handler index
   */
  private void verifyNext(RoutingContext ctx, int index) {
    if(ctx.get("auth_error") != null && ctx.get("auth_error").toString().contains("token expired")) {
      ctx.fail(new OgcException(401, "Invalid Token", "Token Expired"));
      return;
    }

    if (index >= handlers.size()) {
      LOGGER.debug("Authentication failed at all handlers, returning 401");
      ctx.fail(new OgcException(401, "Invalid Token", "Unauthorized"));
      return;
    }

    AuthenticationHandler handler = handlers.get(index);
    LOGGER.debug("Handling authentication with handler {}", handler.getClass().getSimpleName());
    handler.handle(ctx);
    Boolean authFailed = ctx.get("auth_failed");
    if (authFailed != null && authFailed) {
      LOGGER.warn("Authentication failed at handler {}", handler.getClass().getSimpleName());
      ctx.put("auth_failed", false);
      verifyNext(ctx, index + 1); // try next handler
    }
  }

  /**
   * Returns the list of chained handlers.
   *
   * @return list of AuthenticationHandlers
   */
  public List<AuthenticationHandler> getHandlers() {
    return handlers;
  }
}
