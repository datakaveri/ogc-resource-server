package ogc.rs.apiserver.router.gisentities.ogcprocesses;

import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.handlers.FailureHandler;
import ogc.rs.apiserver.router.gisentities.GisEntityInterface;
import ogc.rs.apiserver.router.routerbuilders.OgcRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.StacRouterBuilder;
import ogc.rs.apiserver.router.util.OasFragments;
import ogc.rs.database.DatabaseService;
import static ogc.rs.apiserver.util.Constants.*;

/**
 * Class to handle creation of routes for OGC Processes.
 * 
 */
@AutoService(GisEntityInterface.class)
public class OgcProcessesEntity implements GisEntityInterface{

  @Override
  public void giveOgcRoutes(OgcRouterBuilder ogcRouterBuilder) {
    
    RouterBuilder builder = ogcRouterBuilder.routerBuilder;
    ApiServerVerticle apiServerVerticle = ogcRouterBuilder.apiServerVerticle;
    FailureHandler failureHandler = ogcRouterBuilder.failureHandler;
    
    builder
        .operation(EXECUTE_API)
        .handler(ogcRouterBuilder.processAuthZHandler)
        .handler(apiServerVerticle::executeJob)
        .handler(apiServerVerticle::putCommonResponseHeaders)
        .handler(apiServerVerticle::buildResponse)
        .failureHandler(failureHandler);

    builder.operation(PROCESSES_API)
    .handler(apiServerVerticle::getProcesses).handler(apiServerVerticle::putCommonResponseHeaders)
    .handler(apiServerVerticle::buildResponse)
    .failureHandler(failureHandler);

    builder.operation(PROCESS_API)
    .handler(apiServerVerticle::getProcess).handler(apiServerVerticle::putCommonResponseHeaders)
    .handler(apiServerVerticle::buildResponse).failureHandler(failureHandler);

    builder
        .operation(STATUS_API)
        .handler(ogcRouterBuilder.jobsAuthZHandler)
        .handler(apiServerVerticle::getStatus)
        .handler(apiServerVerticle::putCommonResponseHeaders)
        .handler(apiServerVerticle::buildResponse)
        .failureHandler(failureHandler);

    builder.operation(LIST_JOBS_API)
            .handler(ogcRouterBuilder.jobsAuthZHandler)
            .handler(apiServerVerticle::listAllJobs)
            .handler(apiServerVerticle::putCommonResponseHeaders)
            .handler(apiServerVerticle::buildResponse)
            .failureHandler(failureHandler);

    builder.operation(GET_JOB_RESULTS)
            .handler(ogcRouterBuilder.jobsAuthZHandler)
            .handler(apiServerVerticle::retrieveJobResults)
            .handler(apiServerVerticle::putCommonResponseHeaders)
            .handler(apiServerVerticle::buildResponse)
            .failureHandler(failureHandler);
  }

  @Override
  public void giveStacRoutes(StacRouterBuilder routerBuilder) {
    // no stac routes as of now
  }

  @Override
  public Future<OasFragments> generateNewSpecFragments(JsonObject existingOgcSpec,
      JsonObject existingStacSpec, DatabaseService dbService, JsonObject config) {
    // no custom spec generation for now
    return Future.succeededFuture(new OasFragments());
  }

}
