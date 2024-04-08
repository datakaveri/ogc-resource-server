package ogc.rs.processes;

import static ogc.rs.processes.util.Constants.COLLECTIONS_DETAILS_INSERT_QUERY;
import static ogc.rs.processes.util.Constants.COLLECTIONS_DETAILS_SELECT_QUERY;
import static ogc.rs.processes.util.Constants.COLLECTIONS_DETAILS_TABLE_EXIST_QUERY;
import static ogc.rs.processes.util.Constants.COLLECTION_ROLE;
import static ogc.rs.processes.util.Constants.COLLECTION_SUPPORTED_CRS_INSERT_QUERY;
import static ogc.rs.processes.util.Constants.COLLECTION_TYPE;
import static ogc.rs.processes.util.Constants.CRS_TO_SRID_SELECT_QUERY;
import static ogc.rs.processes.util.Constants.DEFAULT_SERVER_CRS;
import static ogc.rs.processes.util.Constants.FEATURE;
import static ogc.rs.processes.util.Constants.FILE_SIZE;
import static ogc.rs.processes.util.Constants.GRANT_QUERY;
import static ogc.rs.processes.util.Constants.RG_DETAILS_INSERT_QUERY;
import static ogc.rs.processes.util.Constants.RI_DETAILS_INSERT_QUERY;
import static ogc.rs.processes.util.Constants.ROLES_INSERT_QUERY;
import static ogc.rs.processes.util.Constants.SECURE_ACCESS_KEY;
import static ogc.rs.processes.util.Constants.STAC_COLLECTION_ASSETS_INSERT_QUERY;

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

    requestInput.put("progress", calculateProgress(1, 6));

    String tableID = requestInput.getString("resourceId");
    requestInput.put("collectionsDetailsTableId", tableID);

    utilClass.updateJobTableStatus(requestInput, Status.RUNNING)
      .compose(updateTableResult -> makeCatApiRequest(requestInput)).compose(
        catResponseHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(2, 6))))
      .compose(updateTableResult1 -> checkIfCollectionPresent(requestInput)).compose(
        checkCollectionTableHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(3, 6))))
      .compose(updateTableResult2 -> checkForCrs(requestInput)).compose(
        checkCollectionTableHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(4, 6))))
      .compose(progressUpdateHandler -> onboardingCollection(requestInput)).compose(
        onboardingCollectionHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(5, 6))))
      .compose(progressUpdateHandler -> checkDbAndTable(requestInput))
      .compose(checkDbHandler -> utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL))
      .onSuccess(onboardingSuccessHandler -> {
        LOGGER.debug("ONBOARDING DONE");
        objectPromise.complete();
      }).onFailure(onboardingFailureHandler -> {
        LOGGER.error("ONBOARDING FAILURE");
        handleFailure(requestInput, onboardingFailureHandler.getMessage(), objectPromise);
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
    webClient.get(catServerPort, catServerHost, catRequestUri)
      .addQueryParam("id", requestInput.getString("resourceId")).send()
      .onSuccess(responseFromCat -> {
        if (responseFromCat.statusCode() == 200) {
          requestInput.put("owner",
            responseFromCat.bodyAsJsonObject().getJsonArray("results").getJsonObject(0)
              .getString("ownerUserId"));
          if (!(requestInput.getValue("owner").toString().equals(requestInput.getValue("userId").toString()))) {
            LOGGER.error("Resource does not belong to the user.");
            promise.fail("Resource does not belong to the user.");
            return;
          }
          requestInput.put("resourceGroup",
            responseFromCat.bodyAsJsonObject().getJsonArray("results").getJsonObject(0)
              .getString("resourceGroup"));
          requestInput.put("accessPolicy",
            responseFromCat.bodyAsJsonObject().getJsonArray("results").getJsonObject(0)
              .getString("accessPolicy"));
          promise.complete(requestInput);
        } else {
          promise.fail("Item not present in catalog");
        }
      }).onFailure(failureResponseFromCat -> promise.fail(
        "Failed to get response from catalog " + failureResponseFromCat.getMessage()));
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
    pgPool.withConnection(
      sqlConnection -> sqlConnection.preparedQuery(COLLECTIONS_DETAILS_SELECT_QUERY)
        .execute(Tuple.of((jsonObject.getString("collectionsDetailsTableId"))))
        .onSuccess(successHandler -> {
          if (successHandler.size() == 0) {
            promise.complete();
          } else {
            LOGGER.error("Collection already present.");
            promise.fail("Collection already present.");
          }
        }).onFailure(failureHandler -> {
          LOGGER.error("failed to check collection in db {}", failureHandler.getMessage());
          promise.fail("failed to check collection in db " + failureHandler.getMessage());
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
      LOGGER.debug("exit value in ogrinfo: " + exitValue);
      String output = stdout.toString();

      // Extracting JSON object from string 'output', removing the initial message "Had to open data source read-only."
      // to retrieve the necessary data for code flow.
      JsonObject cmdOutput =
        new JsonObject(Buffer.buffer(output.replace("Had to open data source read-only.", "")));
      JsonObject featureProperties = extractFeatureProperties(cmdOutput);
      String organization = featureProperties.getString("organization");
      if (!organization.equals("EPSG")) {
        LOGGER.error("CRS not present as EPSG");
        onboardingPromise.fail("CRS not present as EPSG");
        return onboardingPromise.future();
      }
      int organizationCoordId = featureProperties.getInteger("organization_coordsys_id");
      LOGGER.debug("organization " + organization + " crs " + organizationCoordId);
      validSridFromDatabase(organizationCoordId, input, onboardingPromise);
    } catch (IOException e1) {
      LOGGER.error("Failed while getting ogrInfo because {}", e1.getMessage());
      onboardingPromise.fail("Failed while getting ogrInfo because {} " + e1.getMessage());
    }
    return onboardingPromise.future();
  }

  private JsonObject extractFeatureProperties(JsonObject cmdOutput) {
    JsonObject layers =
      cmdOutput.getJsonArray("layers", new JsonArray().add(new JsonObject())).getJsonObject(0);
    JsonArray features = layers.getJsonArray("features",
      new JsonArray().add(new JsonObject().put("properties", new JsonObject())));
    return features.getJsonObject(0).getJsonObject("properties");
  }

  private void validSridFromDatabase(int organizationCoordId, JsonObject input,
                                     Promise<JsonObject> onboardingPromise) {
    pgPool.withConnection(sqlConnection -> sqlConnection.preparedQuery(CRS_TO_SRID_SELECT_QUERY)
      .execute(Tuple.of(organizationCoordId)).onSuccess(rows -> {
        if (!rows.iterator().hasNext()) {
          LOGGER.error("Failed to fetch CRS: No rows found.");
          onboardingPromise.fail("Failed to fetch CRS: No rows found.");
          return;
        }
        Row row = rows.iterator().next();
        String crs = row.getString("crs");
        int srid = row.getInteger("srid");
        input.put("crs", crs);
        input.put("srid", srid);
        onboardingPromise.complete(input);
      }).onFailure(failureHandler -> {
        LOGGER.error("Failed to fetch CRS from table: " + failureHandler.getMessage());
        onboardingPromise.fail("Failed to fetch CRS from table: " + failureHandler.getMessage());
      }));
  }
  /**
   This method, getOrgInfoCommandLine, is utilized for generating a command line to execute the ogrinfo command.
   The ogrinfo command serves the purpose of extracting feature properties from the command line output,
   which in turn facilitates the retrieval of properties.
   **/
  private CommandLine getOrgInfoCommandLine(JsonObject input) {
    LOGGER.debug("inside command line");

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
    ogrinfo.addArgument(String.format("/vsis3/%s%s", awsBucketUrl, filename));
    ogrinfo.addArgument("-sql");
    ogrinfo.addArgument(
      "select organization,organization_coordsys_id from gpkg_contents join gpkg_spatial_ref_sys using(srs_id)",
      false);
    return ogrinfo;
  }

  private Future<JsonObject> ogr2ogrCmd(JsonObject input) {

    LOGGER.debug("Trying to onboard the collection");
    CommandLine cmdLine = getCommandLineOgr2Ogr(input);
    DefaultExecutor defaultExecutor = DefaultExecutor.builder().get();
    defaultExecutor.setExitValue(0);
    ExecuteWatchdog watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofHours(1)).get();
    defaultExecutor.setWatchdog(watchdog);

    Promise<JsonObject> onboardingPromise = Promise.promise();

    try {
      int exitValue = defaultExecutor.execute(cmdLine);
      input.put("exitValue", exitValue);
      onboardingPromise.complete(input);
    } catch (IOException e1) {
      LOGGER.error("Failed to onboard the collection because {}", e1.getMessage());
      onboardingPromise.fail("Failed to onboard the collection because " + e1.getMessage());
    }
    return onboardingPromise.future();
  }

  /**
   * Asynchronously performs the onboarding process for a collection based on the provided input parameters.
   *
   * @param input The JsonObject containing parameters required for the collection onboarding process.
   * @return A Future<Void> representing the completion of the onboarding process.
   */
  private Future<Void> onboardingCollection(JsonObject input) {
    Promise<Void> promise = Promise.promise();

    LOGGER.debug("Pre-onboarding started");

    int srid = input.getInteger("srid");
    String fileName = input.getString("fileName");
    String title = input.getString("title");
    String description = input.getString("description");
    String collectionsDetailsTableName = input.getString("collectionsDetailsTableId");
    String userId = input.getString("userId");
    String role = input.getString("role").toUpperCase();
    String resourceGroupId = input.getString("resourceGroup");
    String resourceId = input.getString("resourceId");
    String accessPolicy = input.getString("accessPolicy");
    String grantQuery = GRANT_QUERY.replace("collections_details_id", collectionsDetailsTableName)
      .replace("databaseUser", databaseUser);

    pgPool.withTransaction(sqlClient -> sqlClient.preparedQuery(COLLECTIONS_DETAILS_INSERT_QUERY)
      .execute(
        Tuple.of(collectionsDetailsTableName, title, description, DEFAULT_SERVER_CRS, FEATURE))
      .compose(
        collectionsDetailsResult -> sqlClient.preparedQuery(COLLECTION_SUPPORTED_CRS_INSERT_QUERY)
          .execute(Tuple.of(collectionsDetailsTableName, srid))).compose(
        collectionResult -> sqlClient.preparedQuery(ROLES_INSERT_QUERY)
          .execute(Tuple.of(userId, role))).compose(
        rolesResult -> sqlClient.preparedQuery(RG_DETAILS_INSERT_QUERY)
          .execute(Tuple.of(resourceGroupId, userId, SECURE_ACCESS_KEY))).compose(
        rgDetailsResult -> sqlClient.preparedQuery(RI_DETAILS_INSERT_QUERY)
          .execute(Tuple.of(resourceId, resourceGroupId, accessPolicy))).compose(
        riDetailsResult -> sqlClient.preparedQuery(STAC_COLLECTION_ASSETS_INSERT_QUERY).execute(
          Tuple.of(collectionsDetailsTableName, title, fileName, COLLECTION_TYPE, FILE_SIZE,
            COLLECTION_ROLE))).compose(stacCollectionResult -> ogr2ogrCmd(input))
      .compose(onBoardingSuccess -> sqlClient.query(grantQuery).execute())
      .onSuccess(grantQueryResult -> {
        LOGGER.debug("Collection onboarded successfully ");
        promise.complete();
      }).onFailure(failure -> {
        LOGGER.error(failure.getMessage());
        promise.fail(failure.getMessage());
      }));
    return promise.future();
  }

  /**
   * Constructs a command line for executing the ogr2ogr command which reads a collection file from source and insert collection data into db.
   *
   * @param input The JsonObject containing parameters for the ogr2ogr command.
   * @return The constructed CommandLine object ready for execution.
   */
  private CommandLine getCommandLineOgr2Ogr(JsonObject input) {

    String filename = input.getString("fileName");
    String collectionsDetailsTableName = input.getString("collectionsDetailsTableId");

    CommandLine cmdLine = new CommandLine("ogr2ogr");

    cmdLine.addArgument("-nln");
    cmdLine.addArgument(collectionsDetailsTableName);
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
    cmdLine.addArgument("-t_srs");
    cmdLine.addArgument("EPSG:4326");
    cmdLine.addArgument("-f");
    cmdLine.addArgument("PostgreSQL");
    cmdLine.addArgument(
      String.format("PG:host=%s dbname=%s user=%s port=%d password=%s schemas=public", databaseHost,
        databaseName, databaseUser, databasePort, databasePassword), false);
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

    return cmdLine;
  }

  /**
   * Checks if the collection with the given resource ID is already present in the database and also checks if the table with name resource ID is present.
   *
   * @param requestJson the JSON object containing the resource ID
   * @return a future that completes when the check is complete.
   */
  private Future<RowSet<Row>> checkDbAndTable(JsonObject requestJson) {

    LOGGER.debug("Checking collection in database.");
    String collectionsDetailsTableName = requestJson.getString("collectionsDetailsTableId");

    return pgPool.getConnection().compose(
      conn -> conn.preparedQuery(COLLECTIONS_DETAILS_SELECT_QUERY)
        .execute(Tuple.of(collectionsDetailsTableName)).compose(res -> {
          if (res.size() > 0) {
            LOGGER.debug("Collection found in collections_detail");
            return conn.preparedQuery(COLLECTIONS_DETAILS_TABLE_EXIST_QUERY)
              .execute(Tuple.of(collectionsDetailsTableName)).map(existsResult -> {
                if (existsResult.iterator().next().getBoolean("table_existence")) {
                  LOGGER.debug("Table Exist with name " + collectionsDetailsTableName);
                  return existsResult;
                } else {
                  LOGGER.error("Table does not exist.");
                  throw new RuntimeException("Table does not exist ");
                }
              }).onComplete(ar -> conn.close());
          } else {
            conn.close();
            LOGGER.error("Collection not present in collections_details");
            return Future.failedFuture("Collection not present in collections_details");
          }
        }));
  }

  private void handleFailure(JsonObject input, String errorMessage,
                             Promise<JsonObject> itemDetails) {
    utilClass.updateJobTableStatus(input, Status.FAILED).onSuccess(l -> {
      itemDetails.fail("Failed to onboard collection because " + errorMessage);
    }).onFailure(jobTableUpdateFailure -> {
      itemDetails.fail("Failed to update fail status because " + jobTableUpdateFailure.getCause());
    });

  }

  private float calculateProgress(int currentStep, int totalSteps) {
    return ((float) currentStep / totalSteps) * 100;
  }


}
