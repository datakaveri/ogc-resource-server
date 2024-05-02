package ogc.rs.processes;

import static ogc.rs.common.Constants.PROCESSING_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import ogc.rs.common.DataFromS3;
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
  private String S3_BUCKET;
  private String S3_REGION;
  private String S3_ACCESS_KEY;
  private String S3_SECRET_KEY;
  private ProcessesRunnerService processService;
  private HttpClientOptions httpClientOptions;
  private HttpClient httpClient;
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

    S3_BUCKET = config().getString("s3BucketUrl");
    S3_REGION = config().getString("awsRegion");
    S3_ACCESS_KEY = config().getString("awsAccessKey");
    S3_SECRET_KEY = config().getString("awsSecretKey");

    httpClientOptions = new HttpClientOptions().setSsl(true);
    httpClient = vertx.createHttpClient(httpClientOptions);
    DataFromS3 dataFromS3 =
            new DataFromS3(httpClient, S3_BUCKET, S3_REGION, S3_ACCESS_KEY, S3_SECRET_KEY);

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

    processService = new ProcessesRunnerImpl(pool,createWebClient(vertx),config(),dataFromS3,vertx);

    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(PROCESSING_SERVICE_ADDRESS).register(ProcessesRunnerService.class, processService);
    LOGGER.debug("Processes verticle started.");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }

}
