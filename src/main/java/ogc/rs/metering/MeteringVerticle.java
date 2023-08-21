package ogc.rs.metering;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import ogc.rs.common.VirtualHosts;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.common.Constants.*;

public class MeteringVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(MeteringVerticle.class);
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private MeteringService metering;
  private DatabaseService databaseService;
  private String dataBrokerIp;
  private int dataBrokerPort;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private boolean automaticRecoveryEnabled;
  private String virtualHost;
  private RabbitMQOptions config;
  private RabbitMQClient client;
  private DataBrokerService dataBrokerService;

  @Override
  public void start() throws Exception {
    dataBrokerIp = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval = config().getInteger("networkRecoveryInterval");
    automaticRecoveryEnabled = config().getBoolean("automaticRecoveryEnabled");
    virtualHost = config().getString(VirtualHosts.IUDX_INTERNAL.value);

    /* Configure the RabbitMQ Data Broker client with input from config files. */

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

    client = RabbitMQClient.create(vertx, config);

    binder = new ServiceBinder(vertx);
    databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    dataBrokerService = new DataBrokerServiceImpl(client);
    metering = new MeteringServiceImpl(vertx, databaseService, config(), dataBrokerService);
    consumer =
        binder.setAddress(METERING_SERVICE_ADDRESS).register(MeteringService.class, metering);
    LOGGER.info("Metering Verticle Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
