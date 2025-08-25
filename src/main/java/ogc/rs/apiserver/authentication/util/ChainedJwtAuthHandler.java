package ogc.rs.apiserver.authentication.util;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.List;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChainedJwtAuthHandler implements AuthenticationHandler {
  private static final Logger LOGGER = LogManager.getLogger(ChainedJwtAuthHandler.class);
  private final List<AuthenticationHandler> handlers;
  // Constructor
  public ChainedJwtAuthHandler(List<AuthenticationHandler> handlers) {
    this.handlers = handlers;
  }
  @Override
  public void handle(RoutingContext ctx) {
    verifyNext(ctx, 0);
  }
  private void verifyNext(RoutingContext ctx, int index) {
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
  // Getter for handlers if needed
  public List<AuthenticationHandler> getHandlers() {
    return handlers;
  }
}
