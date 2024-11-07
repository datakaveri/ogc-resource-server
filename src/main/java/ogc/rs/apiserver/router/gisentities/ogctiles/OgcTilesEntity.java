package ogc.rs.apiserver.router.gisentities.ogctiles;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.handlers.FailureHandler;
import ogc.rs.apiserver.handlers.TilesMeteringHandler;
import ogc.rs.apiserver.router.gisentities.GisEntityInterface;
import ogc.rs.apiserver.router.routerbuilders.OgcRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.StacRouterBuilder;
import ogc.rs.apiserver.router.util.OasFragments;
import ogc.rs.database.DatabaseService;
import static ogc.rs.apiserver.util.Constants.*;
import com.google.auto.service.AutoService;

/**
 * Class to handle creation of routes for OGC Tiles.
 *
 */
@AutoService(GisEntityInterface.class)
public class OgcTilesEntity implements GisEntityInterface{

  @Override
  public void giveOgcRoutes(OgcRouterBuilder ogcRouterBuilder) {
    // TODO Auto-generated method stub

    RouterBuilder builder = ogcRouterBuilder.routerBuilder;
    ApiServerVerticle apiServerVerticle = ogcRouterBuilder.apiServerVerticle;
    FailureHandler failureHandler = ogcRouterBuilder.failureHandler;

          builder
              .operation(TILEMATRIXSETS_API)
              .handler(apiServerVerticle::getTileMatrixSetList)
              .handler(apiServerVerticle::putCommonResponseHeaders)
              .handler(apiServerVerticle::buildResponse)
              .failureHandler(failureHandler);

          builder
                .operation(TILEMATRIXSET_API)
                .handler(apiServerVerticle::getTileMatrixSet)
                .handler(apiServerVerticle::putCommonResponseHeaders)
                .handler(apiServerVerticle::buildResponse)
                .failureHandler(failureHandler);

          builder
              .operation(TILESETSLIST_API)
              .handler(apiServerVerticle::getTileSetList)
              .handler(apiServerVerticle::putCommonResponseHeaders)
              .handler(apiServerVerticle::buildResponse)
              .failureHandler(failureHandler);

          builder
              .operation(TILESET_API)
              .handler(apiServerVerticle::getTileSet)
              .handler(apiServerVerticle::putCommonResponseHeaders)
              .handler(apiServerVerticle::buildResponse)
              .failureHandler(failureHandler);

    builder
        .operation(TILE_API)
        .handler(
            ctx -> {
              ctx.addBodyEndHandler(
                  context -> ogcRouterBuilder.tilesMeteringHandler.handleMetering(ctx));
              ctx.next();
            })
        .handler(ogcRouterBuilder.ogcFeaturesAuthZHandler)
        .handler(apiServerVerticle::getTile)
        .handler(apiServerVerticle::putCommonResponseHeaders)
        .handler(apiServerVerticle::buildResponse)
        .failureHandler(failureHandler);
  }

  @Override
  public void giveStacRoutes(StacRouterBuilder routerBuilder) {
    // no stac routes for now
  }

  @Override
  public Future<OasFragments> generateNewSpecFragments(
      JsonObject existingOgcSpec,
      JsonObject existingStacSpec,
      DatabaseService dbService,
      JsonObject config) {
    // no custom spec generation for now
    return Future.succeededFuture(new OasFragments());
  }

}
