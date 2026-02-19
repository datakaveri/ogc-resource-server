package ogc.rs.metering;

import static ogc.rs.common.Constants.DATA_BROKER_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.METERING_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import ogc.rs.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(MeteringVerticle.class);
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private MeteringService metering;
  private DataBrokerService dataBrokerService;
  private PgConnectOptions meteringConnectOptions;
  private PgConnectOptions dbConnectOptions;
  private PoolOptions poolOptions;

  private PgPool meteringPool;
  private PgPool dbPool;
  private String meteringDatabaseHost;
  private int meteringDatabasePort;
  private String meteringDatabaseName;
  private String meteringDatabaseUserName;
  private String meteringDatabasePassword;

  private String ogcDatabaseHost;
  private int ogcDatabasePort;
  private String ogcDatabaseName;
  private String ogcDatabaseUserName;
  private String ogcDatabasePassword;
  private int poolSize;

  @Override
  public void start() throws Exception {

    meteringDatabaseHost = config().getString("meteringDatabaseHost");
    meteringDatabasePort = config().getInteger("meteringDatabasePort");
    meteringDatabaseName = config().getString("meteringDatabaseName");
    meteringDatabaseUserName = config().getString("meteringDatabaseUser");
    meteringDatabasePassword = config().getString("meteringDatabasePassword");

    ogcDatabaseHost = config().getString("databaseHost");
    ogcDatabasePort = config().getInteger("databasePort");
    ogcDatabaseName = config().getString("databaseName");
    ogcDatabaseUserName = config().getString("databaseUser");
    ogcDatabasePassword = config().getString("databasePassword");

    poolSize = config().getInteger("poolSize");

    this.meteringConnectOptions =
        new PgConnectOptions()
            .setPort(meteringDatabasePort)
            .setHost(meteringDatabaseHost)
            .setDatabase(meteringDatabaseName)
            .setUser(meteringDatabaseUserName)
            .setPassword(meteringDatabasePassword)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000L);

    this.dbConnectOptions =
            new PgConnectOptions()
                    .setPort(ogcDatabasePort)
                    .setHost(ogcDatabaseHost)
                    .setDatabase(ogcDatabaseName)
                    .setUser(ogcDatabaseUserName)
                    .setPassword(ogcDatabasePassword)
                    .setReconnectAttempts(2)
                    .setReconnectInterval(1000L);

    this.poolOptions = new PoolOptions().setMaxSize(poolSize);
    this.meteringPool = PgPool.pool(vertx, meteringConnectOptions, poolOptions);
    this.dbPool = PgPool.pool(vertx, dbConnectOptions, poolOptions); // Create new pool for ogc db
    LOGGER.info("Metering Database Connection done");
    LOGGER.info("OGC Database Connection done");
    binder = new ServiceBinder(vertx);
    dataBrokerService = DataBrokerService.createProxy(vertx,DATA_BROKER_SERVICE_ADDRESS);
    metering = new MeteringServiceImpl(vertx, this.meteringPool, this.dbPool, config(), dataBrokerService);
    consumer =
        binder.setAddress(METERING_SERVICE_ADDRESS).register(MeteringService.class, metering);
    LOGGER.info("Metering Verticle Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
