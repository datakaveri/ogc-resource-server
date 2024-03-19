package ogc.rs.processes;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class implements the process of onboarding a collection.
 */
public class CollectionOnboardingProcess implements ProcessService {

  private static final Logger LOGGER = LogManager.getLogger(CollectionOnboardingProcess.class);
  private final PgPool pgPool;
  private final WebClient webClient;
  private final UtilClass utilClass;
  private String awsEndPoint;
  private String accessKey;
  private String secretKey;
  private String awsBucketUrl;
  private String catServerHost;
  private String catRequestUri;
  private int catServerPort;
  private String databaseName;
  private String databaseHost;
  private String databasePassword;
  private String databaseUser;
  private int databasePort;

  public CollectionOnboardingProcess(PgPool pgPool, WebClient webClient, JsonObject config) {
    this.pgPool = pgPool;
    this.webClient = webClient;
    this.utilClass = new UtilClass(pgPool);
    initializeConfig(config);
  }

  /**
   * Initializes the configuration parameters based on the given JSON object.
   *
   * @param config the JSON object containing the configuration parameters
   */
  private void initializeConfig(JsonObject config) {
    this.awsEndPoint = config.getString("awsEndPoint");
    this.accessKey = config.getString("awsAccessKey");
    this.secretKey = config.getString("awsSecretKey");
    this.awsBucketUrl = config.getString("s3BucketUrl");
    this.catRequestUri = config.getString("catRequestItemsUri");
    this.catServerHost = config.getString("catServerHost");
    this.catServerPort = config.getInteger("catServerPort");
    this.databaseName = config.getString("databaseName");
    this.databaseHost = config.getString("databaseHost");
    this.databasePassword = config.getString("databasePassword");
    this.databaseUser = config.getString("databaseUser");
    this.databasePort = config.getInteger("databasePort");
  }

  @Override
  public Future<JsonObject> execute(JsonObject requestInput) {
    Promise<JsonObject> objectPromise = Promise.promise();

    requestInput.put("progress", calculateProgress(1, 5));

    utilClass.updateJobTableStatus(requestInput, Status.RUNNING)
      .compose(updateTableResult -> makeCatApiRequest(requestInput)).compose(
        catResponse -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(2, 5))))
      .compose(updateTableResult1 -> checkIfCollectionPresent(requestInput)).compose(
        checkCollectionTableHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(3, 5))))
      .compose(p -> checkForCrs(requestInput))
      .compose(
        checkCollectionTableHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(4, 5))))
      .compose(progressUpdateHandler -> onboardingCollections(requestInput))
      .compose(onboardingCollectionsHandler -> checkDbAndTable(requestInput))
      .compose(onboardingSuccess -> utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL))
      .onSuccess(responseFromCat -> {
        LOGGER.info("ONBOARDING DONE");
        objectPromise.complete();
      }).onFailure(failure -> {
        LOGGER.error("ONBOARDING FAILURE");
        handleFailure(requestInput, failure.getMessage(), objectPromise);
      });
    return objectPromise.future();
  }

  /**
   * Makes a request to the catalog to check if the resource ID is present.
   *
   * @param requestInput the JSON object containing the resource ID
   * @return a future that completes with the request input if the resource ID is present in the catalog, or fails with an error message if the resource ID is not present in the catalog
   */
  private Future<JsonObject> makeCatApiRequest(JsonObject requestInput) {
    Promise<JsonObject> promise = Promise.promise();
    // to check if the resource id is present in cat
    webClient.get(catServerPort, catServerHost, catRequestUri)
      .addQueryParam("id", requestInput.getString("resourceId")).send()
      .onSuccess(responseFromCat -> {
        if (responseFromCat.statusCode() == 200) {
          // check if the provider is the same as the token decoded user
          requestInput.put("provider",
            responseFromCat.bodyAsJsonObject().getJsonArray("results").getJsonObject(0)
              .getString("owner"));
          promise.complete(requestInput);
        } else {
          promise.fail("Item not present in catalog");
        }
      }).onFailure(failureResponseFromCat -> promise.fail("failed to get response from catalog"));
    return promise.future();
  }

  /**
   * Checks if the collection with the given resource ID is already present in the database.
   *
   * @param jsonObject the JSON object containing the resource ID
   * @return a future that completes when the check is complete when the resource is not present in the database
   */
  private Future<Void> checkIfCollectionPresent(JsonObject jsonObject) {
    Promise<Void> promise = Promise.promise();
    pgPool.withConnection(sqlConnection -> sqlConnection.preparedQuery(
        "select id,title from collections_details where id=$1::uuid;")
      .execute(Tuple.of((jsonObject.getString("resourceId")))).onSuccess(successHandler -> {
        LOGGER.info("got success in collection table " + successHandler.size());
        //  TODO: change the if condition to > 0 for testing
        if (successHandler.size() == 0) {
          promise.complete();
        } else {
          promise.fail("Collection already present");
        }
      }).onFailure(failureHandler -> {
        LOGGER.error("failure handler " + failureHandler.toString());
        promise.fail("failed " + failureHandler.getCause());
      }));
    return promise.future();
  }

  /**
   * This method is used to extract the feature properties from the command line output.
   *
   * @param input the command line output
   * @return the feature properties
   */
  private Future<JsonObject> checkForCrs(JsonObject input) {
    CommandLine cmdLine = getOrgInfoCommandLine(input);
    DefaultExecutor defaultExecutor = DefaultExecutor.builder().get();
    defaultExecutor.setExitValue(0);
    ExecuteWatchdog watchdog =
      ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(10000)).get();
    defaultExecutor.setWatchdog(watchdog);
    Promise<JsonObject> onboardingPromise = Promise.promise();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    PumpStreamHandler psh = new PumpStreamHandler(stdout, stderr);
    defaultExecutor.setStreamHandler(psh);

    try {
      int exitValue = defaultExecutor.execute(cmdLine);
      LOGGER.info("exit value: " + exitValue);
      String output = stdout.toString();

      JsonObject cmdOutput =
        new JsonObject(Buffer.buffer(output.replace("Had to open data source read-only.", "")));
      JsonObject featureProperties = extractFeatureProperties(cmdOutput);

      String organization = featureProperties.getString("organization");
      int organizationCoordId = featureProperties.getInteger("organization_coordsys_id");
      LOGGER.info("organization " + organization + " id " + organizationCoordId);

      fetchCrsFromDatabase(organizationCoordId, input, onboardingPromise);
    } catch (IOException e1) {
      LOGGER.error("error while onboarding the collection " + e1.getMessage());
      onboardingPromise.fail("failed to do so " + e1.getMessage());
    }
    return onboardingPromise.future();
  }

  private JsonObject extractFeatureProperties(JsonObject cmdOutput) {
    LOGGER.info("COMND output " + cmdOutput);
    JsonObject layers = cmdOutput.getJsonArray("layers",new JsonArray().add(new JsonObject())).getJsonObject(0);
    JsonArray features = layers.getJsonArray("features",new JsonArray().add(new JsonObject().put("properties",new JsonObject())));
//    LOGGER.info("OGR INFO "+features);
    return features.getJsonObject(0).getJsonObject("properties");
  }
  private void fetchCrsFromDatabase(int organizationCoordId, JsonObject input,
                                    Promise<JsonObject> onboardingPromise) {
    pgPool.withConnection(
      sqlConnection -> sqlConnection.preparedQuery("SELECT crs FROM CRS_TO_SRID WHERE SRID = $1;")
        .execute(Tuple.of(organizationCoordId)).onSuccess(rows -> {
          if (!rows.iterator().hasNext()) {
            LOGGER.error("Failed to fetch CRS: No rows found.");
            onboardingPromise.fail("Failed to fetch CRS: No rows found.");
            return;
          }
          String crs = rows.iterator().next().getString("crs");
          input.put("crs", crs);
          onboardingPromise.complete(input);
        }).onFailure(failureHandler -> {
          LOGGER.error("Failed to fetch CRS from table: " + failureHandler.getMessage());
          onboardingPromise.fail("Failed to fetch CRS from table: " + failureHandler.getMessage());
        }));
  }

  private CommandLine getOrgInfoCommandLine(JsonObject input) {
    LOGGER.info("inside command line");

    String filename = input.getString("fileName");
    CommandLine ogrinfo = new CommandLine("ogrinfo");

    ogrinfo.addArgument("--config");
    ogrinfo.addArgument("CPL_VSIL_USE_TEMP_FILE_FOR_RANDOM_WRITE");
    ogrinfo.addArgument("NO");
    ogrinfo.addArgument("--config");
    ogrinfo.addArgument("AWS_S3_ENDPOINT");
    ogrinfo.addArgument(awsEndPoint);
    ogrinfo.addArgument("--config");
    ogrinfo.addArgument("AWS_ACCESS_KEY_ID");
    ogrinfo.addArgument(accessKey);
    ogrinfo.addArgument("--config");
    ogrinfo.addArgument("AWS_SECRET_ACCESS_KEY");
    ogrinfo.addArgument(secretKey);
    ogrinfo.addArgument("-json");
    ogrinfo.addArgument("-features");
//    ogrinfo.addArgument(String.format("/vsis3/%s%s",awsBucketUrl,filename));

    ogrinfo.addArgument(String.format("/home/gopal/Downloads/geopackages/%s",filename));
    ogrinfo.addArgument("-sql");
    ogrinfo.addArgument(
      "select organization,organization_coordsys_id from gpkg_contents join gpkg_spatial_ref_sys using(srs_id)",
      false);

    LOGGER.info("cmd " + ogrinfo);

    return ogrinfo;
  }

  private Future<JsonObject> onboardingCollections(JsonObject input) {
    String newItem = UUID.randomUUID().toString();
    input.put("jobTableId", newItem);

    CommandLine cmdLine = getCommandLineOgr2Ogr(input);
    DefaultExecutor defaultExecutor = DefaultExecutor.builder().get();
    defaultExecutor.setExitValue(0);
    ExecuteWatchdog watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofHours(1)).get();
    defaultExecutor.setWatchdog(watchdog);

    Promise<JsonObject> onboardingPromise = Promise.promise();

    try {
      int exitValue = defaultExecutor.execute(cmdLine);
      LOGGER.info("exit value: " + exitValue);
      onboardingPromise.complete(input);
    } catch (IOException e1) {
      LOGGER.error("error while onboarding the collection " + e1.getMessage());
      onboardingPromise.fail("failed to do so " + e1.getMessage());
    }
    return onboardingPromise.future();
  }

  private CommandLine getCommandLineOgr2Ogr(JsonObject input) {
    LOGGER.info("inside command line");

    String filename = input.getString("fileName");
    String title = input.getString("title");
    String description = input.getString("description");
    String newItem = input.getString("jobTableId");
    LOGGER.info("UUID OF TABLE " + input);
    String crs = input.getString("crs");
    String preludeSqlStmt = String.format(
      "BEGIN;INSERT INTO collections_details (id, title, description, crs,type) VALUES ('%s', '%s', '%s', '%s','%s'); INSERT INTO collection_supported_crs VALUES ('%s', '%s');",
      newItem, title, description, crs, "FEATURE",newItem,"gettheidfrmtble");
    String closingSqlStmt = "COMMIT";

    CommandLine cmdLine = new CommandLine("ogr2ogr");

    cmdLine.addArgument("-nln");
    cmdLine.addArgument(newItem);
    cmdLine.addArgument("-nlt");
    cmdLine.addArgument("PROMOTE_TO_MULTI");
    cmdLine.addArgument("-lco");
    cmdLine.addArgument("PRECISION=NO");
    cmdLine.addArgument("-lco");
    cmdLine.addArgument("LAUNDER=NO");
    cmdLine.addArgument("-lco");
    cmdLine.addArgument("GEOMETRY_NAME=geom");
    cmdLine.addArgument("-lco");
    cmdLine.addArgument("ENCODING=UTF-8");
    cmdLine.addArgument("-lco");
    cmdLine.addArgument("FID=id");
    cmdLine.addArgument("-f");
    cmdLine.addArgument("PostgreSQL");
    cmdLine.addArgument(
      String.format("PG:host=%s dbname=%s user=%s port=%d password=%s schemas=public", databaseHost,
        databaseName, databaseUser, databasePort, databasePassword), false);
    cmdLine.addArgument("-doo");
    cmdLine.addArgument("PRELUDE_STATEMENTS=" + preludeSqlStmt, false);
    cmdLine.addArgument("-doo");
    cmdLine.addArgument("CLOSING_STATEMENTS=" + closingSqlStmt, false);
    cmdLine.addArgument("-progress");
    cmdLine.addArgument("--debug");
    cmdLine.addArgument("ON");
    cmdLine.addArgument("-append");

    cmdLine.addArgument("--config");
    cmdLine.addArgument("AWS_S3_ENDPOINT");
    cmdLine.addArgument(awsEndPoint);
    cmdLine.addArgument("--config");
    cmdLine.addArgument("AWS_ACCESS_KEY_ID");
    cmdLine.addArgument(accessKey);
    cmdLine.addArgument("--config");
    cmdLine.addArgument("AWS_SECRET_ACCESS_KEY");
    cmdLine.addArgument(secretKey);
    cmdLine.addArgument(String.format("/vsis3/%s%s", awsBucketUrl, filename));
    LOGGER.info("CMD OGR2OGR " + cmdLine);

    return cmdLine;
  }

  /**
   * Checks if the collection with the given resource ID is already present in the database and also checks if the table with name resource ID is present.
   *
   * @param requestJson the JSON object containing the resource ID
   * @return a future that completes when the check is complete.
   */
  private Future<RowSet<Row>> checkDbAndTable(JsonObject requestJson) {
    String newItem = requestJson.getString("jobTableId");
    LOGGER.info("REQEUST " + requestJson);
    return pgPool.getConnection().compose(
      conn -> conn.preparedQuery("SELECT * FROM collections_details where id=$1::UUID;")
        .execute(Tuple.of(newItem)).compose(res -> {
          if (res.size() > 0) {
            LOGGER.info("Collection found in collections_detail");
            return conn.preparedQuery(
                "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = $1) AS table_existence;")
              .execute(Tuple.of(newItem)).map(existsResult -> {
                if (existsResult.iterator().next().getBoolean("table_existence")) {
                  LOGGER.info("Table Exist with name " + newItem);
                  return existsResult;
                } else {
                  LOGGER.error("Table does not exist.");
                  throw new RuntimeException("Table does not exist");
                }
              }).onComplete(ar -> conn.close());
          } else {
            conn.close();
            LOGGER.error("collection not present in collections_table");
            return Future.failedFuture("collection not present in collections_table");
          }
        }));
  }

  private void handleFailure(JsonObject input, String errorMessage,
                             Promise<JsonObject> itemDetails) {
    LOGGER.error("Inside failure handler because {}", errorMessage);
    utilClass.updateJobTableStatus(input, Status.FAILED).onSuccess(l -> {
      LOGGER.info("success");
      itemDetails.fail("Failed to onboard collection because " + errorMessage);
    }).onFailure(jobTableUpdateFailure -> {
      LOGGER.error("failure");
      itemDetails.fail("Failed to update fail status because " + jobTableUpdateFailure.getCause());
    });

  }

  private float calculateProgress(int currentStep, int totalSteps) {
    return ((float) currentStep / totalSteps) * 100;
  }


}
