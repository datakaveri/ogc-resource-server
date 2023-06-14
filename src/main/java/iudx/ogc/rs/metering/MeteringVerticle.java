package iudx.ogc.rs.metering;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;
import static iudx.ogc.rs.common.Constants.METERING_SERVICE_ADDRESS;

public class MeteringVerticle  extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(MeteringVerticle.class);
    private ServiceBinder binder;
    private MessageConsumer<JsonObject> consumer;
    private MeteringService metering;
    private DatabaseService databaseService;

    @Override
    public void start() throws Exception {
        binder = new ServiceBinder(vertx);
        databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

        metering = new MeteringServiceImpl(vertx, databaseService);
        consumer =
                binder.setAddress(METERING_SERVICE_ADDRESS).register(MeteringService.class, metering);
        LOGGER.info("Metering Verticle Started");
    }

    @Override
    public void stop() {
        binder.unregister(consumer);
    }
}
