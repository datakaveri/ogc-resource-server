package ogc.rs.apiserver;

import static ogc.rs.apiserver.util.Constants.COLLECTIONS;
import static ogc.rs.apiserver.util.Constants.COLLECTIONS_API;
import static ogc.rs.apiserver.util.Constants.COLLECTION_API;
import static ogc.rs.apiserver.util.Constants.CONFORMANCE_CLASSES;
import static ogc.rs.apiserver.util.Constants.FEATURES_API;
import static ogc.rs.apiserver.util.Constants.FEATURE_API;
import static ogc.rs.apiserver.util.Constants.HEADER_ACCEPT;
import static ogc.rs.apiserver.util.Constants.HEADER_ALLOW_ORIGIN;
import static ogc.rs.apiserver.util.Constants.HEADER_CONTENT_LENGTH;
import static ogc.rs.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static ogc.rs.apiserver.util.Constants.HEADER_HOST;
import static ogc.rs.apiserver.util.Constants.HEADER_ORIGIN;
import static ogc.rs.apiserver.util.Constants.HEADER_REFERER;
import static ogc.rs.apiserver.util.Constants.HEADER_TOKEN;
import static ogc.rs.apiserver.util.Constants.LANDING_PAGE;
import static ogc.rs.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static ogc.rs.apiserver.util.Constants.OPENAPI_SPEC;
import static ogc.rs.apiserver.util.Constants.PROCESSES_API;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ogc.rs.apiserver.handlers.FailureHandler;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The OGC Resource Server API Verticle.
 *
 * <h1>OGC Resource Server API Verticle</h1>
 *
 * <p>The API Server verticle implements the OGC Resource Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @version 1.0
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @since 2023-06-12
 */
public class ApiServerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);
  private Router router;
  private String ogcBasePath;
  private String hostName;
  private DatabaseService dbService;


  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster/single instance, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443 or an HTTP server at port 8080.
   *
   * @throws Exception which is a startup exception TODO Need to add documentation for all the
   */
  @Override
  public void start() throws Exception {

    Set<String> allowedHeaders =
      Set.of(HEADER_TOKEN, HEADER_CONTENT_LENGTH, HEADER_CONTENT_TYPE, HEADER_HOST, HEADER_ORIGIN,
        HEADER_REFERER, HEADER_ACCEPT, HEADER_ALLOW_ORIGIN);

    Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.OPTIONS);
    FailureHandler failureHandler = new FailureHandler();


    /* Get base paths from config */
    ogcBasePath = config().getString("ogcBasePath");
    hostName = config().getString("hostName");
    Future<RouterBuilder> routerBuilderFut = RouterBuilder.create(vertx, "docs/openapiv3_0.yaml");
    routerBuilderFut.compose(routerBuilder -> {

        LOGGER.debug("Info: Mounting routes from OpenApi3 spec");

        RouterBuilderOptions factoryOptions =
          new RouterBuilderOptions().setMountResponseContentTypeHandler(true);

        routerBuilder.rootHandler(
          CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
        routerBuilder.rootHandler(BodyHandler.create());
        try {
          routerBuilder.operation(LANDING_PAGE).handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.sendFile("docs/landingPage.json");
          });
          routerBuilder.operation(CONFORMANCE_CLASSES).handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.sendFile("docs/conformance.json");
          });

          routerBuilder.operation(COLLECTIONS_API).handler(this::getCollections)
            .handler(this::putCommonResponseHeaders).handler(this::buildResponse);

          routerBuilder.operation(COLLECTION_API).handler(this::getCollection)
            .handler(this::putCommonResponseHeaders).handler(this::buildResponse);

          routerBuilder.operation(FEATURES_API)
            // .handler(AuthHandler.create(vertx))
            .handler(this::getFeatures).handler(this::putCommonResponseHeaders)
            .handler(this::buildResponse);

          routerBuilder.operation(PROCESSES_API)
            // .handler(AuthHandler.create(vertx))
            .handler(this::getProcesses).handler(this::putCommonResponseHeaders)
            .handler(this::buildResponse).failureHandler(failureHandler);


          routerBuilder.operation(FEATURE_API)
            //    .handler(AuthHandler.create(vertx))
            .handler(this::getFeature).handler(this::putCommonResponseHeaders)
            .handler(this::buildResponse);


          router = routerBuilder.createRouter();
          router.get(OPENAPI_SPEC).handler(routingContext -> {
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
      .onFailure(Throwable::printStackTrace);
  }

  private void getFeature(RoutingContext routingContext) {

    String collectionId = routingContext.pathParam("collectionId");
//    if (!(Boolean) routingContext.get("isAuthorised")){
//      routingContext.next();
//      return;
//    }
    String featureId = routingContext.pathParam("featureId");
    System.out.println("collectionId- " + collectionId + " featureId- " + featureId);
    dbService.getFeature(collectionId, featureId).onSuccess(success -> {
      LOGGER.debug("Success! - {}", success.encodePrettily());
      // TODO: Add base_path from config
      routingContext.put("response", success.toString());
      routingContext.put("statusCode", 200);
      routingContext.next();
    }).onFailure(failed -> {
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

  private void getProcesses(RoutingContext routingContext) {
    int limit = Integer.parseInt(routingContext.queryParams().get("limit"));

    dbService.getProcesses(limit).onSuccess(successResult -> {
      routingContext.put("response", successResult.toString());
      routingContext.put("statusCode", 200);
      routingContext.next();
    }).onFailure(failed -> {
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

  private void getFeatures(RoutingContext routingContext) {
    String collectionId = routingContext.pathParam("collectionId");
//    if (!(Boolean) routingContext.get("isAuthorised")){
//      routingContext.next();
//      return;
//    }
    Map<String, String> queryParamsMap = new HashMap<>();
    try {
      String datetime;
      MultiMap queryParams = routingContext.queryParams();
      queryParams.forEach(param -> queryParamsMap.put(param.getKey(), param.getValue()));
      ZonedDateTime zone;
      DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
      if (queryParamsMap.containsKey("datetime")) {
        datetime = queryParamsMap.get("datetime");
        if (!datetime.contains("/")) {
          zone = ZonedDateTime.parse(datetime, formatter);
        } else if (datetime.contains("/")) {
          String[] dateTimeArr = datetime.split("/");
          if (dateTimeArr[0].equals("..")) { // -- before
            zone = ZonedDateTime.parse(dateTimeArr[1], formatter);
          } else if (dateTimeArr[1].equals("..")) { // -- after
            zone = ZonedDateTime.parse(dateTimeArr[0], formatter);
          } else {
            zone = ZonedDateTime.parse(dateTimeArr[0], formatter);
            zone = ZonedDateTime.parse(dateTimeArr[1], formatter);
          }
        }
      }
    } catch (NullPointerException ne) {
      OgcException ogcException =
        new OgcException(500, "Internal Server Error", "Internal Server Error");
      routingContext.put("response", ogcException.getJson().toString());
      routingContext.put("statusCode", ogcException.getStatusCode());
      routingContext.next();
      return;
    } catch (DateTimeParseException dtpe) {
      OgcException ogcException =
        new OgcException(400, "Bad Request", "Time parameter not in ISO format");
      routingContext.put("response", ogcException.getJson().toString());
      routingContext.put("statusCode", ogcException.getStatusCode());
      routingContext.next();
      return;
    }
    System.out.println("<APIServer> QP- " + queryParamsMap);
    dbService.getFeatures(collectionId, queryParamsMap).onSuccess(success -> {
      LOGGER.debug("Success! - {}", success.encodePrettily());
      // TODO: Add base_path from config
      routingContext.put("response", success.toString());
      routingContext.put("statusCode", 200);
      routingContext.next();
    }).onFailure(failed -> {
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

  private void buildResponse(RoutingContext routingContext) {
    routingContext.response().setStatusCode(routingContext.get("statusCode"))
      .end((String) routingContext.get("response"));
  }

  private void getCollection(RoutingContext routingContext) {
    String collectionId = routingContext.pathParam("collectionId");
    LOGGER.debug("collectionId- {}", collectionId);
    dbService.getCollection(collectionId).onSuccess(success -> {
      LOGGER.debug("Success! - {}", success.toString());
      JsonObject jsonResult = buildCollectionResult(success);
      routingContext.put("response", jsonResult.toString());
      routingContext.put("statusCode", 200);
      routingContext.next();
    }).onFailure(failed -> {
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

  private void getCollections(RoutingContext routingContext) {

    dbService.getCollections().onSuccess(success -> {
      JsonArray collections = new JsonArray();
      success.forEach(collection -> {
        try {
          JsonObject json;
          List<JsonObject> tempArray = new ArrayList<>();
          tempArray.add(collection);
          json = buildCollectionResult(tempArray);
          collections.add(json);
        } catch (Exception e) {
          LOGGER.error("Something went wrong here: {}", e.getMessage());
          routingContext.put("response",
            new OgcException(500, "Internal Server Error", "Something " + "broke").getJson()
              .toString());
          routingContext.put("statusCode", 500);
          routingContext.next();
        }
      });
      routingContext.put("response", collections.toString());
      routingContext.put("statusCode", 200);
      routingContext.next();
    }).onFailure(failed -> {
      if (failed instanceof OgcException) {
        routingContext.put("response", ((OgcException) failed).getJson().toString());
        routingContext.put("statusCode", 404);
      } else {
        routingContext.put("response",
          new OgcException(500, "Internal Server Error", "Internal Server Error").getJson()
            .toString());
        routingContext.put("statusCode", 500);
      }
      routingContext.next();
    });
  }

  private void putCommonResponseHeaders(RoutingContext routingContext) {
    routingContext.response().putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
      .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
      .putHeader("Pragma", "no-cache").putHeader("Expires", "0")
      .putHeader("X-Content-Type-Options", "nosniff");
    routingContext.next();
  }

  private JsonObject buildCollectionResult(List<JsonObject> success) {
    JsonObject collection = success.get(0);
    collection.put("properties", new JsonObject());
    if (collection.getString("datetime_key") != null &&
      !collection.getString("datetime_key").isEmpty()) {
      collection.getJsonObject("properties")
        .put("datetimeParameter", collection.getString("datetime_key"));
    }
    collection.put("links", new JsonArray().add(new JsonObject().put("href",
          hostName + ogcBasePath + COLLECTIONS + "/" + collection.getString("id")).put("rel", "self")
        .put("title", collection.getString("title"))
        .put("description", collection.getString("description"))).add(new JsonObject().put("href",
          hostName + ogcBasePath + COLLECTIONS + "/" + collection.getString("id") + "/items")
        .put("rel", "items").put("title", collection.getString("title"))
        .put("type", "application/geo+json")).add(new JsonObject().put("href",
          hostName + ogcBasePath + COLLECTIONS + "/" + collection.getString("id") +
            "/items/{featureId}").put("rel", "item")
        .put("title", "Link template for " + collection.getString("id") + " features")
        .put("templated", "true"))).put("itemType", "feature")
      .put("crs", new JsonArray().add("http://www.opengis.net/def/crs/ESPG/0/4326"));
    collection.remove("title");
    collection.remove("description");
    collection.remove("datetime_key");
    return collection;
  }
}
