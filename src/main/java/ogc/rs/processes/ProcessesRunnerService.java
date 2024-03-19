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

@VertxGen
@ProxyGen
public interface ProcessesRunnerService {
  @GenIgnore
  static ProcessesRunnerService createProxy(Vertx vertx, String address) {
    return new ProcessesRunnerServiceVertxEBProxy(vertx, address);
  }
  @Fluent
  ProcessesRunnerService run(JsonObject input, Handler<AsyncResult<JsonObject>>
    promise);

}
