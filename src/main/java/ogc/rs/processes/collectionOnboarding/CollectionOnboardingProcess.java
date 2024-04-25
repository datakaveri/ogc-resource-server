package ogc.rs.processes.collectionOnboarding;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.router.RouterManager;
import ogc.rs.common.DataFromS3;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;

import static ogc.rs.processes.collectionOnboarding.Constants.*;


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
  private final boolean VERTX_EXECUTE_BLOCKING_IN_ORDER = false;
  private DataFromS3 dataFromS3;
  private Vertx vertx;
  public CollectionOnboardingProcess(PgPool pgPool, WebClient webClient, JsonObject config,DataFromS3 dataFromS3,Vertx vertx) {
    this.pgPool = pgPool;
    this.webClient = webClient;
    this.utilClass = new UtilClass(pgPool);
    this.vertx=vertx;
    this.dataFromS3=dataFromS3;
    initializeConfig(config);
  }

  /**
   * Initializes the configuration parameters based on the given JSON object.
   *
   * @param config the JSON object containing the configuration parameters
   */
  private void initializeConfig(JsonObject config) {
    this.awsEndPoint = "s3.".concat(config.getString("awsRegion")).concat(".amazonaws.com");
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

    requestInput.put("progress", calculateProgress(1, 7));

    String tableID = requestInput.getString("resourceId");
    requestInput.put("collectionsDetailsTableId", tableID);

    utilClass.updateJobTableStatus(requestInput, Status.RUNNING,CHECK_CAT_FOR_RESOURCE_REQUEST)
      .compose(progressUpdateHandler -> makeCatApiRequest(requestInput)).compose(
        catResponseHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(2, 7)).put(MESSAGE,CAT_REQUEST_RESPONSE)))
      .compose(progressUpdateHandler -> checkIfCollectionPresent(requestInput)).compose(
        checkCollectionTableHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(3, 7)).put(MESSAGE,COLLECTION_RESPONSE)))
            .compose(progressUpdateHandler -> checkForCrs(requestInput)).compose(
        checkCollectionTableHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(4, 7)).put(MESSAGE,CRS_RESPONSE)))
            .compose(progressHandler->getMetaDataFromS3(requestInput)).compose(
                    checkCollectionTableHandler -> utilClass.updateJobTableProgress(
                            requestInput.put("progress", calculateProgress(5, 7)).put(MESSAGE,S3_RESPONSE)))
            .compose(progressUpdateHandler -> onboardingCollection(requestInput)).compose(
        onboardingCollectionHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(6, 7)).put(MESSAGE,ONBOARDING_RESPONSE)))
      .compose(progressUpdateHandler -> checkDbAndTable(requestInput))
      .compose(checkDbHandler -> utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL,DB_CHECK_RESPONSE))
      .onSuccess(onboardingSuccessHandler -> {
        LOGGER.debug("COLLECTION ONBOARDING DONE");
        objectPromise.complete();
      }).onFailure(onboardingFailureHandler -> {
        LOGGER.error("COLLECTION ONBOARDING FAILED ");
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
          requestInput.put("accessPolicy",
            responseFromCat.bodyAsJsonObject().getJsonArray("results").getJsonObject(0)
              .getString("accessPolicy"));
          promise.complete(requestInput);
        } else {
          LOGGER.error("Item not present in catalogue");
          promise.fail("Item not present in catalogue");
        }
      }).onFailure(failureResponseFromCat -> {
        LOGGER.error("Failed to get response from catalogue " + failureResponseFromCat.getMessage());
        promise.fail("Failed to get response from Catalogue.");
      });
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
          LOGGER.error("Failed to check collection in db {}", failureHandler.getMessage());
          promise.fail("Failed to check collection existence in db.");
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

    Promise<JsonObject> promise = Promise.promise();

    vertx
        .<JsonObject>executeBlocking(
            crsPromise -> {
              LOGGER.debug("Trying ogrInfo");
              CommandLine cmdLine = getOrgInfoCommandLine(input);
              DefaultExecutor defaultExecutor = DefaultExecutor.builder().get();
              defaultExecutor.setExitValue(0);
              ExecuteWatchdog watchdog =
                  ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(10000)).get();
              defaultExecutor.setWatchdog(watchdog);
              ByteArrayOutputStream stdout = new ByteArrayOutputStream();
              ByteArrayOutputStream stderr = new ByteArrayOutputStream();
              PumpStreamHandler psh = new PumpStreamHandler(stdout, stderr);
              defaultExecutor.setStreamHandler(psh);

              try {
                int exitValue = defaultExecutor.execute(cmdLine);
                LOGGER.debug("exit value in ogrInfo: " + exitValue);
                String output = stdout.toString();

                // Extracting JSON object from string 'output', removing the initial message "Had to
                // open data source read-only."
                // to retrieve the necessary data for code flow.
                JsonObject cmdOutput =
                    new JsonObject(
                        Buffer.buffer(output.replace("Had to open data source read-only.", "")));
                crsPromise.complete(cmdOutput);
              } catch (IOException e1) {
                crsPromise.fail(e1.getMessage());
              }
            },VERTX_EXECUTE_BLOCKING_IN_ORDER)
        .onSuccess(
            cmdOutput -> {
              JsonObject featureProperties = extractFeatureProperties(cmdOutput);
              String organization = featureProperties.getString("organization");
              if (!organization.equals("EPSG")) {
                LOGGER.error("CRS not present as EPSG");
                promise.fail("CRS not present as EPSG");
                return;
              }
              int organizationCoOrdId = featureProperties.getInteger("organization_coordsys_id");
              LOGGER.debug("organization " + organization + " crs " + organizationCoOrdId);
              validSridFromDatabase(organizationCoOrdId, input, promise);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failed in ogrInfo because {}", failureHandler.getMessage());
              promise.fail("Failed in ogrInfo.");
            });
    return promise.future();
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
          onboardingPromise.fail("Failed to fetch CRS.");
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
        onboardingPromise.fail("Failed to fetch CRS from table.");
      }));
  }
  /**
   This method, getOrgInfoCommandLine, is utilized for generating a command line to execute the ogrinfo command.
   The ogrinfo command serves the purpose of extracting feature properties from the command line output,
   which in turn facilitates the retrieval of properties.
   **/
  private CommandLine getOrgInfoCommandLine(JsonObject input) {
    LOGGER.debug("inside command line");

    String filename = "/"+input.getString("fileName");
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

    Promise<JsonObject> promise = Promise.promise();
    vertx.<JsonObject>executeBlocking(onboardingPromise -> {

      LOGGER.debug("Trying to onboard the collection");
      CommandLine cmdLine = getCommandLineOgr2Ogr(input);
      DefaultExecutor defaultExecutor = DefaultExecutor.builder().get();
      defaultExecutor.setExitValue(0);
      ExecuteWatchdog watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofHours(1)).get();
      defaultExecutor.setWatchdog(watchdog);

      try {
        int exitValue = defaultExecutor.execute(cmdLine);
        input.put("exitValue", exitValue);
        LOGGER.debug("OGR2OGR cmd executed successfully.");
        onboardingPromise.complete(input);
      } catch (IOException e1) {
        LOGGER.error("Failed to onboard the collection because {}", e1.getMessage());
        onboardingPromise.fail("Failed to onboard the collection in OGR2OGR.");
      }},VERTX_EXECUTE_BLOCKING_IN_ORDER).onSuccess(promise::complete).onFailure(promise::fail);
    return promise.future();
  }

  /**
   * Asynchronously performs the onboarding process for a collection based on the provided input parameters.
   *
   * @param input The JsonObject containing parameters required for the collection onboarding process.
   * @return A Future<Void> representing the completion of the onboarding process.
   */
  private Future<Void> onboardingCollection(JsonObject input) {
    Promise<Void> promise = Promise.promise();
    LOGGER.debug("Starting Pre-onboarding of collection");

    int srid = input.getInteger("srid");
    BigInteger fileSize = (BigInteger) input.getValue("fileSize");
    String fileName = input.getString("fileName");
    String title = input.getString("title");
    String description = input.getString("description");
    String collectionsDetailsTableName = input.getString("collectionsDetailsTableId");
    String userId = input.getString("userId");
    String role = input.getString("role").toUpperCase();
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
        rgDetailsResult -> sqlClient.preparedQuery(RI_DETAILS_INSERT_QUERY)
          .execute(Tuple.of(resourceId, userId, accessPolicy))).compose(
        riDetailsResult -> sqlClient.preparedQuery(STAC_COLLECTION_ASSETS_INSERT_QUERY).execute(
          Tuple.of(collectionsDetailsTableName, title, fileName, COLLECTION_TYPE, fileSize,
            COLLECTION_ROLE))).compose(stacCollectionResult -> ogr2ogrCmd(input))
      .compose(onBoardingSuccess -> sqlClient.query(grantQuery).execute())
      .onSuccess(grantQueryResult -> {
        LOGGER.debug("Collection onboarded successfully ");
        promise.complete();
      }).onFailure(failure -> {
        String message = Objects.equals(failure.getMessage(), "Failed to onboard the collection in OGR2OGR") ? failure.getMessage() :  "Failed to onboard the collection in db.";
        LOGGER.error("Failed to onboard the collection because {} ",failure.getMessage());
        promise.fail(message);
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

    String filename = "/"+input.getString("fileName");
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
    cmdLine.addArgument("--config");
    cmdLine.addArgument("PG_USE_COPY");
    cmdLine.addArgument("NO");
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
   * Checks if the collection with the given resource ID is already present in the database and also
   * checks if the table with name resource ID is present.
   *
   * @param requestJson the JSON object containing the resource ID
   * @return a future that completes when the check is complete.
   */
  private Future<Void> checkDbAndTable(JsonObject requestJson) {
    LOGGER.debug("Checking collection in database.");
    String collectionsDetailsTableName = requestJson.getString("collectionsDetailsTableId");
    Promise<Void> promise= Promise.promise();
    pgPool
        .getConnection()
        .compose(
            conn ->
                conn.preparedQuery(COLLECTIONS_DETAILS_SELECT_QUERY)
                    .execute(Tuple.of(collectionsDetailsTableName))
                    .compose(
                        res -> {
                          if (res.size() > 0) {
                            LOGGER.debug("Collection found in collections_detail");
                            return conn.preparedQuery(COLLECTIONS_DETAILS_TABLE_EXIST_QUERY)
                                .execute(Tuple.of(collectionsDetailsTableName))
                                .compose(
                                    existsResult -> {
                                      if (existsResult
                                          .iterator()
                                          .next()
                                          .getBoolean("table_existence")) {
                                        LOGGER.debug(
                                            "Table Exist with name " + collectionsDetailsTableName);
                                        return conn.preparedQuery(
                                                RouterManager
                                                    .TRIGGER_SPEC_UPDATE_AND_ROUTER_REGEN_SQL
                                                    .apply("demo"))
                                            .execute();
                                      } else {
                                        LOGGER.error("Table does not exist.");
                                        throw new RuntimeException("Table does not exist.");
                                      }
                                    })
                                .onSuccess(
                                    ar -> {
                                      conn.close();
                                      promise.complete();
                                    });
                          } else {
                            conn.close();
                            LOGGER.error("Collection not present in collections_details");
                            return Future.failedFuture(
                                "Collection not present in collections_details");
                          }
                        })
                    .onFailure(
                        failureHandler -> {
                          LOGGER.error(
                              "Failed to confirm collection in db " + failureHandler.getMessage());
                          promise.fail(failureHandler.getMessage());
                        }));
     return promise.future();
  }
  /**
   * This method is used to get meta data of the collection stored in S3 bucket.
   *
   * @param requestInput the JSON object containing the file name.
   * @return a future that completes after getting metadata from S3.
   */
  private Future<JsonObject> getMetaDataFromS3(JsonObject requestInput) {
    Promise<JsonObject> promise = Promise.promise();
    String fileName = requestInput.getString("fileName");
    String urlString =
            dataFromS3.getFullyQualifiedProcessUrlString(fileName);
    dataFromS3.setUrlFromString(urlString);
    dataFromS3.setSignatureHeader(HttpMethod.HEAD);
    dataFromS3
        .getDataFromS3(HttpMethod.HEAD)
        .onSuccess(
            responseFromS3 -> {
              requestInput.put("fileSize",new BigInteger(responseFromS3.getHeader("Content-Length")));
              promise.complete(requestInput);
            })
        .onFailure(
            failed -> {
              LOGGER.error("Failed to get response from S3 " + failed.getLocalizedMessage());
              promise.fail(failed.getMessage());
            });
    return promise.future();
  }
  private void handleFailure(JsonObject input, String errorMessage,
                             Promise<JsonObject> itemDetails) {
    utilClass.updateJobTableStatus(input, Status.FAILED,errorMessage).onSuccess(l -> {
      itemDetails.fail("Failed to onboard collection because " + errorMessage);
    }).onFailure(jobTableUpdateFailure -> {
      itemDetails.fail("Failed to update fail status because " + jobTableUpdateFailure.getCause());
    });

  }

  private float calculateProgress(int currentStep, int totalSteps) {
    return ((float) currentStep / totalSteps) * 100;
  }


}
