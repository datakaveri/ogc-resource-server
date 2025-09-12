package ogc.rs.catalogue;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import static ogc.rs.common.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CatalogueVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(CatalogueVerticle.class);
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

  private CatalogueService catalogueService;
  private WebClient catWebClient;
  private String host;
  private int port;
  private String catBasePath;
  private String catSearchPath;
  private String catalogueItemPath;

  @Override
  public void start() throws Exception {



    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(false).setVerifyHost(true).setSsl(true);
    catWebClient = WebClient.create(vertx, options);
    host = config().getString("catServerHost");
    port = config().getInteger("catServerPort");
    this.catBasePath = config().getString("dxCatalogueBasePath");
    this.catSearchPath = catBasePath + CAT_SEARCH_PATH;
    this.catalogueItemPath = config().getString("catRequestItemsUri");
    catalogueService = new CatalogueService(catWebClient, host, port,catBasePath,catalogueItemPath, catSearchPath);

    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(CATALOGUE_SERVICE_ADDRESS).register(CatalogueInterface.class, catalogueService);
    LOGGER.info("Catalogue verticle started.");

  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}