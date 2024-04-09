package ogc.rs.apiserver.router.routerbuilders;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.handlers.FailureHandler;
import static ogc.rs.apiserver.util.Constants.*;
import java.util.Set;

/**
 * Abstract class to aid in configuration and building of routers using {@link RouterBuilder}.
 *
 */
public abstract class EntityRouterBuilder {

  private static final Set<String> allowedHeaders =
      Set.of(HEADER_TOKEN, HEADER_CONTENT_LENGTH, HEADER_CONTENT_TYPE, HEADER_HOST, HEADER_ORIGIN,
          HEADER_REFERER, HEADER_ACCEPT, HEADER_ALLOW_ORIGIN);

  private static final Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.OPTIONS);

  /* this is used since all the API methods are implemented in the ApiServer verticle */
  public ApiServerVerticle apiServerVerticle;
  
  /* this is used to create handlers or anything that is initialized using a vert.x instance */
  public Vertx vertx;
  
  /* create all handlers here and make them public so that they can be accessed */
  public FailureHandler failureHandler = new FailureHandler();
  
  public RouterBuilder routerBuilder;
  private JsonObject config;

  EntityRouterBuilder(ApiServerVerticle apiServerVerticle, Vertx vertx, RouterBuilder routerBuilder,
      JsonObject config) {
    this.apiServerVerticle = apiServerVerticle;
    this.vertx = vertx;
    this.routerBuilder = routerBuilder;
    this.config = config;
  }

  /**
   * Any common {@link RouterBuilder} configuration to be added to any router to be built.
   */
  final void setCommonRouterBuilderConfiguration() {
    
    RouterBuilderOptions factoryOptions =
        new RouterBuilderOptions().setMountResponseContentTypeHandler(true);

    routerBuilder.rootHandler(
        CorsHandler.create().allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
    routerBuilder.rootHandler(BodyHandler.create());
  }

  /**
   * Get a {@link Router} with all routes added to it. 
   * 
   * @return a fully built {@link Router}
   */
  public final Router getRouter() {

    setCommonRouterBuilderConfiguration();
    addImplSpecificRoutes();

    Router router = routerBuilder.createRouter();

    JsonObject oasJson = routerBuilder.getOpenAPI().getOpenAPI();

    /* Set the OpenAPI spec route */
    router.get(getOasApiPath()).handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("Content-type", "application/vnd.oai.openapi+json;version=3.0");
      response.send(oasJson.toBuffer());
    });
    
    /* Handle not implemented/ not found paths */
    router.route().last().handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("Content-type", "application/vnd.oai.openapi+json;version=3.0");
      response.setStatusCode(404);
      response.send(new JsonObject().put("code", "Not Found")
          .put("description", "API / Collection not found").toBuffer());
    });

    return router;
  }

  /**
   * Return the API path where the OpenAPI spec JSON is served. The route is created in the
   * {@link #getRouter()} method.
   * 
   * @return the endpoint
   */
  abstract String getOasApiPath();

  /**
   * Add all implementation specific routes to the {@link RouterBuilder}.
   */
  abstract void addImplSpecificRoutes();
}
