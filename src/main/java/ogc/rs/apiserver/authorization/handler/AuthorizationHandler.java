package ogc.rs.apiserver.authorization.handler;

import static ogc.rs.apiserver.util.Constants.NOT_AUTHORIZED;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import ogc.rs.apiserver.authorization.registry.SystemRoleScopeMap;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Route-level, scope-based authorization, mirroring {@code
 * org.cdpg.dx.auth.authorization.handler.AuthorizationHandler#forScopes} in dx-common. Reads the
 * {@code "scopes"} array pre-computed by {@link
 * ogc.rs.apiserver.authentication.handler.MultiIssuerJwtAuthHandler}.
 *
 * <pre>
 *   builder.operation(EXECUTE_API)
 *       .handler(AuthorizationHandler.forScopes(Scopes.OWN_ASSET_MANAGEMENT))
 *       .handler(...);
 * </pre>
 */
public final class AuthorizationHandler {

  private static final Logger LOGGER = LogManager.getLogger(AuthorizationHandler.class);

  private AuthorizationHandler() {}

  /** Passes if the user's effective scopes contain <em>any</em> of the required scopes. */
  public static Handler<RoutingContext> forScopes(String... required) {
    Objects.requireNonNull(required, "required");
    if (required.length == 0) {
      throw new IllegalArgumentException("forScopes requires at least one scope");
    }
    Set<String> requiredSet = new HashSet<>(Arrays.asList(required));

    return ctx -> {
      if (ctx.user() == null) {
        LOGGER.warn("Authorization denied: no authenticated user, path={}", ctx.request().path());
        ctx.fail(new OgcException(401, NOT_AUTHORIZED, "No authenticated user"));
        return;
      }
      JsonArray scopes =
          ctx.user().principal().getJsonArray(SystemRoleScopeMap.CLAIM_SCOPES, new JsonArray());
      LOGGER.debug("Effective scopes: {}", scopes);

      if (scopes.stream().map(String::valueOf).anyMatch(requiredSet::contains)) {
        ctx.next();
      } else {
        LOGGER.warn(
            "Authorization denied: required scopes={}, path={}", requiredSet, ctx.request().path());
        ctx.fail(
            new OgcException(
                401, NOT_AUTHORIZED, "User is not authorised. Please contact DX AAA "));
      }
    };
  }
}
