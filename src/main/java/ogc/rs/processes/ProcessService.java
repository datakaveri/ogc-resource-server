package ogc.rs.processes;

import io.vertx.core.json.JsonObject;
import io.vertx.core.Future;

public interface ProcessService {
  Future<JsonObject> execute(JsonObject input);
}
