package ogc.rs.apiserver.router.routerbuilders;

import static ogc.rs.apiserver.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.handlers.AuthHandler;

/**
 * Class to build router for DX Metering APIs. <br>
 * 
 * <b>NOTE: <b/> Although this class is an instance of {@link EntityRouterBuilder}, there is no
 * dynamic route generation done by this class. Only static routes are supplied.
 *
 */
public class DxMeteringRouterBuilder extends EntityRouterBuilder {

  private static final String OAS_PATH = "docs/dxmeteringopenapiv3_0.json";
  private static final String OAS_API_PATH = "/metering/api";

  private DxMeteringRouterBuilder(ApiServerVerticle apiServerVerticle, Vertx vertx,
      JsonObject config, RouterBuilder routerBuilder) {
    super(apiServerVerticle, vertx, routerBuilder, config);
  }

  /**
   * Create an instance of {@link DxMeteringRouterBuilder}.
   * 
   * @param apiServerVerticle the {@link ApiServerVerticle} whose router is to be updated
   * @param vertx an instance of Vert.x
   * @param config the config JSON
   * @return a Future of {@link DxMeteringRouterBuilder}
   */
  public static Future<DxMeteringRouterBuilder> create(ApiServerVerticle apiServerVerticle,
      Vertx vertx, JsonObject config) {
    Promise<DxMeteringRouterBuilder> promise = Promise.promise();

    Future<RouterBuilder> routerBuilderFut = RouterBuilder.create(vertx, OAS_PATH);

    routerBuilderFut
        .onSuccess(routerBuilder -> promise
            .complete(new DxMeteringRouterBuilder(apiServerVerticle, vertx, config, routerBuilder)))
        .onFailure(err -> promise.fail(err));

    return promise.future();
  }

  @Override
  String getOasApiPath() {
    return OAS_API_PATH;
  }

  @Override
  void addImplSpecificRoutes() {

    routerBuilder.operation(SUMMARY_AUDIT_API)
        .handler(AuthHandler.create(vertx))
        .handler(apiServerVerticle::getSummary)
        .handler(apiServerVerticle::putCommonResponseHeaders)
        .handler(apiServerVerticle::buildResponse)
        .failureHandler(failureHandler);


    routerBuilder.operation(OVERVIEW_AUDIT_API)
        .handler(AuthHandler.create(vertx))
        .handler(apiServerVerticle::getMonthlyOverview)
        .handler(apiServerVerticle::putCommonResponseHeaders)
        .handler(apiServerVerticle::buildResponse)
        .failureHandler(failureHandler);

    routerBuilder.operation(CONSUMER_AUDIT_API)
        .handler(AuthHandler.create(vertx))
        .handler(apiServerVerticle::getConsumerAuditDetail)
        .handler(apiServerVerticle::putCommonResponseHeaders)
        .handler(apiServerVerticle::buildResponse)
        .failureHandler(failureHandler);

    routerBuilder.operation(PROVIDER_AUDIT_API)
        .handler(AuthHandler.create(vertx))
        .handler(apiServerVerticle::getProviderAuditDetail)
        .handler(apiServerVerticle::putCommonResponseHeaders)
        .handler(apiServerVerticle::buildResponse)
        .failureHandler(failureHandler);

    return;
  }
}
