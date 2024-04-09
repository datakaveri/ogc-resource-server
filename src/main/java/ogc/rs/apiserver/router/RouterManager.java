package ogc.rs.apiserver.router;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.Validator;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.pubsub.PgSubscriber;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.router.gisentities.GisEntityInterface;
import ogc.rs.apiserver.router.routerbuilders.DxMeteringRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.OgcRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.StacRouterBuilder;
import ogc.rs.apiserver.router.util.OasFragments;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static ogc.rs.common.Constants.*;

/**
 * Handles OpenAPI spec generation for the OGC and STAC OpenAPI specs and creation of the routers
 * based on the spec. <br>
 * Both operations can be triggered by any component in the system by calling
 * {@link RouterManager#TRIGGER_SPEC_UPDATE_AND_ROUTER_REGEN_SQL} and executing the given string as
 * a query.
 * 
 */
public class RouterManager {

  private static final String SPEC_AND_ROUTER_UPDATE_PG_CHANNEL = "update_spec_and_routes_now";

  /**
   * Get an SQL query to force a spec update and router. param can be any string for now.
   */
  public static final Function<String, String> TRIGGER_SPEC_UPDATE_AND_ROUTER_REGEN_SQL =
      (param) -> "NOTIFY '" + SPEC_AND_ROUTER_UPDATE_PG_CHANNEL + "', '" + param + "'";

  private static final String SPEC_UPDATE_LOCK = "SPEC_UPDATE_LOCK";

  private static final String OGC_OAS_TEMPLATE_PATH = "docs/openapiv3_0.json";
  public static final String OGC_OAS_REAL_PATH =
      System.getProperty("java.io.tmpdir") + "/ogcSpec.json";
  private static final String STAC_OAS_TEMPLATE_PATH = "docs/stacopenapiv3_0.json";
  public static final String STAC_OAS_REAL_PATH =
      System.getProperty("java.io.tmpdir") + "/stacSpec.json";

  private static final Logger LOGGER = LogManager.getLogger(RouterManager.class);

  private Vertx vertx;
  private JsonObject config;
  private List<ApiServerVerticle> apiServerVerticleInstances = new ArrayList<ApiServerVerticle>();
  private Validator openApiValidator;

  public RouterManager(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;

    JsonSchema openApiSchema =
        JsonSchema.of(vertx.fileSystem().readFileBlocking("openApiJsonSchema.json").toJsonObject());

    openApiValidator =
        Validator.create(openApiSchema, new JsonSchemaOptions().setBaseUri("https://example.com"));

    setupPgNotifyChannel(config);

    int numberOfApiInstances = config.getInteger("verticleInstances", 1);
    waitForApiServersToRegisterThenInitRouter(numberOfApiInstances);
  }

  /**
   * Method to wait till all instances of {@link ApiServerVerticle} have registered with the
   * {@link RouterManager} using {@link RouterManager#registerApiServer(ApiServerVerticle)}. Once
   * all instances have registered, the {@link RouterManager#genOasAndInitRouterFirstTime()} method
   * is called.
   * 
   * @param instances the number of {@link ApiServerVerticle} instances configured to start
   */
  private void waitForApiServersToRegisterThenInitRouter(int instances) {
    vertx.setPeriodic(250, i -> {
      if (apiServerVerticleInstances.size() == instances) {
        LOGGER.debug("All ApiServerVerticle instances registered w/ RouterManager");
        vertx.cancelTimer(i);
        genOasAndInitRouterFirstTime();
      }
    });
  }

  /**
   * Allow {@link ApiServerVerticle} instances to register themselves with the
   * {@link RouterManager}. Access to the {@link ApiServerVerticle} object allows for creating the
   * routes in {@link StacRouterBuilder}, {@link OgcRouterBuilder} and
   * {@link DxMeteringRouterBuilder}, as well as refreshing the router using the
   * {@link ApiServerVerticle#resetRouter(Router, Router)} method.
   * 
   * @param instance an {@link ApiServerVerticle} instance
   */
  public void registerApiServer(ApiServerVerticle instance) {
    apiServerVerticleInstances.add(instance);
  }

  /**
   * Generates the OGC and STAC OpenAPI specs from the templates and initializes the routers for all
   * instances of {@link ApiServerVerticle} on startup.
   */
  private void genOasAndInitRouterFirstTime() {
    FileSystem fs = vertx.fileSystem();
    String ogcOasTemplateStr = fs.readFileBlocking(OGC_OAS_TEMPLATE_PATH).toString();
    String stacOasTemplateStr = fs.readFileBlocking(STAC_OAS_TEMPLATE_PATH).toString();
    
    /* Replace ${HOSTNAME} values in spec with actual hostname */
    ogcOasTemplateStr = ogcOasTemplateStr.replace("${HOSTNAME}", config.getString("hostName"));
    stacOasTemplateStr = stacOasTemplateStr.replace("${HOSTNAME}", config.getString("hostName"));
    
    JsonObject ogcOasTemplate = new JsonObject(ogcOasTemplateStr);
    JsonObject stacOasTemplate = new JsonObject(stacOasTemplateStr);

    DatabaseService dbService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    /* Load all implementations of the GisEntityInterface using SPI */
    ServiceLoader<GisEntityInterface> loader = ServiceLoader.load(GisEntityInterface.class);

    List<Future<OasFragments>> oasFragmentsListFut =
        loader.stream().map(entity -> entity.get().generateNewSpecFragments(ogcOasTemplate,
            stacOasTemplate, dbService, config)).collect(Collectors.toList());

    Future<Void> createSpecsAndSetRouters = Future.all(oasFragmentsListFut).compose(futuresResult -> {

      List<OasFragments> oasFragmentsList = futuresResult.list();

      return updateAndWriteSpecFiles(oasFragmentsList, ogcOasTemplate, stacOasTemplate)
          .compose(res -> buildAndUpdateRoutersInApiServInstances());
    });
        
    createSpecsAndSetRouters.onSuccess(succ -> {
      LOGGER.info(
          "Generated specs from templates and initialized routers in all ApiServerVerticles successfully");
    }).onFailure(err -> {
      LOGGER.fatal("Failed to generate specs and initialize routers {}", err.getMessage());
    });

    return;
  }

  /**
   * Refreshes the OGC and STAC OpenAPI specs and regenerates routers for all instances of
   * {@link ApiServerVerticle}.
   * 
   */
  private void refreshSpecsAndRegenRouters() {
    FileSystem fs = vertx.fileSystem();

    if (!fs.existsBlocking(OGC_OAS_REAL_PATH) || !fs.existsBlocking(STAC_OAS_REAL_PATH)) {
      LOGGER.error(
          "Cannot refresh OpenAPI spec and routers - generated OpenAPI spec files are not present at path");
      return;
    }

    JsonObject ogcOasFile = fs.readFileBlocking(OGC_OAS_REAL_PATH).toJsonObject();
    JsonObject stacOasFile = fs.readFileBlocking(STAC_OAS_REAL_PATH).toJsonObject();

    DatabaseService dbService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    /* Load all implementations of the GisEntityInterface using SPI */
    ServiceLoader<GisEntityInterface> loader = ServiceLoader.load(GisEntityInterface.class);

    List<Future<OasFragments>> oasFragmentsListFut = loader.stream().map(
        entity -> entity.get().generateNewSpecFragments(ogcOasFile, stacOasFile, dbService, config))
        .collect(Collectors.toList());

    Future.all(oasFragmentsListFut).compose(futuresResult -> {

      List<OasFragments> result = futuresResult.list();

      return updateExistingSpecFiles(result)
          .compose(res -> buildAndUpdateRoutersInApiServInstances());

    }).onSuccess(i -> {
      LOGGER.info("Refreshed specs and regenerated routers in all ApiServerVerticles successfully");
    }).onFailure(err -> {
      LOGGER.fatal("Failed to refresh specs and regenerate routers : {}", err.getMessage());
    });

    return;
  }

  /**
   * Set up Postgres LISTEN/NOTIFY channel to allow the {@link RouterManager} to be notified when a
   * router/spec update needs to be done.
   * 
   * @param config the server config to get Postgres info
   */
  private void setupPgNotifyChannel(JsonObject config) {

    String databaseIp = config.getString("databaseHost");
    Integer databasePort = config.getInteger("databasePort");
    String databaseName = config.getString("databaseName");
    String databaseUserName = config.getString("databaseUser");
    String databasePassword = config.getString("databasePassword");

    PgConnectOptions connectOptions = new PgConnectOptions().setPort(databasePort)
        .setHost(databaseIp).setDatabase(databaseName).setUser(databaseUserName)
        .setPassword(databasePassword).setReconnectAttempts(2).setReconnectInterval(1000L);

    PgSubscriber subl = PgSubscriber.subscriber(vertx, connectOptions);

    subl.channel(SPEC_AND_ROUTER_UPDATE_PG_CHANNEL).handler(payload -> {
      refreshSpecsAndRegenRouters();
    });

    subl.connect().onSuccess(succ -> {
      LOGGER.info(
          "Successfully connected to Postgres channel '{}' to trigger spec updates and router regenerations",
          SPEC_AND_ROUTER_UPDATE_PG_CHANNEL);
    }).onFailure(err -> {
      LOGGER.fatal(
          "Failed to connect to Postgres channel '{}'. RouterManager will not be able to trigger spec updates",
          SPEC_AND_ROUTER_UPDATE_PG_CHANNEL);
    });
  }

  /**
   * Build routers using generated OpenAPI specs and update routers in all {@link ApiServerVerticle}
   * instances.
   * 
   * @return
   */
  private Future<Void> buildAndUpdateRoutersInApiServInstances() {
    LOGGER.debug("Updating routers in all ApiServerVerticle instances");

    Promise<Void> promise = Promise.promise();

    List<Future<Void>> updateInstancesFut = apiServerVerticleInstances.stream().map(instance -> {

      Future<Router> ogc =
          OgcRouterBuilder.create(instance, vertx, config).map(OgcRouterBuilder::getRouter);
      Future<Router> stac =
          StacRouterBuilder.create(instance, vertx, config).map(StacRouterBuilder::getRouter);
      Future<Router> dxMetering = DxMeteringRouterBuilder.create(instance, vertx, config)
          .map(DxMeteringRouterBuilder::getRouter);

      Future<Void> updatedRouter = Future.all(ogc, stac, dxMetering).compose(res -> {
        instance.resetRouter(List.of(ogc.result(), stac.result(), dxMetering.result()));
        return Future.succeededFuture();
      });

      return updatedRouter;
    }).collect(Collectors.toList());

    Future.all(updateInstancesFut).onSuccess(succ -> {
      LOGGER.info("Reset sub-routers in all ApiServerVerticle instances successfully");
      promise.complete();
    }).onFailure(err -> {
      LOGGER.error("Failed to reset sub-routers in ApiServerVerticle instances : {}", err);
      promise.fail(err);
    });

    return promise.future();
  }

  /**
   * Validate OpenAPI spec JSON using JSON Schema.
   * 
   * @param spec the JSON spec
   * @return true if valid, else false
   */
  private boolean validateGeneratedSpec(JsonObject spec) {
    LOGGER.debug("Validating spec");

    OutputUnit op = openApiValidator.validate(spec);

    if (op.getValid()) {
      return true;
    }

    LOGGER.error("Spec validation failed : {}",
        op.getErrors().stream().map(OutputUnit::getError).collect(Collectors.joining(" ,")));
    return false;
  }

  /**
   * Update existing OpenAPI spec files. The spec files are read once again before updating and
   * writing back to the file system in case the spec was updated beforehand. The whole operation is
   * done using a lock to prevent race conditions.
   * 
   * @param fragments the list of {@link OasFragments}
   * @return
   */
  private Future<Void> updateExistingSpecFiles(List<OasFragments> fragments) {
    Promise<Void> promise = Promise.promise();

    Future<Lock> lock = vertx.sharedData().getLocalLock(SPEC_UPDATE_LOCK);

    lock.compose(res -> {
      LOGGER.debug("Got lock to read latest versions of both specs, update and write back");

      JsonObject ogcOasFile = vertx.fileSystem().readFileBlocking(OGC_OAS_REAL_PATH).toJsonObject();
      JsonObject stacOasFile =
          vertx.fileSystem().readFileBlocking(STAC_OAS_REAL_PATH).toJsonObject();

      return updateAndWriteSpecFiles(fragments, ogcOasFile, stacOasFile);

    }).onSuccess(succ -> {

      lock.result().release();
      LOGGER.info("Updated spec files successfully; (Released lock)");
      promise.complete();

    }).onFailure(err -> {

      if (lock.succeeded()) {
        LOGGER.error("Failed to update spec files; Released lock");
        lock.result().release();
      }
      promise.fail(err);
    });

    return promise.future();
  }

  /**
   * Modify given OGC and STAC spec files using the {@link OasFragments} list and write them to
   * {@link RouterManager#OGC_OAS_REAL_PATH} and {@link RouterManager#STAC_OAS_REAL_PATH}. The
   * modified spec JSON objects are also validated with JSON schema using
   * {@link RouterManager#validateGeneratedSpec(JsonObject)}
   * 
   * @param fragments the list of {@link OasFragments}
   * @param ogcSpec the OGC spec JSON
   * @param stacSpec the STAC spec JSON
   * @return
   */
  private Future<Void> updateAndWriteSpecFiles(List<OasFragments> fragments, JsonObject ogcSpec,
      JsonObject stacSpec) {
    LOGGER.info("Trying to update OpenAPI specs and write to filesystem");
    Promise<Void> promise = Promise.promise();

    fragments.forEach(res -> {
      List<JsonObject> ogcFrags = res.getOgc();
      List<JsonObject> stacFrags = res.getStac();

      ogcFrags.forEach(i -> ogcSpec.getJsonObject("paths").mergeIn(i));
      stacFrags.forEach(i -> stacSpec.getJsonObject("paths").mergeIn(i));
    });

    if (!validateGeneratedSpec(ogcSpec)) {
      LOGGER.error("Spec validation failed for OGC Spec");
      promise.fail("Did not write spec files - spec validation failed");
      return promise.future();
    }

    if (!validateGeneratedSpec(stacSpec)) {
      LOGGER.error("Spec validation failed for STAC Spec");
      promise.fail("Did not write spec files - spec validation failed");
      return promise.future();
    }

    Future<Void> writeOgcSpec = vertx.fileSystem().writeFile(OGC_OAS_REAL_PATH, ogcSpec.toBuffer());
    Future<Void> writeStacSpec =
        vertx.fileSystem().writeFile(STAC_OAS_REAL_PATH, stacSpec.toBuffer());

    Future.all(writeOgcSpec, writeStacSpec).onSuccess(succ -> {
      LOGGER.info("Spec files written successfully to {} and {}", OGC_OAS_REAL_PATH,
          STAC_OAS_REAL_PATH);

      promise.complete();

    }).onFailure(err -> {
      LOGGER.error("Failed to update spec and write spec to filesystem");
      promise.fail(err);
    });

    return promise.future();
  }
}
