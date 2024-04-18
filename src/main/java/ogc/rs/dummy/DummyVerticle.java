package ogc.rs.dummy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Authentication Verticle.
 *
 * <h1>Authentication Verticle</h1>
 *
 * <p>The Authentication Verticle implementation in the the OGC Resource Server exposes the {@link
 * DummyService} over the Vert.x Event Bus.
 *
 * @version 0.0.1
 * @since 2023-06-13
 */
public class DummyVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(DummyVerticle.class);
    private DummyService jwtAuthenticationService;
    private ServiceBinder binder;
    private MessageConsumer<JsonObject> consumer;
    private WebClient webClient;
    @Override
    public void start() throws Exception {
                binder = new ServiceBinder(vertx);
                 jwtAuthenticationService =
                        new DummyServiceImpl();
                    /* Publish the Authentication service with the Event Bus against an address. */
                    consumer =
                        binder
                            .setAddress("ogc.rs.dummy.service")
                            .register(DummyService.class, jwtAuthenticationService);

                    LOGGER.info("Dummy verticle deployed");
                 }

    @Override
    public void stop() {
        binder.unregister(consumer);
    }
}
