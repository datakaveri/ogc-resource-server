package ogc.rs.jobs;

import static ogc.rs.common.Constants.JOBS_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.PROCESSING_SERVICE_ADDRESS;

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

public class JobsVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(JobsVerticle.class);
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
  private JobsService jobsService;

  @Override
  public void start() throws Exception {

    databaseIp = config().getString("databaseHost");
    databasePort = config().getInteger("databasePort");
    databaseName = config().getString("databaseName");
    databaseUserName = config().getString("databaseUser");
    databasePassword = config().getString("databasePassword");
    poolSize = config().getInteger("poolSize");

    this.connectOptions =
      new PgConnectOptions().setPort(databasePort).setHost(databaseIp).setDatabase(databaseName)
        .setUser(databaseUserName).setPassword(databasePassword).setReconnectAttempts(5)
        .setReconnectInterval(1000L).setTcpKeepAlive(true);

    this.poolOptions = new PoolOptions().setMaxSize(poolSize);
    this.pool = PgPool.pool(vertx, connectOptions, poolOptions);


    jobsService = new JobsServiceImpl(pool, config());

    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(JOBS_SERVICE_ADDRESS).register(JobsService.class, jobsService);
    LOGGER.debug("Jobs verticle started.");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }

}
