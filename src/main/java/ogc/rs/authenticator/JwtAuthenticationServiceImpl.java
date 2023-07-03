package ogc.rs.authenticator;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JwtAuthenticationServiceImpl implements AuthenticationService {
    private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);

    @Override
    public Future<JsonObject> tokenIntrospect(JsonObject request, JsonObject authenticationInfo) {
        return null;
    }
}
