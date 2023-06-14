package iudx.ogc.rs.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JwtAuthenticationServiceImpl implements AuthenticationService {
    private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);

    @Override
    public AuthenticationService tokenIntrospect(JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
        return null;
    }
}
