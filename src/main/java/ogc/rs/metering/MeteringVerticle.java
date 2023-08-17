package ogc.rs.metering;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import ogc.rs.apiserver.service.CatalogueService;
import ogc.rs.database.DatabaseService;
import ogc.rs.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.common.Constants.*;
import static ogc.rs.metering.util.MeteringConstant.USER_ID;

public class MeteringVerticle  extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(MeteringVerticle.class);
    private ServiceBinder binder;
    private MessageConsumer<JsonObject> consumer;
    private MeteringService metering;
    private DatabaseService databaseService;
    private DataBrokerService databrokerService;

    @Override
    public void start() throws Exception {
        binder = new ServiceBinder(vertx);
        databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
        databrokerService = DataBrokerService.createProxy(vertx,DATABROKER_SERVICE_ADDRESS);

        metering = new MeteringServiceImpl(vertx, databaseService,config(),databrokerService);
        consumer =
                binder.setAddress(METERING_SERVICE_ADDRESS).register(MeteringService.class, metering);
        LOGGER.info("Metering Verticle Started");
    }

    @Override
    public void stop() {
        binder.unregister(consumer);
    }
}
