package ogc.rs.authenticator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Authentication Verticle.
 *
 * <h1>Authentication Verticle</h1>
 *
 * <p>The Authentication Verticle implementation in the the OGC Resource Server exposes the {@link
 * AuthenticationService} over the Vert.x Event Bus.
 *
 * @version 0.0.1
 * @since 2023-06-13
 */
public class AuthenticationVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(AuthenticationVerticle.class);
    private AuthenticationService jwtAuthenticationService;
    private ServiceBinder binder;
    private MessageConsumer<JsonObject> consumer;

    @Override
    public void start() throws Exception {
        LOGGER.info("Authentication verticle deployed");
    }

    @Override
    public void stop() {
        binder.unregister(consumer);
    }

}
