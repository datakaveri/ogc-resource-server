package ogc.rs.processes;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import ogc.rs.catalogue.CatalogueInterface;
import ogc.rs.catalogue.CatalogueService;

@VertxGen
@ProxyGen
public interface ProcessesRunnerService {
  @GenIgnore
  static ProcessesRunnerService createProxy(Vertx vertx, String address) {
    return new ProcessesRunnerServiceVertxEBProxy(vertx, address);
  }
  /**
   * Runs the process with the given input data.
   *
   * @param input the input data for the process
   * @param handler the handler to be called with the result of the process
   */
  @Fluent
  ProcessesRunnerService run(JsonObject input, Handler<AsyncResult<JsonObject>>
    handler);

}
