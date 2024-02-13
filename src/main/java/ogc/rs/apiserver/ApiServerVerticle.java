package ogc.rs.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static ogc.rs.apiserver.util.Constants.*;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.DEFAULT_SERVER_CRS;

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
    private Router router;
    private String ogcBasePath;
    private String hostName;
    private DatabaseService dbService;

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

      Set<String> allowedHeaders = Set.of(HEADER_TOKEN, HEADER_CONTENT_LENGTH, HEADER_CONTENT_TYPE, HEADER_HOST
          ,HEADER_ORIGIN, HEADER_REFERER, HEADER_ACCEPT, HEADER_ALLOW_ORIGIN);

      Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.OPTIONS);


      /* Get base paths from config */
      ogcBasePath = config().getString("ogcBasePath");
      hostName = config().getString("hostName");

      Future<RouterBuilder> routerBuilderFut = RouterBuilder.create(vertx, "docs/openapiv3_0.yaml");
      Future<RouterBuilder> routerBuilderStacFut =
        RouterBuilder.create(vertx, "docs/stacopenapiv3_0.yaml");
      CompositeFuture.all(routerBuilderFut, routerBuilderStacFut)
        .compose(
            result -> {
              RouterBuilder routerBuilder = result.resultAt(0);
              RouterBuilder routerBuilderStac = result.resultAt(1);

          LOGGER.debug("Info: Mounting routes from OpenApi3 spec");

          RouterBuilderOptions factoryOptions =
                  new RouterBuilderOptions().setMountResponseContentTypeHandler(true);

          routerBuilder.rootHandler(CorsHandler.create("*").allowedHeaders(allowedHeaders)
                  .allowedMethods(allowedMethods));
          routerBuilder.rootHandler(BodyHandler.create());
          routerBuilderStac.rootHandler(CorsHandler.create("*").allowedHeaders(allowedHeaders)
                        .allowedMethods(allowedMethods));
          routerBuilderStac.rootHandler(BodyHandler.create());
          try {
          routerBuilder
              .operation(LANDING_PAGE)
              .handler(
                  routingContext -> {
                      HttpServerResponse response = routingContext.response();
                      response.sendFile("docs/landingPage.json");
                  });
          routerBuilder
              .operation(CONFORMANCE_CLASSES)
              .handler(
                  routingContext -> {
                      HttpServerResponse response = routingContext.response();
                      response.sendFile("docs/conformance.json");
                  });

          routerBuilder
              .operation(COLLECTIONS_API)
              .handler(this::getCollections)
              .handler(this::putCommonResponseHeaders)
              .handler(this::buildResponse);

          routerBuilder
              .operation(COLLECTION_API)
              .handler(this::getCollection)
              .handler(this::putCommonResponseHeaders)
              .handler(this::buildResponse);

          routerBuilder
              .operation(FEATURES_API)
             // .handler(AuthHandler.create(vertx))
              .handler(this::getFeatures)
              .handler(this::putCommonResponseHeaders)
              .handler(this::buildResponse);


          routerBuilder
              .operation(FEATURE_API)
          //    .handler(AuthHandler.create(vertx))
              .handler(this::getFeature)
              .handler(this::putCommonResponseHeaders)
              .handler(this::buildResponse);

                routerBuilderStac
                    .operation("getStacLandingPage")
                    .handler(this::stacCatalog)
                    .handler(this::putCommonResponseHeaders)
                    .handler(this::buildResponse);

                routerBuilderStac
                    .operation("getStacCollections")
                    .handler(this::stacCollections)
                    .handler(this::putCommonResponseHeaders)
                    .handler(this::buildResponse);

                routerBuilderStac
                    .operation("getConformanceDeclaration")
                    .handler(
                        routingContext -> {
                          HttpServerResponse response = routingContext.response();
                          response.sendFile("docs/conformance.json");
                        });

                Router ogcRouter = routerBuilder.createRouter();
                Router stacRouter = routerBuilderStac.createRouter();
                router = Router.router(vertx);
                router.route("/*").subRouter(stacRouter);
                router.route("/*").subRouter(ogcRouter);

                router
                    .get(OPENAPI_SPEC)
                    .handler(
                        routingContext -> {
                          HttpServerResponse response = routingContext.response();
                          response.sendFile("docs/openapiv3_0.json");
                        });
              } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
              }

          dbService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
          // TODO: ssl configuration
          HttpServerOptions serverOptions = new HttpServerOptions();
          serverOptions.setCompressionSupported(true).setCompressionLevel(5);
          int port = config().getInteger("httpPort") == null ? 8080 : config().getInteger("httpPort");

          HttpServer server = vertx.createHttpServer(serverOptions);
          return server.requestHandler(router).listen(port);
          }).onSuccess(success -> LOGGER.info("Started HTTP server at port:" + success.actualPort()))
          .onFailure(Throwable::printStackTrace);;
    }

  private void getFeature(RoutingContext routingContext) {

    String collectionId = routingContext.pathParam("collectionId");
//    if (!(Boolean) routingContext.get("isAuthorised")){
//      routingContext.next();
//      return;
//    }
    String featureId = routingContext.pathParam("featureId");
    System.out.println("collectionId- " + collectionId + " featureId- " + featureId);
    Map<String, String> queryParamsMap = new HashMap<>();
    MultiMap queryParams = routingContext.queryParams();
    queryParams.forEach(param -> queryParamsMap.put(param.getKey(), param.getValue()));
    LOGGER.debug("<APIServer> QP- {}",queryParamsMap);
    Future<Map<String, Integer>> isCrsValid = dbService.isCrsValid(collectionId, queryParamsMap);
    isCrsValid
        .compose(crs -> dbService.getFeature(collectionId, featureId, queryParamsMap, crs))
        .onSuccess(success -> {
          LOGGER.debug("Success! - {}", success.encodePrettily());
          // TODO: Add base_path from config
          success.put("links", new JsonArray()
              .add(new JsonObject()
                  .put("href", hostName + ogcBasePath + COLLECTIONS + "/" + collectionId + "/items/" + featureId)
                  .put("rel", "self")
                  .put("type", "application/geo+json"))
              .add(new JsonObject()
                  .put("href", hostName + ogcBasePath + COLLECTIONS + "/" + collectionId)
                  .put("rel", "collection")
                  .put("type", "application/json")));
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

  private void getFeatures(RoutingContext routingContext) {
    String collectionId = routingContext.pathParam("collectionId");
//    if (!(Boolean) routingContext.get("isAuthorised")){
//      routingContext.next();
//      return;
//    }
    Map<String, String> queryParamsMap = new HashMap<>();
    MultiMap queryParams = routingContext.queryParams();
    queryParams.forEach(param -> queryParamsMap.put(param.getKey(), param.getValue()));
    LOGGER.debug("<APIServer> QP- {}",queryParamsMap);
    Future<Map<String, Integer>> isCrsValid = dbService.isCrsValid(collectionId, queryParamsMap);
    isCrsValid
        .compose(crsss -> dbService.matchFilterWithProperties(collectionId, queryParamsMap))
//    Future<Void> matchFiltersWithProperties = dbService.matchFilterWithProperties(collectionId, queryParamsMap);
//    matchFiltersWithProperties
        .compose(datetimeCheck -> {
          try {
            String datetime;
            ZonedDateTime zone, zone2;
            DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
            if (queryParamsMap.containsKey("datetime")) {
              datetime =  queryParamsMap.get("datetime");
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
            OgcException ogcException = new OgcException(500, "Internal Server Error", "Internal Server Error");
            return Future.failedFuture(ogcException);
          } catch (DateTimeParseException dtpe) {
            OgcException ogcException = new OgcException(400, "Bad Request", "Time parameter not in ISO format");
            return Future.failedFuture(ogcException);
          }
          return Future.succeededFuture();
        })
        .compose(dbCall -> dbService.getFeatures(collectionId, queryParamsMap, isCrsValid.result()))
        .onSuccess(success -> {
          LOGGER.debug("Success! - {}", success.encodePrettily());
          // TODO: Add base_path from config
          int next, offset=1, limit=10;
          if (queryParamsMap.containsKey("offset")){
            offset = Integer.parseInt(queryParamsMap.get("offset"));
          }
          if (queryParamsMap.containsKey("limit")){
            limit = Math.min(10000, Integer.parseInt(queryParamsMap.get("limit")));
          }
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

  private void buildResponse(RoutingContext routingContext) {
      routingContext.response().setStatusCode(routingContext.get("statusCode"))
          .end((String) routingContext.get("response"));
  }

  private void getCollection(RoutingContext routingContext) {
      String collectionId = routingContext.pathParam("collectionId");
      LOGGER.debug("collectionId- {}", collectionId);
      dbService.getCollection(collectionId)
          .onSuccess(success -> {
            LOGGER.debug("Success! - {}", success.toString());
            JsonObject jsonResult = buildCollectionResult(success);
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

  private void getCollections(RoutingContext routingContext) {

    dbService.getCollections()
        .onSuccess(success -> {
          JsonArray collections  = new JsonArray();
          success.forEach(collection -> {
                        try {
                            JsonObject json;
                            List<JsonObject> tempArray = new ArrayList<>();
                            tempArray.add(collection);
                            json = buildCollectionResult(tempArray);
                            collections.add(json);
                        } catch (Exception e) {
                            LOGGER.error("Something went wrong here: {}", e.getMessage());
                            routingContext.put("response", new OgcException(500, "Internal Server Error", "Something " +
                                "broke").getJson().toString());
                            routingContext.put("statusCode", 500);
                            routingContext.next();
                        }
                    });
          JsonObject featureCollections = new JsonObject()
              .put("links", new JsonArray()
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
          if (failed instanceof OgcException){
            routingContext.put("response",((OgcException) failed).getJson().toString());
            routingContext.put("statusCode", 404);
          }
          else{
            routingContext.put("response",
                new OgcException(500, "Internal Server Error", "Internal Server Error").getJson().toString());
            routingContext.put("statusCode", 500);
          }
          routingContext.next();
        });
  }
  private void putCommonResponseHeaders(RoutingContext routingContext) {
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

  private JsonObject buildCollectionResult(List<JsonObject> success) {
    JsonObject collection = success.get(0);
    JsonObject extent = new JsonObject();
    collection.put("properties", new JsonObject());
    if (collection.getString("datetime_key") != null && !collection.getString("datetime_key").isEmpty() )
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
        .put("itemType", "feature");
    collection.remove("title");
    collection.remove("description");
    collection.remove("datetime_key");
    collection.remove("bbox");
    collection.remove("temporal");
    return collection;
  }

  private void stacCollections(RoutingContext routingContext) {
    dbService
        .getStacCollections()
        .onSuccess(
            success -> {
              JsonArray collections = new JsonArray();
              success.forEach(
                  collection -> {
                    try {
                      JsonObject json;
                      List<JsonObject> tempArray = new ArrayList<>();
                      tempArray.add(collection);
                      String jsonFilePath = "docs/getStacLandingPage.json";
                      FileSystem fileSystem = vertx.fileSystem();
                      Buffer buffer = fileSystem.readFileBlocking(jsonFilePath);
                      JsonObject stacMetadata = new JsonObject(buffer.toString());
                      String stacVersion = stacMetadata.getString("stacVersion");
                      JsonObject singleCollection = tempArray.get(0);
                      if (singleCollection.getString("license") == null
                          || collection.getString("license").isEmpty()) {
                        singleCollection.put("license", stacMetadata.getString("stacLicense"));
                      }
                      singleCollection
                          .put("type", "Collection")
                          .put(
                              "links",
                              new JsonArray()
                                  .add(createLink("root", STAC + "/", null))
                                  .add(createLink("parent", STAC + "/", null))
                                  .add(
                                      createLink(
                                          "self",
                                          STAC
                                              + "/"
                                              + COLLECTIONS
                                              + "/"
                                              + collection.getString("id"),
                                          collection.getString("title")))
                                  .add(
                                      createLink(
                                          "items",
                                          STAC
                                              + "/"
                                              + COLLECTIONS
                                              + "/"
                                              + collection.getString("id")
                                              + "/"
                                              + ITEMS,
                                          collection.getString("title"))))
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
                                                  .add(
                                                      new JsonArray()
                                                          .add(
                                                              collection.getJsonArray(
                                                                  "temporal"))))));
                      singleCollection.remove("bbox");
                      singleCollection.remove("temporal");
                      collections.add(singleCollection);
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
              routingContext.put("response", collections.toString());
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

  private void stacCatalog(RoutingContext routingContext) {
    try {
      String jsonFilePath = "docs/getStacLandingPage.json";
      FileSystem fileSystem = vertx.fileSystem();
      Buffer buffer = fileSystem.readFileBlocking(jsonFilePath);
      JsonObject stacLandingPage = new JsonObject(buffer.toString());

      String type = stacLandingPage.getString("type");
      String description = stacLandingPage.getString("description");
      String title = stacLandingPage.getString("title");
      String stacVersion = stacLandingPage.getString("stacVersion");
      String catalogId = config().getString("catalogId");

      JsonArray links =
          new JsonArray()
              .add(createLink("root", STAC, title))
              .add(createLink("self", STAC, title))
              .add(createLink("child", STAC + "/" + COLLECTIONS + "/{collectionId}", null));

      JsonObject catalog =
          new JsonObject()
              .put("type", type)
              .put("description", description)
              .put("id", catalogId)
              .put("stac_version", stacVersion)
              .put("links", links);

      routingContext.put("response", catalog.encode());
      routingContext.put("statusCode", 200);
      routingContext.next();
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
}
