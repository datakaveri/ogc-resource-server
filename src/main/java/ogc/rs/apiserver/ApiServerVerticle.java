package ogc.rs.apiserver;

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
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import static ogc.rs.apiserver.util.Constants.*;
import static ogc.rs.common.Constants.DATABASE_SERVICE_ADDRESS;

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

      Set<String> allowedHeaders = Set.of(HEADER_TOKEN, HEADER_CONTENT_LENGTH, HEADER_CONTENT_TYPE, HEADER_HOST
          ,HEADER_ORIGIN, HEADER_REFERER, HEADER_ACCEPT, HEADER_ALLOW_ORIGIN);

      Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.OPTIONS);


      /* Get base paths from config */
      ogcBasePath = config().getString("ogcBasePath");
      Future<RouterBuilder> routerBuilderFut = RouterBuilder.create(vertx, "docs/openapiv3_0.yaml");
      routerBuilderFut.compose(routerBuilder -> {

          LOGGER.debug("Info: Mounting routes from OpenApi3 spec");

          RouterBuilderOptions factoryOptions =
                  new RouterBuilderOptions().setMountResponseContentTypeHandler(true);

          routerBuilder.rootHandler(CorsHandler.create("*").allowedHeaders(allowedHeaders)
                  .allowedMethods(allowedMethods));
          routerBuilder.rootHandler(BodyHandler.create());
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

          /* routerBuilder
          *   .operation(FEATURES_API)
          *   .handler(this::getFeatures)
          *   .handler(this::putCommonResponseHeaders)
          *   .handler(this::buildResponse)
          * */

            /*  routerBuilder
             *  .operation(FEATURE_API)
             *  .handler(this::getFeatures)
             *  .handler(this::putCommonResponseHeaders)
             *  .handler(this::buildResponse)
             * */

          router = routerBuilder.createRouter();
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

  private void buildResponse(RoutingContext routingContext) {
      routingContext.response().setStatusCode(routingContext.get("status_code"))
          .end((String) routingContext.get("response"));
  }

  private void getCollection(RoutingContext routingContext) {
      // validation logic here?

      String collectionId = routingContext.pathParam("collectionId");
      LOGGER.debug("collectionId- {}", collectionId);
      dbService.getCollection(collectionId)
          .onSuccess(success -> {
            // write your success story
            LOGGER.debug("Success! - {}", success.encodePrettily());
              routingContext.put("response", success.toString());
              routingContext.put("status_code", 200);
           // }
            routingContext.next();
          })
          .onFailure(failed -> {
            // well, you tried
            if (failed instanceof OgcException){
              routingContext.put("response",((OgcException) failed).getJson().toString());
              routingContext.put("status_code", 404);
            }
            else{
              routingContext.put("response", new OgcException("InternalServerError", "Something broke").getJson().toString());
              routingContext.put("status_code", 500);
            }
            routingContext.next();
          });
  }

  private void getCollections(RoutingContext routingContext) {
    // validation logic here?

    dbService.getCollections()
        .onSuccess(success -> {
          // write your success story
          LOGGER.debug("Success! - {}", success.encodePrettily());
          routingContext.put("response", success.toString());
          routingContext.put("status_code", 200);
          // }
          routingContext.next();
        })
        .onFailure(failed -> {
          // well, you tried
          if (failed instanceof OgcException){
            routingContext.put("response",((OgcException) failed).getJson().toString());
            routingContext.put("status_code", 404);
          }
          else{
            routingContext.put("response", new OgcException("InternalServerError", "Something broke"));
            routingContext.put("status_code", 500);
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
     routingContext.next();
//        router.route().handler(requestHandler -> {
//            requestHandler
//                    .response()
//
//            requestHandler.next();
//        });
    }

    /* private void getFeatures(RoutingContext routingContext) {
      // validation logic here?
      --> check if query params exists [limit, bbox, datetime, filter]
      --> send params as a JsonObject, null if missing
            {limit:null/value, bbox: , datetime: , filter: }
      --> dbService.getFeatures(JsonObject) --> returns <JsonArray> features
      --> handle exception
      String collectionId = routingContext.pathParam("collectionId");
      MultiMap params = routingContext.queryParams();
      dbService.getFeatures(collectionId, Multimap queryParams)
          .onSuccess(success -> {
            // write your success story
            LOGGER.debug("Success! - {}", success.encodePrettily());
              // TODO: Add base_path from config
              routingContext.put("response",success.toString());
              routingContext.put("status_code", 200);
           // }
            routingContext.next();
          })
          .onFailure(failed -> {
            // well, you tried
            if (failed instanceof OgcException){
              routingContext.put("response",((OgcException) failed).getJson().toString());
              routingContext.put("status_code", 404);
            }
            else{
              routingContext.put("response", new OgcException("InternalServerError", "Something broke"));
              routingContext.put("status_code", 500);
            }
            routingContext.next();
          });
  }*/

  /* private void getFeature(RoutingContext routingContext) {
      // validation logic here?
      --> /collections/:collectionId/items/:itemId
      --> same as /collections/:collectionId
      String featureId = routingContext.pathParam("featureId");
      System.out.println("collectionId- "+collectionId);
      dbService.getFeature(collectionId, featureId)
          .onSuccess(success -> {
            // write your success story
            JsonObject response = new JsonObject();
            LOGGER.debug("Success! - {}", success.encodePrettily());
              // TODO: Add base_path from config
              routingContext.put("response",success.toString());
              routingContext.put("status_code", 200);
           // }
            routingContext.next();
          })
          .onFailure(failed -> {
            // well, you tried
            if (failed instanceof OgcException){
              routingContext.put("response",((OgcException) failed).getJson().toString());
              routingContext.put("status_code", 404);
            }
            else{
              routingContext.put("response", new OgcException("InternalServerError", "Something broke"));
              routingContext.put("status_code", 500);
            }
            routingContext.next();
          });
  }*/
}
