package ogc.rs.apiserver.router.routerbuilders;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.router.RouterManager;
import ogc.rs.apiserver.router.gisentities.GisEntityInterface;
import java.util.ServiceLoader;

/**
 * Class to build router for STAC APIs.
 *
 */
public class StacRouterBuilder extends EntityRouterBuilder {

  private static final String OAS_PATH = RouterManager.STAC_OAS_REAL_PATH;
  private static final String OAS_API_PATH = "/stac/api";

  private StacRouterBuilder(ApiServerVerticle apiServerVerticle, Vertx vertx, JsonObject config,
      RouterBuilder routerBuilder) {
    super(apiServerVerticle, vertx, routerBuilder, config);
  }

  /**
   * Create an instance of {@link StacRouterBuilder}. 
   * 
   * @param apiServerVerticle the {@link ApiServerVerticle} whose router is to be updated
   * @param vertx an instance of Vert.x
   * @param config the config JSON
   * @return a Future of {@link StacRouterBuilder}
   */
  public static Future<StacRouterBuilder> create(ApiServerVerticle apiServerVerticle, Vertx vertx,
      JsonObject config) {
    Promise<StacRouterBuilder> promise = Promise.promise();

    Future<RouterBuilder> routerBuilderFut = RouterBuilder.create(vertx, OAS_PATH);
    
    routerBuilderFut
        .onSuccess(routerBuilder -> promise
            .complete(new StacRouterBuilder(apiServerVerticle, vertx, config, routerBuilder)))
        .onFailure(err -> promise.fail(err));

    return promise.future();
  }
  
  
  @Override
  String getOasApiPath() {
    return OAS_API_PATH;
  }

  @Override
  void addImplSpecificRoutes() {
    
    routerBuilder
    .operation("getStacLandingPage")
    .handler(apiServerVerticle::stacCatalog)
    .handler(apiServerVerticle::putCommonResponseHeaders)
    .handler(apiServerVerticle::buildResponse);

    routerBuilder
    .operation("getConformanceDeclaration")
    .handler(
        routingContext -> {
          HttpServerResponse response = routingContext.response();
          response.sendFile("docs/stacConformance.json");
        });

    routerBuilder
    .operation("getStacCollections")
    .handler(apiServerVerticle::stacCollections)
    .handler(apiServerVerticle::putCommonResponseHeaders)
    .handler(apiServerVerticle::buildResponse);
    
    /**
     *  For all implementers of GisEntityInterface, add the STAC routes to the RouterBuilder.
     */
    ServiceLoader<GisEntityInterface> loader = ServiceLoader.load(GisEntityInterface.class);
    loader.forEach(i -> i.giveStacRoutes(this));
    
    return;
  }
}
