package ogc.rs.processes;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ProcessesRunnerService.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;
  private PgConnectOptions connectOptions;
  private PoolOptions poolOptions;
  private PgPool pool;
  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;
  private ProcessesRunnerService processService;
  static WebClient createWebClient(Vertx vertx) {
    return createWebClient(vertx, false);
  }
  static WebClient createWebClient(Vertx vertxObj, boolean testing) {
    WebClientOptions webClientOptions = new WebClientOptions();
    if (testing) {
      webClientOptions.setTrustAll(true).setVerifyHost(false);
    }
    webClientOptions.setSsl(true);
    return WebClient.create(vertxObj, webClientOptions);
  }

  @Override
  public void start() throws Exception {

    databaseIp = config().getString("databaseHost");
    databasePort = config().getInteger("databasePort");
    databaseName = config().getString("databaseName");
    databaseUserName = config().getString("databaseUser");
    databasePassword = config().getString("databasePassword");
    poolSize = config().getInteger("poolSize");

    this.connectOptions =
      new PgConnectOptions()
        .setPort(databasePort)
        .setHost(databaseIp)
        .setDatabase(databaseName)
        .setUser(databaseUserName)
        .setPassword(databasePassword)
        .setReconnectAttempts(5)
        .setReconnectInterval(1000L)
        .setTcpKeepAlive(true);

    this.poolOptions = new PoolOptions().setMaxSize(poolSize);
    this.pool = PgPool.pool(vertx, connectOptions, poolOptions);

    processService = new ProcessesRunnerImpl(pool,createWebClient(vertx),config());

    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress("ogc.rs.processes.service").register(ProcessesRunnerService.class, processService);
    LOGGER.info("Processes verticle started.");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }

}
