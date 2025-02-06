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
import ogc.rs.common.S3Config;
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
import java.util.*;

import static ogc.rs.processes.collectionOnboarding.Constants.*;


/**
 * This class implements the process of onboarding a collection.
 */
public class CollectionOnboardingProcess implements ProcessService {

  private static final Logger LOGGER = LogManager.getLogger(CollectionOnboardingProcess.class);
  private final PgPool pgPool;
  private final WebClient webClient;
  private final UtilClass utilClass;
  private S3Config s3conf;
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

    this.s3conf = new S3Config.Builder()
        .endpoint(config.getString(S3Config.ENDPOINT_CONF_OP))
        .bucket(config.getString(S3Config.BUCKET_CONF_OP))
        .region(config.getString(S3Config.REGION_CONF_OP))
        .accessKey(config.getString(S3Config.ACCESS_KEY_CONF_OP))
        .secretKey(config.getString(S3Config.SECRET_KEY_CONF_OP))
        .pathBasedAccess(config.getBoolean(S3Config.PATH_BASED_ACC_CONF_OP))
        .build();

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

    requestInput.put("progress", calculateProgress(1, 8));

    String tableID = requestInput.getString("resourceId");
    requestInput.put("collectionsDetailsTableId", tableID);

    utilClass.updateJobTableStatus(requestInput, Status.RUNNING,CHECK_CAT_FOR_RESOURCE_REQUEST)
      .compose(progressUpdateHandler -> makeCatApiRequest(requestInput)).compose(
        catResponseHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(2, 8)).put(MESSAGE,CAT_REQUEST_RESPONSE)))
      .compose(progressUpdateHandler -> checkIfCollectionPresent(requestInput)).compose(
        checkCollectionTableHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(3, 8)).put(MESSAGE,COLLECTION_RESPONSE)))
            .compose(progressUpdateHandler -> checkForCrs(requestInput)).compose(
        checkCollectionTableHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(4, 8)).put(MESSAGE,CRS_RESPONSE)))
            .compose(progressHandler->getMetaDataFromS3(requestInput)).compose(
                    checkCollectionTableHandler -> utilClass.updateJobTableProgress(
                            requestInput.put("progress", calculateProgress(5, 8)).put(MESSAGE,S3_RESPONSE)))
            .compose(progressUpdateHandler -> onboardingCollection(requestInput)).compose(
        onboardingCollectionHandler -> utilClass.updateJobTableProgress(
          requestInput.put("progress", calculateProgress(6, 8)).put(MESSAGE,ONBOARDING_RESPONSE)))
      .compose(progressUpdateHandler -> checkDbAndTable(requestInput))
            .compose(
                    onboardingCollectionHandler -> utilClass.updateJobTableProgress(
                            requestInput.put("progress", calculateProgress(7, 8)).put(MESSAGE,VERIFYING_RESPONSE)))
            .compose(progressUpdateHandler->ogr2ogrCmdExtent(requestInput))
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
  public Future<JsonObject> makeCatApiRequest(JsonObject requestInput) {
    Promise<JsonObject> promise = Promise.promise();
    webClient.get(catServerPort, catServerHost, catRequestUri)
      .addQueryParam("id", requestInput.getString("resourceId")).send()
      .onSuccess(responseFromCat -> {
        if (responseFromCat.statusCode() == 200) {
          requestInput.put("owner",
            responseFromCat.bodyAsJsonObject().getJsonArray("results").getJsonObject(0)
              .getString("ownerUserId"));
          if (!(requestInput.getValue("owner").toString().equals(requestInput.getValue("userId").toString()))) {
            LOGGER.error(RESOURCE_OWNERSHIP_ERROR);
            promise.fail(RESOURCE_OWNERSHIP_ERROR);
            return;
          }
          requestInput.put("accessPolicy",
            responseFromCat.bodyAsJsonObject().getJsonArray("results").getJsonObject(0)
              .getString("accessPolicy"));
          promise.complete(requestInput);
        } else {
          LOGGER.error(ITEM_NOT_PRESENT_ERROR);
          promise.fail(ITEM_NOT_PRESENT_ERROR);
        }
      }).onFailure(failureResponseFromCat -> {
        LOGGER.error(CAT_RESPONSE_FAILURE + failureResponseFromCat.getMessage());
        promise.fail(CAT_RESPONSE_FAILURE);
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
            LOGGER.error(COLLECTION_PRESENT_ERROR);
            promise.fail(COLLECTION_PRESENT_ERROR);
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
              Map<String, String> authorityAndCode = extractAuthorityAndCode(cmdOutput);
              String organization = authorityAndCode.get("authority");
              if (!organization.equals("EPSG")) {
                LOGGER.error(CRS_ERROR);
                promise.fail(CRS_ERROR);
                return;
              }
              int organizationCoOrdId = Integer.parseInt(authorityAndCode.get("code"));
              LOGGER.debug("organization " + organization + " crs " + organizationCoOrdId);
              validSridFromDatabase(organizationCoOrdId, input, promise);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failed in ogrInfo because {}", failureHandler.getMessage());
              promise.fail(OGR_INFO_FAILED);
            });
    return promise.future();
  }

  private Map<String, String> extractAuthorityAndCode(JsonObject ogrinfoCmdOutput) {
    Map<String, String> authorityAndCode = new HashMap<>();

    JsonArray layers = ogrinfoCmdOutput.getJsonArray("layers", new JsonArray());
    if (layers.isEmpty()) {
      return authorityAndCode; // Return empty map if no layers are found
    }

    JsonObject firstLayer = layers.getJsonObject(0);
    JsonArray geometryFields = firstLayer.getJsonArray("geometryFields", new JsonArray());
    if (geometryFields.isEmpty()) {
      return authorityAndCode; // Return empty map if no geometry fields are found
    }

    JsonObject geometryField = geometryFields.getJsonObject(0);
    JsonObject coordinateSystem = geometryField.getJsonObject("coordinateSystem", new JsonObject());
    JsonObject projjson = coordinateSystem.getJsonObject("projjson", new JsonObject());
    JsonObject id = projjson.getJsonObject("id", new JsonObject());

    String authority = id.getString("authority", "");
    int code = id.getInteger("code", 0);

    authorityAndCode.put("authority", authority);
    authorityAndCode.put("code", String.valueOf(code));

    return authorityAndCode;
  }

  private void validSridFromDatabase(int organizationCoordId, JsonObject input,
                                     Promise<JsonObject> onboardingPromise) {
    pgPool.withConnection(sqlConnection -> sqlConnection.preparedQuery(CRS_TO_SRID_SELECT_QUERY)
      .execute(Tuple.of(organizationCoordId)).onSuccess(rows -> {
        if (!rows.iterator().hasNext()) {
          LOGGER.error("Failed to fetch CRS: No rows found.");
          onboardingPromise.fail(CRS_FETCH_FAILED);
          return;
        }
        Row row = rows.iterator().next();
        String crs = row.getString("crs");
        int srid = row.getInteger("srid");
        input.put("crs", crs);
        input.put("srid", srid);
        onboardingPromise.complete(input);
      }).onFailure(failureHandler -> {
                LOGGER.error("Failed to fetch CRS from table: {}", failureHandler.getMessage());
        onboardingPromise.fail("Failed to fetch CRS from table.");
      }));
  }

  /**
   * Configures S3 options for HTTP access and path-based access.
   *
   * @param ogrinfo the {@link CommandLine} object to be configured with S3 options.
   */

  private void setS3Options(CommandLine ogrinfo){
      if (!s3conf.isHttps()) {
            ogrinfo.addArgument("--config");
            ogrinfo.addArgument("AWS_HTTPS");
            ogrinfo.addArgument("NO");
      }
      
      if (s3conf.isPathBasedAccess()) {
            ogrinfo.addArgument("--config");
            ogrinfo.addArgument("AWS_VIRTUAL_HOSTING");
            ogrinfo.addArgument("FALSE");
      }
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
    ogrinfo.addArgument("--debug");
    ogrinfo.addArgument("ON");
    ogrinfo.addArgument("--config");
    ogrinfo.addArgument("CPL_VSIL_USE_TEMP_FILE_FOR_RANDOM_WRITE");
    ogrinfo.addArgument("NO");
    setS3Options(ogrinfo);
    ogrinfo.addArgument("--config");
    ogrinfo.addArgument("AWS_S3_ENDPOINT");
    ogrinfo.addArgument(s3conf.getEndpoint().replaceFirst("https?://", "")); // GDAL needs endpoint without protocol
    ogrinfo.addArgument("--config");
    ogrinfo.addArgument("AWS_ACCESS_KEY_ID");
    ogrinfo.addArgument(s3conf.getAccessKey());
    ogrinfo.addArgument("--config");
    ogrinfo.addArgument("AWS_SECRET_ACCESS_KEY");
    ogrinfo.addArgument(s3conf.getSecretKey());
    ogrinfo.addArgument("-json");
    ogrinfo.addArgument("-ro");
    ogrinfo.addArgument(String.format("/vsis3/%s%s", s3conf.getBucket(), filename));

    return ogrinfo;
  }

  /**
   * Executes a command using 'ogr2ogr' to onboard the collection.
   *
   * @param input A JsonObject containing the necessary information for executing the 'ogr2ogr' command.
   * @return A Future<JsonObject> representing the outcome of the command. It completes with the updated
   *         JsonObject on success or fails with an error message if the command execution fails.
   */
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
        LOGGER.error("Failed to onboard the collection in ogr2ogr because {}", e1.getMessage());
        onboardingPromise.fail(OGR_2_OGR_FAILED);
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
        Tuple.of(collectionsDetailsTableName, title, description, DEFAULT_SERVER_CRS))
      .compose(
        collectionResult -> sqlClient.preparedQuery(COLLECTION_TYPE_INSERT_QUERY)
      .execute(Tuple.of(collectionsDetailsTableName,FEATURE)))
      .compose(
        collectionsDetailsResult -> sqlClient.preparedQuery(COLLECTION_SUPPORTED_CRS_INSERT_QUERY)
          .execute(Tuple.of(collectionsDetailsTableName, srid))).compose(
        collectionTypeResult -> sqlClient.preparedQuery(ROLES_INSERT_QUERY)
          .execute(Tuple.of(userId, role))).compose(
        rgDetailsResult -> sqlClient.preparedQuery(RI_DETAILS_INSERT_QUERY)
          .execute(Tuple.of(resourceId, userId, accessPolicy))).compose(
        riDetailsResult -> sqlClient.preparedQuery(STAC_COLLECTION_ENCLOSURE_INSERT_QUERY).execute(
          Tuple.of(collectionsDetailsTableName, title, fileName, COLLECTION_TYPE, fileSize))).compose(stacCollectionResult -> ogr2ogrCmd(input))
      .compose(onBoardingSuccess -> sqlClient.query(grantQuery).execute())
      .onSuccess(grantQueryResult -> {
        LOGGER.debug("Collection onboarded successfully ");
        promise.complete();
      }).onFailure(failure -> {
        String message = Objects.equals(failure.getMessage(), OGR_2_OGR_FAILED) ? failure.getMessage() :  ONBOARDING_FAILED_DB_ERROR;
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
    setS3Options(cmdLine);
    cmdLine.addArgument("--config");
    cmdLine.addArgument("AWS_S3_ENDPOINT");
    cmdLine.addArgument(s3conf.getEndpoint().replaceFirst("https?://", "")); // GDAL needs endpoint without protocol
    cmdLine.addArgument("--config");
    cmdLine.addArgument("AWS_ACCESS_KEY_ID");
    cmdLine.addArgument(s3conf.getAccessKey());
    cmdLine.addArgument("--config");
    cmdLine.addArgument("AWS_SECRET_ACCESS_KEY");
    cmdLine.addArgument(s3conf.getSecretKey());

    cmdLine.addArgument(String.format("/vsis3/%s%s", s3conf.getBucket(), filename));
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
                                        LOGGER.error(TABLE_NOT_EXIST_ERROR);
                                        throw new RuntimeException(TABLE_NOT_EXIST_ERROR);
                                      }
                                    })
                                .onSuccess(
                                    ar -> {
                                      conn.close();
                                      promise.complete();
                                    });
                          } else {
                            conn.close();
                            LOGGER.error(COLLECTION_NOT_PRESENT_ERROR);
                            return Future.failedFuture(
                                    COLLECTION_NOT_PRESENT_ERROR);
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
            dataFromS3.getFullyQualifiedUrlString(fileName);
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

  /**
   * Creates a CommandLine instruction for 'ogrinfo' to fetch metadata from a PostgreSQL table in JSON format.
   *
   * @param input A JsonObject containing:
   *              - "collectionsDetailsTableId": The name of the PostgreSQL table to query.
   * @return A CommandLine object representing the command to execute.
   */
  private CommandLine getOrgInfoBBox(JsonObject input) {
    String collectionsDetailsTableId = input.getString("collectionsDetailsTableId");
    CommandLine ogrInfo = new CommandLine("ogrinfo");
    ogrInfo.addArgument("-al");
    ogrInfo.addArgument("-so");
    ogrInfo.addArgument("-json");
    ogrInfo.addArgument(
            String.format("PG:host=%s dbname=%s user=%s port=%d password=%s schemas=public", databaseHost,
                    databaseName, databaseUser, databasePort, databasePassword), false);
    ogrInfo.addArgument(collectionsDetailsTableId);
    ogrInfo.addArgument("-nocount");
    ogrInfo.addArgument("-nomd");
    ogrInfo.addArgument("-geom=NO");
    return ogrInfo;
  }
  /**
   * Retrieves the bounding box (bbox) information from a PostgreSQL table using the 'ogrinfo' tool
   * and updates the 'collections_details' table with this data.
   *
   * This method executes a command line instruction to obtain bbox information from PostgreSQL
   * and updates the 'collections_details' table's 'bbox' column.
   *
   * @param input A JsonObject with the necessary parameters, including:
   *              - "collectionsDetailsTableId": The name of the PostgreSQL table to query.
   * @return A Future<Void> completes with the updated input object on success, or fails with an error message on failure.
   */
  public Future<Void> ogr2ogrCmdExtent(JsonObject input) {
    LOGGER.debug("Trying to update the Collection table.");
    Promise<Void> promise = Promise.promise();

    vertx
        .<JsonArray>executeBlocking(
            extentPromise -> {
              LOGGER.debug("Trying ogrInfo for ogr");
              CommandLine cmdLine = getOrgInfoBBox(input);
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
                JsonArray extentArray =
                    new JsonObject(Buffer.buffer(output))
                        .getJsonArray("layers")
                        .getJsonObject(0)
                        .getJsonArray("geometryFields")
                        .getJsonObject(0)
                        .getJsonArray("extent",new JsonArray());
                extentPromise.complete(extentArray);
              } catch (IOException e1) {
                LOGGER.error("Failed while getting ogrInfo because {} and also the collection is there in the database", e1.getMessage());
                extentPromise.fail(
                    "Failed while getting ogrInfo because {} and also the collection is there in the database" + e1.getMessage());
              }
            },
            VERTX_EXECUTE_BLOCKING_IN_ORDER)
        .onSuccess(
            extent -> {
              input.put("extent", extent);
              updateCollectionsTableBbox(input,promise);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failed in ogrInfo while getting extent because {}", failureHandler.getMessage());
              promise.fail(failureHandler.getMessage());
            });
    return promise.future();
  }
  /**
   * Updates the 'bbox' column in the 'collections_details' table.
   *
   * This method updates the specified collection with a new bounding box (bbox) in PostgreSQL.
   * The new bbox is provided as a JsonArray of floating-point numbers along with a collection ID.
   *
   * @param input   A JsonObject with:
   *                - "extent": A JsonArray of Float values for the new bbox.
   *                - "collectionsDetailsTableId": The ID of the collection to update.
   * @param promise A Promise<Void> indicating success or failure of the update operation.
   */
  public void updateCollectionsTableBbox(JsonObject input,Promise promise) {
    JsonArray extent = input.getJsonArray("extent");
    List<Float> bboxArray = new ArrayList<Float>(extent.getList());
    LOGGER.info("Trying to update the bbox ");

    String collectionsDetailsId = input.getString("collectionsDetailsTableId");

    pgPool.withConnection(
      sqlConnection -> sqlConnection.preparedQuery(UPDATE_COLLECTIONS_DETAILS)
      .execute(Tuple.of(bboxArray.toArray(),collectionsDetailsId))).onSuccess(successResult -> {
      LOGGER.debug("Bbox updated.");
      promise.complete();
    }).onFailure(failureHandler -> {
      LOGGER.error("Failed to update bbox: {}", failureHandler.getMessage());
      promise.fail("Failed to update bbox: " + failureHandler.getMessage());
    });
  }
  private float calculateProgress(int currentStep, int totalSteps) {
    return ((float) currentStep / totalSteps) * 100;
  }


}
