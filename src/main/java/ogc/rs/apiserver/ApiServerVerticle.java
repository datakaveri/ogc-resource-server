package ogc.rs.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.common.DataFromS3;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.ProcessException;
import ogc.rs.catalogue.CatalogueService;
import ogc.rs.database.DatabaseService;
import ogc.rs.jobs.JobsService;
import ogc.rs.metering.MeteringService;
import ogc.rs.processes.ProcessesRunnerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.apiserver.handlers.DxTokenAuthenticationHandler.USER_KEY;
import static ogc.rs.apiserver.util.Constants.*;
import static ogc.rs.common.Constants.*;
import static ogc.rs.metering.util.MeteringConstant.*;
import static ogc.rs.metering.util.MeteringConstant.ROLE;
import static ogc.rs.metering.util.MeteringConstant.USER_ID;

/**
 * The OGC Resource Server API Verticle.
 *
 * <h1>OGC Resource Server API Verticle</h1>
 *
 * <p>The API Server verticle implements the OGC Resource Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2023-06-12
 */
public class ApiServerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);
  static String S3_BUCKET;
  static String S3_REGION;
  static String S3_ACCESS_KEY;
  static String S3_SECRET_KEY;
  CatalogueService catalogueService;
  MeteringService meteringService;
  private Router router;
  private String ogcBasePath;
  private String hostName;
  private DatabaseService dbService;
  private Buffer ogcLandingPageBuf;
  private HttpClient httpClient;
  private ProcessesRunnerService processService;
  private JobsService jobsService;


  JsonArray allCrsSupported = new JsonArray();
    
  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster/single instance, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443 or an HTTP server at port 8080.
   *
   * @throws Exception which is a startup exception TODO Need to add documentation for all the
   */
  @Override
  public void start() throws Exception {

      /* Get base paths from config */
    ogcBasePath = config().getString("ogcBasePath");
    hostName = config().getString("hostName");

    catalogueService = new CatalogueService(vertx, config());
    meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);

    /* Initialize OGC landing page buffer - since configured hostname needs to be in it */
    String landingPageTemplate = vertx.fileSystem().readFileBlocking("docs/landingPage.json").toString();
    ogcLandingPageBuf = Buffer.buffer(landingPageTemplate.replace("$HOSTNAME", hostName));

    /* Initialize S3-related things */
    S3_BUCKET = config().getString("s3BucketName");
    S3_REGION = config().getString("s3Region");
    S3_ACCESS_KEY = config().getString("s3AccessKey");
    S3_SECRET_KEY = config().getString("s3SecretKey");

    processService = ProcessesRunnerService.createProxy(vertx,PROCESSING_SERVICE_ADDRESS);
    dbService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    jobsService = JobsService.createProxy(vertx,JOBS_SERVICE_ADDRESS);

    // TODO: ssl configuration
    HttpServerOptions serverOptions = new HttpServerOptions();
    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    int port = config().getInteger("httpPort") == null ? 8080 : config().getInteger("httpPort");
    
    HttpServer server = vertx.createHttpServer(serverOptions);
    
    router = Router.router(vertx);

    /*
     * Start server only once router has at least one route (it will have only one, '/' since both
     * the OGC and STAC sub-routers are bound to that path).
     * 
     * Check every second for 20 seconds if router has been created. If it has, start server.
     */ 
    AtomicInteger waitForRouterCounter = new AtomicInteger(20);
    vertx.setPeriodic(1000, wait -> {
        
      if (!router.getRoutes().isEmpty()) {
        vertx.cancelTimer(wait);
        server.requestHandler(router).listen(port)
            .onSuccess(
                success -> LOGGER.info("Started HTTP server at port:" + success.actualPort()))
            .onFailure(Throwable::printStackTrace);
      } else {
        LOGGER.info("Waiting for router to be initialized");
        
        waitForRouterCounter.decrementAndGet();
        if (waitForRouterCounter.intValue() == 0) {
          LOGGER.fatal("Router not initialized - Failed to start HTTP server");
          
          vertx.cancelTimer(wait);
          throw new RuntimeException();
        }
      }
    });

    HttpClientOptions httpCliOptions = new HttpClientOptions().setSsl(true);

    if(System.getProperty("s3.mock") != null){
        LOGGER.fatal("S3 is being mocked!! Are you testing something?");
        httpCliOptions.setTrustAll(true).setVerifyHost(false);
    }

    httpClient = vertx.createHttpClient(httpCliOptions);
  }

  /**
   * Reset {@link ApiServerVerticle#router} by clearing it and then adding all routers in
   * <code>routerList</code> as sub-routers at the root path. Also adds a handler for
   * {@link Route#last()}, i.e. the last route to handle collection/API not found 404 errors.
   * 
   * @param routerList list of routers to be added as sub-routers
   */
  public void resetRouter(List<Router> routerList) {
    router.clear();

    routerList.forEach(subrouter -> router.route("/*").subRouter(subrouter));

    /* Add route to handle not implemented / not found paths */
    router.route().last().handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON);
      response.setStatusCode(404);
      response.send(new JsonObject().put(CODE_KEY, NOT_FOUND)
          .put(DESCRIPTION_KEY, INVALID_ENDPOINT_ERROR).toBuffer());
    });
  }

  public void sendOgcLandingPage(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    response.end(ogcLandingPageBuf);
  }

  public void executeJob(RoutingContext routingContext) {

    RequestParameters paramsFromOasValidation =
      routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    JsonObject requestBody = paramsFromOasValidation.body().getJsonObject().getJsonObject("inputs");
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    requestBody.put("processId", paramsFromOasValidation.pathParameter("processId").getString())
      .put("userId", authInfo.getString("userId")).put("role", authInfo.getString("role"));

    processService.run(requestBody, handler -> {

      if (handler.succeeded()) {
        LOGGER.debug("Process started successfully.");
        routingContext.response().headers().add("Location", handler.result().getString("location"));
        handler.result().remove("location");
        routingContext.put("response", handler.result().toString());
        routingContext.put("statusCode", 201);
        routingContext.next();
      } else {
        ProcessException processException = (ProcessException) handler.cause();
        routingContext.put("response", processException.getJson().toString());
        routingContext.put("statusCode", processException.getStatusCode());
        routingContext.next();
      }
    });
  }

  public void getStatus(RoutingContext routingContext) {

    RequestParameters paramsFromOasValidation =
      routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    JsonObject requestBody = new JsonObject();
    requestBody.put("jobId", paramsFromOasValidation.pathParameter("jobId").getString())
      .put("userId", authInfo.getString("userId")).put("role", authInfo.getString("role"));

    jobsService.getStatus(requestBody).onSuccess(handler -> {
      {
        LOGGER.debug("Job status found.");
        routingContext.put("response", handler.toString());
        routingContext.put("statusCode", 200);
        routingContext.next();
      }
    }).onFailure(failureHandler -> {
      LOGGER.debug("JOB status not found. " + failureHandler);
      ProcessException processException = (ProcessException) failureHandler;
      routingContext.put("response", processException.getJson().toString());
      routingContext.put("statusCode", processException.getStatusCode());
      routingContext.next();
    });
  }
  
  public void getFeature(RoutingContext routingContext) {

    if (!(Boolean) routingContext.get("isAuthorised")){
      routingContext.next();
      return;
    }
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    String collectionId = routingContext.request().path().split("/")[2];
    Integer featureId = requestParameters.pathParameter("featureId").getInteger();
    Map<String, Object> queryParams = requestParameters.toJson().getJsonObject("query").getMap();
    Map<String, String> queryParamsMap = queryParams.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));
    LOGGER.debug("<APIServer> QP- {}", queryParamsMap);
    Future<Map<String, Integer>> isCrsValid = dbService.isCrsValid(collectionId, queryParamsMap);
    isCrsValid
        .compose(crs -> dbService.getFeature(collectionId, featureId, queryParamsMap, crs))
        .onSuccess(success -> {
              // TODO: Add base_path from config
              success.put("links", new JsonArray()
                  .add(new JsonObject().put("href", hostName + ogcBasePath + COLLECTIONS + "/" + collectionId + "/items/"
                                      + featureId)
                              .put("rel", "self")
                              .put("type", "application/geo+json"))
                      .add(new JsonObject()
                              .put("href", hostName + ogcBasePath + COLLECTIONS + "/" + collectionId)
                              .put("rel", "collection")
                              .put("type", "application/json")));
              routingContext.put("response", success.toString());
              routingContext.put("statusCode", 200);
              routingContext.put(
                  "crs", "<" + queryParamsMap.getOrDefault("crs", DEFAULT_SERVER_CRS) + ">");
              routingContext.next();
            })
        .onFailure(
            failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", ((OgcException) failed).getStatusCode());
              } else {
                OgcException ogcException =
                    new OgcException(500, "Internal Server Error", "Internal Server Error");
                routingContext.put("response", ogcException.getJson().toString());
                routingContext.put("statusCode", ogcException.getStatusCode());
              }
              routingContext.next();
            });
  }
  
  public void getFeatures(RoutingContext routingContext) {

    if (!(Boolean) routingContext.get("isAuthorised")){
      routingContext.next();
      return;
    }

    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    String collectionId = routingContext.request().path().split("/")[2];
    Map<String, Object> queryParams = requestParameters.toJson().getJsonObject("query").getMap();
    Map<String, String> queryParamsMap = queryParams.entrySet()
        .stream()
        .filter(i -> i.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    LOGGER.debug("<APIServer> QP- {}", queryParamsMap);

    Future<Map<String, Integer>> isCrsValid = dbService.isCrsValid(collectionId, queryParamsMap);
    isCrsValid
        .compose(datetimeCheck -> {
          try {
            String datetime;
            ZonedDateTime zone, zone2;
            DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
            if (queryParamsMap.containsKey("datetime")) {
              datetime = queryParamsMap.get("datetime");
              if (!datetime.contains("/")) {
                zone = ZonedDateTime.parse(datetime, formatter);
              } else if (datetime.contains("/")) {
                String[] dateTimeArr = datetime.split("/");
                if (dateTimeArr[0].equals("..")) { // -- before
                  zone = ZonedDateTime.parse(dateTimeArr[1], formatter);
                }
                else if (dateTimeArr[1].equals("..")) { // -- after
                  zone = ZonedDateTime.parse(dateTimeArr[0], formatter);
                }
                else {
                  zone = ZonedDateTime.parse(dateTimeArr[0], formatter);
                  zone2 = ZonedDateTime.parse(dateTimeArr[1], formatter);
                  if (zone2.isBefore(zone)){
                    OgcException ogcException = new OgcException(400, "Bad Request", "After time cannot be lesser " +
                        "than Before time");
                    return Future.failedFuture(ogcException);
                  }
                }
              }
            }
          } catch (NullPointerException ne) {
                OgcException ogcException =
                    new OgcException(500, "Internal Server Error", "Internal Server Error");
                return Future.failedFuture(ogcException);
            } catch (DateTimeParseException dtpe) {
              OgcException ogcException =
                  new OgcException(400, "Bad Request", "Time parameter not in ISO format");
              return Future.failedFuture(ogcException);
            }
            return Future.succeededFuture();
          })
        .compose(dbCall -> dbService.getFeatures(collectionId, queryParamsMap, isCrsValid.result()))
        .onSuccess(success -> {
          // TODO: Add base_path from config
          int next, offset, limit;
          offset = Integer.parseInt(queryParamsMap.get("offset"));
          limit = Integer.parseInt(queryParamsMap.get("limit"));

          next = offset + limit;
          success.put("links", new JsonArray()
                  .add(new JsonObject()
                      .put("href", hostName + ogcBasePath + COLLECTIONS + "/" + collectionId + "/items")
                      .put("rel", "self")
                      .put("type", "application/geo+json"))
                  .add(new JsonObject()
                      .put("href", hostName + ogcBasePath  + COLLECTIONS + "/" + collectionId + "/items")
                      .put("rel", "alternate")
                      .put("type", "application/geo+json")))
              .put("timeStamp", Instant.now().toString());
          if (next < success.getInteger("numberMatched")) {
            success.getJsonArray("links")
                .add(new JsonObject()
                    .put("href",
                        hostName + ogcBasePath + COLLECTIONS + "/" + collectionId + "/items?offset=" + next + "&limit" +
                            "=" + limit)
                    .put("rel", "next")
                    .put("type", "application/geo+json" ));
          }
          routingContext.put("response",success.toString());
          routingContext.put("statusCode", 200);
          routingContext.put("crs", "<" + queryParamsMap.getOrDefault("crs", DEFAULT_SERVER_CRS) + ">");
          routingContext.next();
        })
        .onFailure(failed -> {
          if (failed instanceof OgcException){
            routingContext.put("response",((OgcException) failed).getJson().toString());
            routingContext.put("statusCode", ((OgcException) failed).getStatusCode());
          }
          else{
            OgcException ogcException = new OgcException(500, "Internal Server Error", "Internal Server Error");
            routingContext.put("response", ogcException.getJson().toString());
            routingContext.put("statusCode", ogcException.getStatusCode());
          }
          routingContext.next();
        });
  }

  public void getProcesses(RoutingContext routingContext) {
    RequestParameters paramsFromOasValidation = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

    int limit = paramsFromOasValidation.queryParameter("limit").getInteger();
    dbService.getProcesses(limit).onSuccess(successResult -> {
      routingContext.put("response", successResult.toString());
      routingContext.put("statusCode", 200);
      routingContext.next();
    }).onFailure(failureHandler -> {
        ProcessException processException = (ProcessException) failureHandler;
        routingContext.put("response", processException.getJson().toString());
        routingContext.put("statusCode", processException.getStatusCode());
        routingContext.next();
    });
  }

  public void getProcess(RoutingContext routingContext) {
    String processId = routingContext.pathParams().get("processId");
    dbService.getProcess(processId).onSuccess(successResult -> {
      routingContext.put("response", successResult.toString());
      routingContext.put("statusCode", 200);
      routingContext.next();
    }).onFailure(failureHandler -> {
      LOGGER.error("Failed to get process "+failureHandler);
      ProcessException processException = (ProcessException) failureHandler;
      routingContext.put("response", processException.getJson().toString());
      routingContext.put("statusCode", processException.getStatusCode());
      routingContext.next();
    });
  }

  public void buildResponse(RoutingContext routingContext) {
    routingContext
        .response()
        .setStatusCode(routingContext.get("statusCode"))
        .end((String) routingContext.get("response"));
  }

  public void validateQueryParams(RoutingContext routingContext) {
      Set<String> queryParamsInRequest = routingContext.queryParams().names();
      RequestParameters paramsOasValidation = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
      Set<String> allParamsDefinedInOas = paramsOasValidation.toJson().getJsonObject("query").fieldNames();
      Set<String> badQueryParams = queryParamsInRequest.stream()
          .filter(p -> !allParamsDefinedInOas.contains(p))
          .collect(Collectors.toSet());
      if(!badQueryParams.isEmpty()) {
        routingContext.fail(new OgcException(400, "Bad Request", "Invalid parameters in request " + badQueryParams));
      } else
        routingContext.next();
  }

  public void getCollection(RoutingContext routingContext) {
      String collectionId = routingContext.request().path().split("/")[2];
      LOGGER.debug("collectionId- {}", collectionId);
      dbService.getCollection(collectionId)
          .onSuccess(success -> {
            LOGGER.debug("Success! - {}", success.toString());
            JsonObject jsonResult = new JsonObject();
            if (success.get(0).getJsonArray("type").contains("FEATURE"))
              jsonResult = buildCollectionFeatureResult(success);
            else if (success.get(0).getJsonArray("type").contains("MAP"))
              jsonResult = buildCollectionTileResult(success);
            routingContext.put("response", jsonResult.toString());
            routingContext.put("statusCode", 200);
            routingContext.next();
          })
          .onFailure(failed -> {
            if (failed instanceof OgcException){
              routingContext.put("response",((OgcException) failed).getJson().toString());
              routingContext.put("statusCode", ((OgcException) failed).getStatusCode());
            }
            else{
              OgcException ogcException = new OgcException(500, "Internal Server Error", "Internal Server Error");
              routingContext.put("response", ogcException.getJson().toString());
              routingContext.put("statusCode", ogcException.getStatusCode());
            }
            routingContext.next();
          });
  }

  public void getCollections(RoutingContext routingContext) {

    dbService.getCollections()
        .onSuccess(success -> {
              JsonArray collections = new JsonArray();
              /*
               TODO: When updating OGC Tiles API to include multiple itemTypes, use Java stream().filter() to filter
                out different itemTypes to their respective builder functions
                **/
              success.forEach(collection -> {
                    try {
                      JsonObject json = new JsonObject();
                      List<JsonObject> tempArray = new ArrayList<>();
                      tempArray.add(collection);
                      if (collection.getJsonArray("type").contains("FEATURE"))
                        json = buildCollectionFeatureResult(tempArray);
                      else if (collection.getJsonArray("type").contains("MAP"))
                        json = buildCollectionTileResult(tempArray);
                      collections.add(json);
                    } catch (Exception e) {
                      LOGGER.error("Something went wrong here: {}", e.getMessage());
                      routingContext.put(
                          "response",
                          new OgcException(500, "Internal Server Error", "Internal Server Error")
                              .getJson()
                              .toString());
                      routingContext.put("statusCode", 500);
                      routingContext.next();
                    }
                  });
              JsonObject featureCollections = new JsonObject().put("links", new JsonArray()
                      .add(new JsonObject()
                                      .put("href", hostName + ogcBasePath + COLLECTIONS)
                                      .put("rel", "self")
                                      .put("type", "application/json")
                                      .put("title", "This document")))
                  .put("collections", collections);
              routingContext.put("response", featureCollections.toString());
              routingContext.put("statusCode", 200);
              routingContext.next();
            })
        .onFailure(failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", 404);
              } else {
                routingContext.put("response", new OgcException(500, "Internal Server Error", "Internal Server Error")
                        .getJson()
                        .toString());
                routingContext.put("statusCode", 500);
              }
              routingContext.next();
            });
  }

  public void getTile(RoutingContext routingContext) {
    String collectionId = routingContext.pathParam("collectionId");
    String tileMatrixSetId = routingContext.pathParam("tileMatrixSetId");
    String tileMatrixId = routingContext.pathParam("tileMatrix");
    String tileRow = routingContext.pathParam("tileRow");
    String tileCol = routingContext.pathParam("tileCol");
    HttpServerResponse response = routingContext.response();
    String tilesUrlString = collectionId + "/" + tileMatrixSetId + "/" + tileMatrixId + "/" + tileRow + "/" + tileCol;
    // need to set chunked for streaming response because Content-Length cannot be determined
    // beforehand.
    response.setChunked(true);
    DataFromS3 dataFromS3 =
        new DataFromS3(httpClient, S3_BUCKET, S3_REGION, S3_ACCESS_KEY, S3_SECRET_KEY);

    // determine tile format if it is a map (PNG image) or vector (MVT tile) using request header.
    String encodingType = getEncodingFromRequest(routingContext.request().getHeader("Accept"));
    if (encodingType.isEmpty()) {
      routingContext.put(
          "response",
          new OgcException(406, "Not Acceptable", "Content negotiation failed. Unsupported Media-Type.")
              .getJson()
              .toString());
      routingContext.put("statusCode", 406);
      routingContext.next();
      return;
    }
    if (encodingType.equalsIgnoreCase("image/png") || encodingType.equalsIgnoreCase("*/*")) {
      tilesUrlString = tilesUrlString.concat(".png");
      response.putHeader("Content-Type", "image/png");
    }
    else if (encodingType.equalsIgnoreCase("application/vnd.mapbox-vector-tile")) {
      tilesUrlString = tilesUrlString.concat(".pbf");
      response.putHeader("Content-Type", "application/vnd.mapbox-vector-tile");
    }

    //TODO: determine tile format using 'f' query parameter

    String urlString =
        dataFromS3.getFullyQualifiedUrlString(tilesUrlString);
    dataFromS3.setUrlFromString(urlString);
    dataFromS3.setSignatureHeader(HttpMethod.GET);
    dataFromS3
        .getDataFromS3(HttpMethod.GET)
        .onSuccess(success -> success.pipeTo(response))
        .onFailure(
            failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", 404);
              } else {
                routingContext.put(
                    "response",
                    new OgcException(500, "Internal Server Error", "Internal Server Error")
                        .getJson()
                        .toString());
                routingContext.put("statusCode", 500);
              }
              routingContext.next();
            });
  }

  public String getEncodingFromRequest(String acceptRequestHeaders) {
    Set<String> acceptedHeaders = new HashSet<>(Set.of("*/*", "image/png", "application/vnd.mapbox-vector-tile"));
    Set<String> acceptRequestHeadersSet = new HashSet<>();
    String[] tempHeaders = acceptRequestHeaders.split("[,;]");
    // to handle duplicates, ie encoding type with same weightage
    Collections.addAll(acceptRequestHeadersSet, tempHeaders);
    String[] acceptRequestHeadersWithWeight = acceptRequestHeaders.split(",");
    acceptedHeaders.retainAll(acceptRequestHeadersSet);
    if (acceptedHeaders.isEmpty()) {
      return "";
    }
    double qLarge = 0.0; String chosenHeader = "image/png";
    for (String header : acceptRequestHeadersWithWeight) {
      String[] headerArray = header.split(";");
      if (headerArray.length == 2) {
        if (qLarge < Double.parseDouble(headerArray[1].split("=")[1])
            && acceptedHeaders.contains(headerArray[0])) {
          qLarge = Double.parseDouble(headerArray[1].split("=")[1]);
          chosenHeader = headerArray[0];
        }
      }
    }
    return chosenHeader;
  }

  public void getTileSet(RoutingContext routingContext) {
    String collectionId = routingContext.pathParam("collectionId");
    String tileMatrixSetId = routingContext.pathParam("tileMatrixSetId");
    dbService
        .getTileMatrixSetRelationOverload(collectionId, tileMatrixSetId)
        .onSuccess(
            success -> {
              JsonObject tileSetResponse = buildTileSetResponse(collectionId,
                      success.get(0).getString("tilematrixset"),
                      success.get(0).getString("uri"),
                      success.get(0).getString("crs"),
                      success.get(0).getString("datatype"));
              tileSetResponse.put("title", success.get(0).getString("tilematrixset_title"));
              routingContext.put("response", tileSetResponse.toString());
              routingContext.put("statusCode", 200);
              routingContext.next();
            })
        .onFailure(
            failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", 404);
              } else {
                routingContext.put(
                    "response",
                    new OgcException(500, "Internal Server Error", "Internal Server Error")
                        .getJson()
                        .toString());
                routingContext.put("statusCode", 500);
              }
              routingContext.next();
            });
  }

  public void getTileSetList(RoutingContext routingContext) {
    String collectionId = routingContext.pathParam("collectionId");
    dbService.getTileMatrixSetRelation(collectionId)
        .onSuccess(success -> {
              JsonObject tileSetListResponse = new JsonObject()
                      .put("links", new JsonObject().put("href", hostName + ogcBasePath + COLLECTIONS + "/"
                              + collectionId + "/map/tiles")
                              .put("rel", "self")
                              .put("type", "application/geo+json")
                              .put("title", collectionId + " tileset data"));
              JsonArray tileSets = new JsonArray();
              success.forEach(tileMatrixSet -> tileSets.add(
                  buildTileSetResponse(collectionId,
                      tileMatrixSet.getString("tilematrixset"),
                      tileMatrixSet.getString("uri"),
                      tileMatrixSet.getString("crs"),
                      tileMatrixSet.getString("datatype"))));
              tileSetListResponse.put("tilesets", tileSets);
              routingContext.put("response", tileSetListResponse.toString());
              routingContext.put("statusCode", 200);
              routingContext.next();
            })
        .onFailure(failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", 404);
              } else {
                routingContext.put(
                    "response",
                    new OgcException(500, "Internal Server Error", "Internal Server Error")
                        .getJson()
                        .toString());
                routingContext.put("statusCode", 500);
              }
              routingContext.next();
            });
  }

  public void getTileMatrixSet(RoutingContext routingContext) {
    String tileMatrixSetId = routingContext.pathParam("tileMatrixSetId");
    dbService.getTileMatrixSetMetaData(tileMatrixSetId)
        .onSuccess(success -> {
              // drop collection_id, tilematrixset_id,
              // will return everything, we have to parse and store the individual matrices in the
              // format of tileMatrices
              JsonObject tileMatrixSetResponse = new JsonObject();
              JsonObject tempDbRes = success.get(0);
              tileMatrixSetResponse
                  .put("title", tempDbRes.getString("title"))
                  .put("id", tempDbRes.getString("id"))
                  .put("uri", tempDbRes.getString("uri"))
                  .put("crs", tempDbRes.getString("crs"));
              JsonArray tileMatrices = new JsonArray();
              success.forEach(tileMatrix -> {
                    // TODO: Streamline this build
                    JsonObject tempTileMatrix = buildTileMatrices(
                            tileMatrix.getString("tilematrix_id"),
                            tileMatrix.getString("tilematrixmeta_title"),
                            tileMatrix.getString("scaledenominator"),
                            tileMatrix.getString("cellsize"),
                            tileMatrix.getString("tilewidth"),
                            tileMatrix.getString("tileheight"),
                            tileMatrix.getString("matrixwidth"),
                            tileMatrix.getString("matrixheight"));
                    tileMatrices.add(tempTileMatrix);
                  });
              tileMatrixSetResponse.put("tileMatrices", tileMatrices);
              routingContext.put("response", tileMatrixSetResponse.toString());
              routingContext.put("statusCode", 200);
              routingContext.next();
            })
        .onFailure(failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", 404);
              } else {
                  routingContext.put(
                      "response",
                      new OgcException(500, "Internal Server Error", "Internal Server Error")
                          .getJson()
                          .toString());
                  routingContext.put("statusCode", 500);
              }
              routingContext.next();
            });
  }

  public void getTileMatrixSetList(RoutingContext routingContext) {
    dbService.getTileMatrixSets()
        .onSuccess(success -> {
              JsonArray tileMatrixSets = new JsonArray();
              success.forEach(tileMatrixSet -> {
                    tileMatrixSet.put("links", new JsonObject()
                        .put("rel", "self")
                        .put("href", hostName + ogcBasePath + "tileMatrixSets/" + tileMatrixSet.getString("id"))
                        .put("type", "application/json")
                        .put("title", tileMatrixSet.getString("title")));
                    tileMatrixSets.add(tileMatrixSet);
                  });
              routingContext.put("response", new JsonObject().put("tileMatrixSets", tileMatrixSets).toString());
              routingContext.put("statusCode", 200);
              routingContext.next();
            })
        .onFailure(failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", 404);
              } else {
                  routingContext.put(
                      "response",
                      new OgcException(500, "Internal Server Error", "Internal Server Error")
                          .getJson()
                          .toString());
                  routingContext.put("statusCode", 500);
              }
              routingContext.next();
            });
  }

  public void putCommonResponseHeaders(RoutingContext routingContext) {
    routingContext.response()
        .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
        .putHeader("Pragma", "no-cache")
        .putHeader("Expires", "0")
        .putHeader("X-Content-Type-Options", "nosniff");
    // include crs when features - /items api is accessed
    if (routingContext.data().containsKey("crs"))
      routingContext.response().putHeader("Content-Crs", (String) routingContext.get("crs"));
    routingContext.next();
  }

  private JsonObject buildCollectionFeatureResult(List<JsonObject> success) {
    JsonObject collection = success.get(0);
    JsonObject extent = new JsonObject();
    collection.put("properties", new JsonObject());
    if (collection.getString("datetime_key") != null && !collection.getString("datetime_key").isEmpty())
      collection.getJsonObject("properties").put("datetimeParameter", collection.getString("datetime_key"));
    if (collection.getJsonArray("bbox") != null)
      extent.put("spatial", new JsonObject().put("bbox", new JsonArray().add(collection.getJsonArray("bbox"))));
    if (collection.getJsonArray("temporal") != null)
      extent.put("temporal", new JsonObject().put("interval",
          new JsonArray().add(collection.getJsonArray("temporal"))));
    if (!extent.isEmpty())
      collection.put("extent", extent);
    collection.put("links", new JsonArray()
            .add(new JsonObject()
                .put("href", hostName + ogcBasePath + COLLECTIONS + "/" + collection.getString("id"))
                .put("rel","self")
                .put("title", collection.getString("title"))
                .put("description", collection.getString("description")))
            .add(new JsonObject()
                .put("href", hostName + ogcBasePath + COLLECTIONS + "/" + collection.getString("id") + "/items")
                .put("rel", "items")
                .put("title", collection.getString("title"))
                .put("type","application/geo+json"))
            .add(new JsonObject()
                .put("href",
                    hostName + ogcBasePath + COLLECTIONS + "/" + collection.getString("id") + "/items/{featureId}")
                .put("rel", "item")
                .put("title", "Link template for " + collection.getString("id") + " features")
                .put("templated","true")))
        .put("itemType", "FEATURE")
        .put("crs", collection.getJsonArray("crs"));
    if (collection.getJsonArray("type").contains("VECTOR"))
      collection.getJsonArray("links")
          .add(new JsonObject()
              .put("href",
                  hostName + ogcBasePath + COLLECTIONS + "/" + collection.getString("id") + "/map/tiles" +
                      "/WebMercatorQuad/{tileMatrixId}/{tileRow}/{tileCol}?f=mvt")
              .put("rel", "item")
              .put("title", "Mapbox vector tiles; the link is a URI template where {tileMatrix}/{tileRow}/{tileCol}" +
                  " is the tile in the tiling scheme 'WebMercatorQuad'")
              .put("templated","true")
              .put("type", "application/vnd.mapbox-vector-tile"));

    collection.remove("title");
    collection.remove("description");
    collection.remove("bbox");
    collection.remove("temporal");
    collection.remove("datetime_key");
    collection.remove("type");
    return collection;
  }

  private JsonObject buildCollectionTileResult(List<JsonObject> collections) {
    JsonObject collection = collections.get(0);
    collection.put("itemType", "MAP");
    collection.put("links", new JsonArray()
        .add(new JsonObject().put("href", hostName + ogcBasePath + COLLECTIONS + "/" + collection.getString("id"))
                    .put("rel", "self")
                    .put("type", "application/json")
                    .put("title", "This document"))
            .add(new JsonObject().put("href", hostName + ogcBasePath + COLLECTIONS + "/"
                    + collection.getString("id") + "/map/tiles")
                    .put("rel", "http://www.opengis.net/def/rel/ogc/1.0/tilesets-map")
                    .put("type", "application/json")
                    .put("title", "List of available map tilesets for the collection of "
                        + collection.getString("id"))));
    collection.put("extent", new JsonObject()
            .put("spatial", collection.getJsonArray("bbox"))
            .put("temporal", collection.getJsonArray("temporal")));
    collection.remove("bbox");
    collection.remove("temporal");
    collection.remove("datetime_key");
    collection.remove("type");
    return collection;
  }

  public void stacCollections(RoutingContext routingContext) {
    dbService
        .getStacCollections()
        .onSuccess(
            success -> {
              JsonArray collections = new JsonArray();
              JsonObject nestedCollections = new JsonObject();
              success.forEach(
                  collection -> {
                    try {
                      List<JsonObject> tempArray = new ArrayList<>();
                      tempArray.add(collection);
                      String jsonFilePath = "docs/getStacLandingPage.json";
                      FileSystem fileSystem = vertx.fileSystem();
                      Buffer buffer = fileSystem.readFileBlocking(jsonFilePath);
                      JsonObject stacMetadata = new JsonObject(buffer.toString());
                      String stacVersion = stacMetadata.getString("stacVersion");
                      JsonObject singleCollection = tempArray.get(0);
                      if (singleCollection.getString("license") == null
                          || singleCollection.getString("license").isEmpty()) {
                        singleCollection.put("license", stacMetadata.getString("stacLicense"));
                      }
                      if (singleCollection.getJsonArray("temporal") == null
                          || singleCollection.getJsonArray("temporal").isEmpty()) {
                        singleCollection.put("temporal", new JsonArray().add(null).add(null));
                      }
                      singleCollection
                          .put("type", "Collection")
                          .put(
                              "links",
                              new JsonArray()
                                  .add(createLink("root", STAC, null))
                                  .add(createLink("parent", STAC, null))
                                  .add(
                                      createLink(
                                          "self",
                                          STAC
                                              + "/"
                                              + COLLECTIONS
                                              + "/"
                                              + collection.getString("id"),
                                          collection.getString("title")))
                              //                                  .add(
                              //                                      createLink(
                              //                                          "items",
                              //                                          STAC
                              //                                              + "/"
                              //                                              + COLLECTIONS
                              //                                              + "/"
                              //                                              +
                              // collection.getString("id")
                              //                                              + "/"
                              //                                              + ITEMS,
                              //
                              // collection.getString("title")))
                              )
                          .put("stac_version", stacVersion)
                          .put(
                              "extent",
                              new JsonObject()
                                  .put(
                                      "spatial",
                                      new JsonObject()
                                          .put(
                                              "bbox",
                                              new JsonArray().add(collection.getJsonArray("bbox"))))
                                  .put(
                                      "temporal",
                                      new JsonObject()
                                          .put(
                                              "interval",
                                              new JsonArray()
                                                  .add(collection.getJsonArray("temporal")))));
                      if (singleCollection.containsKey("assets")) {
                        JsonObject assets = new JsonObject();

                        singleCollection
                            .getJsonArray("assets")
                            .forEach(
                                assetJson -> {
                                  JsonObject asset = new JsonObject();
                                  asset.mergeIn((JsonObject) assetJson);
                                  String href =
                                      hostName + ogcBasePath + "assets/" + asset.getString("id");
                                  asset.put("href", href);
                                  asset.put("file:size", asset.getInteger("size"));
                                  asset.remove("size");
                                  asset.remove("id");
                                  asset.remove("stac_collections_id");
                                  assets.put(((JsonObject) assetJson).getString("id"), asset);
                                });
                        singleCollection.put("assets", assets);
                      }
                      singleCollection.remove("bbox");
                      singleCollection.remove("temporal");
                      collections.add(singleCollection);
                      nestedCollections
                          .put("collections", collections)
                          .put(
                              "links",
                              new JsonArray()
                                  .add(createLink("root", STAC, null))
                                  .add(createLink("parent", STAC, null))
                                  .add(createLink("self", STAC + "/" + COLLECTIONS, null)));
                    } catch (Exception e) {
                      LOGGER.error("Something went wrong here: {}", e.getMessage());
                      routingContext.put(
                          "response",
                          new OgcException(500, "Internal Server Error", "Something " + "broke")
                              .getJson()
                              .toString());
                      routingContext.put("statusCode", 500);
                      routingContext.next();
                    }
                  });
              routingContext.put("response", nestedCollections.toString());
              routingContext.put("statusCode", 200);
              routingContext.next();
            })
        .onFailure(
            failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", 404);
              } else {
                routingContext.put(
                    "response",
                    new OgcException(500, "Internal Server Error", "Internal Server Error")
                        .getJson()
                        .toString());
                routingContext.put("statusCode", 500);
              }
              routingContext.next();
            });
  }

  public void stacCatalog(RoutingContext routingContext) {
    try {
      String jsonFilePath = "docs/getStacLandingPage.json";
      String conformanceFilePath = "docs/stacConformance.json";
      FileSystem fileSystem = vertx.fileSystem();
      Buffer buffer = fileSystem.readFileBlocking(jsonFilePath);
      Buffer conformanceBuffer = fileSystem.readFileBlocking(conformanceFilePath);
      JsonObject stacLandingPage = new JsonObject(buffer.toString());
      JsonObject stacConformance = new JsonObject(conformanceBuffer.toString());

      String type = stacLandingPage.getString("type");
      String description = stacLandingPage.getString("description");
      String title = stacLandingPage.getString("title");
      String stacVersion = stacLandingPage.getString("stacVersion");
      String catalogId = config().getString("catalogId");

      JsonArray links =
          new JsonArray()
              .add(createLink("root", STAC, title))
              .add(createLink("self", STAC, title))
              .add(
                  new JsonObject()
                      .put("rel", "service-desc")
                      .put("href", hostName + ogcBasePath + "stac/api")
                      .put("type", "application/vnd.oai.openapi+json;version=3.0")
                      .put("title", "API definition for endpoints in JSON format"))
              .add(
                  new JsonObject()
                      .put("rel", "data")
                      .put("href", hostName + ogcBasePath + "stac/collections")
                      .put("type", "application/json"))
              .add(
                  new JsonObject()
                      .put("rel", "conformance")
                      .put("href", hostName + ogcBasePath + "stac/conformance")
                      .put("type", "application/json")
                      .put("title", "STAC/WFS3 conformance classes implemented by this server"));
      dbService
          .getStacCollections()
          .onSuccess(
              success -> {
                success.forEach(
                    collection -> {
                      try {
                        links.add(
                            createLink(
                                "child",
                                STAC + "/" + COLLECTIONS + "/" + collection.getString("id"),
                                collection.getString("title")));
                      } catch (Exception e) {
                        LOGGER.error("Something went wrong here: {}", e.getMessage());
                        routingContext.put(
                            "response",
                            new OgcException(500, "Internal Server Error", "Something " + "broke")
                                .getJson()
                                .toString());
                        routingContext.put("statusCode", 500);
                        routingContext.next();
                      }
                    });
                JsonObject catalog =
                    new JsonObject()
                        .put("type", type)
                        .put("description", description)
                        .put("id", catalogId)
                        .put("stac_version", stacVersion)
                        .put("links", links)
                        .put("conformsTo", stacConformance.getJsonArray("conformsTo"));

                routingContext.put("response", catalog.encode());
                routingContext.put("statusCode", 200);
                routingContext.next();
              })
          .onFailure(
              failed -> {
                if (failed instanceof OgcException) {
                  routingContext.put("response", ((OgcException) failed).getJson().toString());
                  routingContext.put("statusCode", 404);
                } else {
                  routingContext.put(
                      "response",
                      new OgcException(500, "Internal Server Error", "Internal Server Error")
                          .getJson()
                          .toString());
                  routingContext.put("statusCode", 500);
                }
                routingContext.next();
              });

    } catch (Exception e) {
      LOGGER.debug("Error reading the JSON file: {}", e.getMessage());
      routingContext.put(
          "response",
          new OgcException(500, "Internal Server Error", "Something " + "broke")
              .getJson()
              .toString());
      routingContext.put("statusCode", 500);
      routingContext.next();
    }
  }

  public void getStacCollection(RoutingContext routingContext) {
    String collectionId = routingContext.normalizedPath().split("/")[3];
    LOGGER.debug("collectionId- {}", collectionId);
    dbService
        .getStacCollection(collectionId)
        .onSuccess(
            collection -> {
              LOGGER.debug("Success! - {}", collection.toString());
              JsonObject jsonResult = collection;
              try {
                String jsonFilePath = "docs/getStacLandingPage.json";
                FileSystem fileSystem = vertx.fileSystem();
                Buffer buffer = fileSystem.readFileBlocking(jsonFilePath);
                JsonObject stacMetadata = new JsonObject(buffer.toString());
                if (jsonResult.getJsonArray("temporal") == null
                    || jsonResult.getJsonArray("temporal").isEmpty()) {
                  jsonResult.put("temporal", new JsonArray().add(null).add(null));
                }
                if (jsonResult.getString("license") == null
                    || jsonResult.getString("license").isEmpty()) {
                  jsonResult.put("license", stacMetadata.getString("stacLicense"));
                }
                String stacVersion = stacMetadata.getString("stacVersion");
                jsonResult
                    .put("type", "Collection")
                    .put(
                        "links",
                        new JsonArray()
                            .add(createLink("root", STAC + "/", null))
                            .add(createLink("parent", STAC + "/", null))
                            .add(
                                createLink(
                                    "self",
                                    STAC + "/" + COLLECTIONS + "/" + jsonResult.getString("id"),
                                    jsonResult.getString("title"))))
                    .put("stac_version", stacVersion)
                    .put(
                        "extent",
                        new JsonObject()
                            .put(
                                "spatial",
                                new JsonObject()
                                    .put(
                                        "bbox",
                                        new JsonArray().add(jsonResult.getJsonArray("bbox"))))
                            .put(
                                "temporal",
                                new JsonObject()
                                    .put(
                                        "interval",
                                        new JsonArray().add(jsonResult.getJsonArray("temporal")))));
                if (jsonResult.containsKey("assets")) {
                  JsonObject assets = new JsonObject();

                  jsonResult
                      .getJsonArray("assets")
                      .forEach(
                          assetJson -> {
                            JsonObject asset = new JsonObject();
                            asset.mergeIn((JsonObject) assetJson);
                            String href =
                                hostName + ogcBasePath + "assets/" + asset.getString("id");
                            asset.put("href", href);
                            asset.put("file:size", asset.getInteger("size"));
                            asset.remove("size");
                            asset.remove("id");
                            asset.remove("stac_collections_id");
                            assets.put(((JsonObject) assetJson).getString("id"), asset);
                          });
                  jsonResult.put("assets", assets);
                }

                jsonResult.remove("bbox");
                jsonResult.remove("temporal");
              } catch (Exception e) {
                LOGGER.error("Something went wrong here: {}", e.getMessage());
                routingContext.put(
                    "response",
                    new OgcException(500, "Internal Server Error", "Something " + "broke")
                        .getJson()
                        .toString());
                routingContext.put("statusCode", 500);
                routingContext.next();
              }
              routingContext.put("response", jsonResult.toString());
              routingContext.put("statusCode", 200);
              routingContext.next();
            })
        .onFailure(
            failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", ((OgcException) failed).getStatusCode());
              } else {
                OgcException ogcException =
                    new OgcException(500, "Internal Server Error", "Internal Server Error");
                routingContext.put("response", ogcException.getJson().toString());
                routingContext.put("statusCode", ogcException.getStatusCode());
              }
              routingContext.next();
            });
  }

  public void getAssets(RoutingContext routingContext) {
    String assetId = routingContext.pathParam("assetId");
    if (!(Boolean) routingContext.get("isAuthorised")) {
      routingContext.next();
      return;
    }
    HttpServerResponse response = routingContext.response();
    response.setChunked(true);
    dbService
        .getAssets(assetId)
        .onSuccess(
            handler -> {
              response.putHeader("Content-Type", handler.getString("type"));
              DataFromS3 dataFromS3 =
                  new DataFromS3(httpClient, S3_BUCKET, S3_REGION, S3_ACCESS_KEY, S3_SECRET_KEY);
              String urlString =
                  dataFromS3.getFullyQualifiedUrlString(handler.getString("href"));
              dataFromS3.setUrlFromString(urlString);
              dataFromS3.setSignatureHeader(HttpMethod.GET);
              dataFromS3
                  .getDataFromS3(HttpMethod.GET)
                  .onSuccess(success -> success.pipeTo(response))
                  .onFailure(
                      failed -> {
                        if (failed instanceof OgcException) {
                          routingContext.put(
                              "response", ((OgcException) failed).getJson().toString());
                          routingContext.put("statusCode", 404);
                        } else {
                          routingContext.put(
                              "response",
                              new OgcException(
                                      500, "Internal Server Error", "Internal Server Error")
                                  .getJson()
                                  .toString());
                          routingContext.put("statusCode", 500);
                        }
                        routingContext.next();
                      });
            })
        .onFailure(
            failed -> {
              if (failed instanceof OgcException) {
                routingContext.put("response", ((OgcException) failed).getJson().toString());
                routingContext.put("statusCode", ((OgcException) failed).getStatusCode());
              } else {
                OgcException ogcException =
                    new OgcException(500, "Internal Server Error", "Internal Server Error");
                routingContext.put("response", ogcException.getJson().toString());
                routingContext.put("statusCode", ogcException.getStatusCode());
              }
              routingContext.next();
            });
  }

  private JsonObject createLink(String rel, String href, String title) {
    JsonObject link =
        new JsonObject()
            .put("rel", rel)
            .put("href", hostName + ogcBasePath + href)
            .put("type", "application/json");

    if (title != null) {
      link.put("title", title);
    }

    return link;
  }

  private JsonObject buildTileSetResponse(
      String collectionId,
      String tileMatrixSet,
      String tileMatrixSetUri,
      String crs,
      String dataType) {
    // templated URL example
    // =
    // https://ogc.iud.io/collections/{collectionId}/tiles/{tileMatrixSet}//{tileMatrix}/{tileRow}/{tileCol}
    String templatedTileUrl =
        hostName + ogcBasePath + COLLECTIONS + "/" + collectionId + "/map/tiles/" + tileMatrixSet
            + "/{tileMatrix}/{tileRow}/{tileCol}.png";
    JsonArray linkObject = new JsonArray().add(new JsonObject()
                    .put("href", hostName + ogcBasePath + COLLECTIONS + "/" + collectionId + "/map/tiles")
                    .put("rel", "self")
                    .put("type", "application/json")
                    .put("title", collectionId.concat(" tileset tiled using" + tileMatrixSet)))
            .add(new JsonObject().put("href",
                        "https://raw.githubusercontent.com/opengeospatial/2D-Tile-Matrix-Set/master/registry/json"
                            + "/WebMercatorQuad.json")
                    .put("rel", "http://www.opengis.net/def/rel/ogc/1.0/tiling-scheme")
                    .put("type", "application/json")
                    .put("title", "Definition of " + tileMatrixSet + " TileMatrixSet"))
            .add(
                new JsonObject()
                    .put("href", templatedTileUrl)
                    .put("templated", true)
                    .put("rel", "item")
                    .put("type", "image/png")
                    .put("title", "Templated link for retrieving the tiles in PNG"));
    return new JsonObject()
        .put("tileMatrixSetURI", tileMatrixSetUri)
        .put("dataType", dataType)
        .put("crs", crs)
        .put("links", linkObject);
  }

  private JsonObject buildTileMatrices(
      String id,
      String title,
      String scaleDenominator,
      String cellSize,
      String tileWidth,
      String tileHeight,
      String matrixWidth,
      String matrixHeight) {

        return new JsonObject()
            .put("title", title)
            .put("id", id)
            .put("scaleDenominator", scaleDenominator)
            .put("cellSize", cellSize)
            .put("tileWidth", tileWidth)
            .put("tileHeight", tileHeight)
            .put("matrixWidth", matrixWidth)
            .put("matrixHeight", matrixHeight);
  }

  /**
   * Add this to a route's handler chain to audit the API call once the API response has been
   * completely sent. The API is audited <b> only if the response was successful i.e. 2xx status
   * code</b>.
   * 
   * @param context the routing context
   */
  public void auditAfterApiEnded(RoutingContext context) {
    context.addBodyEndHandler(v -> updateAuditTable(context));
    context.next();
  }
  
  private Future<Void> updateAuditTable(RoutingContext context) {
    final List<Integer> STATUS_CODES_TO_AUDIT = List.of(200, 201);
    
    if(!STATUS_CODES_TO_AUDIT.contains(context.response().getStatusCode())) {
      return Future.succeededFuture();
    }
    
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    
    String resourceId = authInfo.getString(ID);
    
    // auditing never done for root path ('/') so [1] will always be there 
    String apiEndpointFirstPart = context.request().path().split("/")[1];
    
    // if assets API, then the ID in the path is NOT the resource ID - take resource ID from token iid
    if("assets".equals(apiEndpointFirstPart)) {
      resourceId = authInfo.getString("iid");
    }
    
    Promise<Void> promise = Promise.promise();
    JsonObject request = new JsonObject();
    
    JsonObject reqBody = context.body().asJsonObject();
    request.put(REQUEST_JSON, reqBody != null ? reqBody : new JsonObject());
    
    catalogueService
        .getCatItem(resourceId)
        .onComplete(
            relHandler -> {
              if (relHandler.succeeded()) {
                JsonObject cacheResult = relHandler.result();
                // Comment here , if we need type (item_type) then we can use this
                /*String type =
                cacheResult.containsKey(RESOURCE_GROUP) ? "RESOURCE" : "RESOURCE_GROUP";*/
                String resourceGroup =
                    cacheResult.containsKey(RESOURCE_GROUP)
                        ? cacheResult.getString(RESOURCE_GROUP)
                        : cacheResult.getString(ID);
                ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                String role = authInfo.getString(ROLE);
                String drl = authInfo.getString(DRL);
                if (role.equalsIgnoreCase("delegate") && drl != null) {
                  request.put(DELEGATOR_ID, authInfo.getString(DID));
                } else {
                  request.put(DELEGATOR_ID, authInfo.getString("userId"));
                }
                String providerId = cacheResult.getString("provider");
                long time = zst.toInstant().toEpochMilli();
                String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();

                request.put(RESOURCE_GROUP, resourceGroup);
                /*request.put(TYPE_KEY, type);*/
                // Comment here , if we need type (item_type) then we can use this
                request.put(EPOCH_TIME, time);
                request.put(ISO_TIME, isoTime);
                request.put(USER_ID, authInfo.getValue("userId"));
                request.put(ID, authInfo.getValue(ID));
                request.put(API, context.request().path());
                request.put(RESPONSE_SIZE, context.response().bytesWritten());
                request.put(PROVIDER_ID, providerId);
                meteringService
                    .insertMeteringValuesInRmq(request)
                    .onComplete(
                        handler -> {
                          if (handler.succeeded()) {
                            LOGGER.debug("message published in RMQ.");
                            promise.complete();
                          } else {
                            LOGGER.error("failed to publish message in RMQ.");
                            promise.complete();
                          }
                        });
              } else {
                LOGGER.debug("Item not found and failed to call metering service");
              }
            });

    return promise.future();
  }

  public void getSummary(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    LOGGER.trace("Info: getSummary Started.");
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    authInfo.put(STARTT, request.getParam(STARTT));
    authInfo.put(ENDT, request.getParam(ENDT));
    JsonObject responseJson =
        new JsonObject().put("type", "urn:dx:rs:success").put("title", "success");
    meteringService
        .summaryOverview(authInfo)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Successful");
                responseJson.put("results", handler.result().getJsonArray("result"));
                routingContext.put("response", responseJson.toString());
                routingContext.put("statusCode", 200);
                routingContext.next();
              } else {
                LOGGER.error("Fail: Bad request "+ handler.cause().getMessage());
                if (handler.cause() instanceof OgcException) {
                  routingContext.put("response", ((OgcException) handler.cause()).getJson().toString());
                  routingContext.put("statusCode", 400);
                } else {
                  routingContext.put(
                          "response",
                          new OgcException(500, "Internal Server Error", "Internal Server Error")
                                  .getJson()
                                  .toString());
                  routingContext.put("statusCode", 500);
                }
                routingContext.next();
              }
            });
  }

  public void getMonthlyOverview(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    LOGGER.trace("Info: getMonthlyOverview Started." + routingContext.data().get("authInfo"));
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    authInfo.put(STARTT, request.getParam(STARTT));
    authInfo.put(ENDT, request.getParam(ENDT));
    JsonObject responseJson =
        new JsonObject().put("type", "urn:dx:rs:success").put("title", "success");
    meteringService
        .monthlyOverview(authInfo)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Successful");
                responseJson.put("results", handler.result().getJsonArray("result"));
                routingContext.put("response", responseJson.toString());
                routingContext.put("statusCode", 200);
                routingContext.next();
              } else {
                LOGGER.error("Fail: Bad request "+ handler.cause().getMessage());
                if (handler.cause() instanceof OgcException) {
                  routingContext.put("response", ((OgcException) handler.cause()).getJson().toString());
                  routingContext.put("statusCode", 400);
                } else {
                  routingContext.put(
                          "response",
                          new OgcException(500, "Internal Server Error", "Internal Server Error")
                                  .getJson()
                                  .toString());
                  routingContext.put("statusCode", 500);
                }
                routingContext.next();
              }
            });
  }

  public void getProviderAuditDetail(RoutingContext routingContext) {
    LOGGER.trace("Info: getProviderAuditDetail Started.");
    JsonObject entries = new JsonObject();
    JsonObject provider = (JsonObject) routingContext.data().get("authInfo");
    AuthInfo authInfo = routingContext.get(USER_KEY);
    if (authInfo.isRsToken()) {
      provider.put("iid", config().getString("audience"));
    }
    HttpServerRequest request = routingContext.request();
    RequestParameters paramsFromOasValidation =
        routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    int limit = paramsFromOasValidation.queryParameter(LIMITPARAM).getInteger();
    int offset = paramsFromOasValidation.queryParameter(OFFSETPARAM).getInteger();

    entries.put("endPoint", provider.getString("apiEndpoint"));
    entries.put("userid", provider.getString("userid"));
    entries.put("iid", provider.getString("iid"));
    entries.put("startTime", request.getParam("time"));
    entries.put("endTime", request.getParam("endTime"));
    entries.put("timeRelation", request.getParam("timerel"));
    entries.put("providerId", request.getParam("providerId"));
    entries.put("consumerID", request.getParam("consumer"));
    entries.put("resourceId", request.getParam("id"));
    entries.put("api", request.getParam("api"));
    entries.put("options", request.headers().get("options"));
    entries.put("offset", offset);
    entries.put("limit", limit);

    LOGGER.debug(entries);
    Promise<JsonObject> promise = Promise.promise();
    JsonObject responseJson =
        new JsonObject().put("type", "urn:dx:rs:success").put("title", "success");
    meteringService
        .executeReadQuery(entries)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Table Reading Done.");
                responseJson.put("results", handler.result().getJsonArray("result"));
                promise.complete(responseJson);
                routingContext.put("response", responseJson.toString());
                routingContext.put("statusCode", 200);
                routingContext.next();
              } else {
                LOGGER.error("Table reading failed.");
                if (handler.cause() instanceof OgcException) {
                  routingContext.put("response", ((OgcException) handler.cause()).getJson().toString());
                  routingContext.put("statusCode", 400);
                } else {
                  routingContext.put(
                          "response",
                          new OgcException(500, "Internal Server Error", "Internal Server Error")
                                  .getJson()
                                  .toString());
                  routingContext.put("statusCode", 500);
                }
                routingContext.next();
              }
            });
    promise.future();
  }

  public void getConsumerAuditDetail(RoutingContext routingContext) {
    LOGGER.trace("Info: getConsumerAuditDetail Started.");
    JsonObject entries = new JsonObject();
    JsonObject consumer = (JsonObject) routingContext.data().get("authInfo");
    HttpServerRequest request = routingContext.request();
    RequestParameters paramsFromOasValidation =
        routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    int limit = paramsFromOasValidation.queryParameter(LIMITPARAM).getInteger();
    int offset = paramsFromOasValidation.queryParameter(OFFSETPARAM).getInteger();

    entries.put("userid", consumer.getString("userid"));
    entries.put("endPoint", consumer.getString("apiEndpoint"));
    entries.put("startTime", request.getParam("time"));
    entries.put("endTime", request.getParam("endTime"));
    entries.put("timeRelation", request.getParam("timerel"));
    entries.put("options", request.headers().get("options"));
    entries.put("resourceId", request.getParam("id"));
    entries.put("api", request.getParam("api"));
    entries.put("offset", offset);
    entries.put("limit", limit);

    LOGGER.debug(entries);
    JsonObject responseJson =
        new JsonObject().put("type", "urn:dx:rs:success").put("title", "success");
    meteringService
        .executeReadQuery(entries)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Table Reading Done.");
                LOGGER.debug(handler);
                if(handler.result().getJsonArray("result")==null) {
                  responseJson.put("results", "0");
                  routingContext.put("response", responseJson.toString());
                  routingContext.put("statusCode", 204);
                  routingContext.next();
                  return;
                }
                responseJson.put("results", handler.result().getJsonArray("result"));
                routingContext.put("response", responseJson.toString());
                routingContext.put("statusCode", 200);
                routingContext.next();
              } else {
                LOGGER.error("Table reading failed.");
                if (handler.cause() instanceof OgcException) {
                  routingContext.put("response", ((OgcException) handler.cause()).getJson().toString());
                  routingContext.put("statusCode", 400);
                } else {
                  routingContext.put(
                          "response",
                          new OgcException(500, "Internal Server Error", "Internal Server Error")
                                  .getJson()
                                  .toString());
                  routingContext.put("statusCode", 500);
                }
                routingContext.next();
              }
            });
  }
}
