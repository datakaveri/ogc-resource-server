package ogc.rs.metering;

import static ogc.rs.common.Constants.METERING_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(MeteringVerticle.class);
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private MeteringService metering;
  private DataBrokerService dataBrokerService;
  private PgConnectOptions meteringConnectOptions;
  private PoolOptions poolOptions;
  private PgPool meteringPool;
  private String meteringDatabaseHost;
  private int meteringDatabasePort;
  private String meteringDatabaseName;
  private String meteringDatabaseUserName;
  private String meteringDatabasePassword;
  private int poolSize;

  @Override
  public void start() throws Exception {
    String dataBrokerIp = config().getString("dataBrokerIP");
    int dataBrokerPort = config().getInteger("dataBrokerPort");
    String dataBrokerUserName = config().getString("dataBrokerUserName");
    String dataBrokerPassword = config().getString("dataBrokerPassword");
    int connectionTimeout = config().getInteger("connectionTimeout");
    int requestedHeartbeat = config().getInteger("requestedHeartbeat");
    int handshakeTimeout = config().getInteger("handshakeTimeout");
    int requestedChannelMax = config().getInteger("requestedChannelMax");
    int networkRecoveryInterval = config().getInteger("networkRecoveryInterval");
    boolean automaticRecoveryEnabled = config().getBoolean("automaticRecoveryEnabled");
    String virtualHost = config().getString("internalVhost");

    /* Configure the RabbitMQ Data Broker client with input from config files. */
    RabbitMQOptions config;
    config = new RabbitMQOptions();
    config.setUser(dataBrokerUserName);
    config.setPassword(dataBrokerPassword);
    config.setHost(dataBrokerIp);
    config.setPort(dataBrokerPort);
    config.setConnectionTimeout(connectionTimeout);
    config.setRequestedHeartbeat(requestedHeartbeat);
    config.setHandshakeTimeout(handshakeTimeout);
    config.setRequestedChannelMax(requestedChannelMax);
    config.setNetworkRecoveryInterval(networkRecoveryInterval);
    config.setAutomaticRecoveryEnabled(automaticRecoveryEnabled);
    config.setVirtualHost(virtualHost);
    RabbitMQClient client;
    client = RabbitMQClient.create(vertx, config);

    meteringDatabaseHost = config().getString("meteringDatabaseHost");
    meteringDatabasePort = config().getInteger("meteringDatabasePort");
    meteringDatabaseName = config().getString("meteringDatabaseName");
    meteringDatabaseUserName = config().getString("meteringDatabaseUser");
    meteringDatabasePassword = config().getString("meteringDatabasePassword");
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

    this.poolOptions = new PoolOptions().setMaxSize(poolSize);
    this.meteringPool = PgPool.pool(vertx, meteringConnectOptions, poolOptions);
    LOGGER.info("Database Metering Connection done");
    binder = new ServiceBinder(vertx);
    dataBrokerService = new DataBrokerServiceImpl(client);
    metering = new MeteringServiceImpl(vertx, this.meteringPool, config(), dataBrokerService);
    consumer =
        binder.setAddress(METERING_SERVICE_ADDRESS).register(MeteringService.class, metering);
    LOGGER.info("Metering Verticle Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
