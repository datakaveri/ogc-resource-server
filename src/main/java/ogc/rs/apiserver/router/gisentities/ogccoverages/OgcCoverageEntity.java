package ogc.rs.apiserver.router.gisentities.ogccoverages;

import static ogc.rs.apiserver.util.Constants.COVERAGE_SCHEMA;

import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.handlers.FailureHandler;
import ogc.rs.apiserver.router.gisentities.GisEntityInterface;
import ogc.rs.apiserver.router.routerbuilders.OgcRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.StacRouterBuilder;
import ogc.rs.apiserver.router.util.OasFragments;
import ogc.rs.database.DatabaseService;

@AutoService(GisEntityInterface.class)
public class OgcCoverageEntity implements GisEntityInterface {


    @Override
    public void giveOgcRoutes(OgcRouterBuilder ogcRouterBuilder) {

        RouterBuilder builder = ogcRouterBuilder.routerBuilder;
        ApiServerVerticle apiServerVerticle = ogcRouterBuilder.apiServerVerticle;
        FailureHandler failureHandler = ogcRouterBuilder.failureHandler;

            builder.operation(COVERAGE_SCHEMA)
                    .handler(apiServerVerticle::getCoverageSchema)
                    .handler(apiServerVerticle::putCommonResponseHeaders)
                    .handler(apiServerVerticle::buildResponse)
                    .failureHandler(failureHandler);

    }

    @Override
    public void giveStacRoutes(StacRouterBuilder routerBuilder) {
        // no stac routes for now
    }

    @Override
    public Future<OasFragments> generateNewSpecFragments(JsonObject existingOgcSpec, JsonObject existingStacSpec, DatabaseService dbService, JsonObject config) {
        // no custom spec generation for now
        return Future.succeededFuture(new OasFragments());
    }
}
