package ogc.rs.jobs;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface JobsService {

  @GenIgnore
  static JobsService createProxy(Vertx vertx, String address) {
    return new JobsServiceVertxEBProxy(vertx, address);
  }

  /**
   * Returns the status of a job.
   *
   * @param requestBody a JSON object containing the "jobId" and "userId" properties
   * @return a JSON object containing the status of the job, including the "links" property
   */
  Future<JsonObject> getStatus(JsonObject requestBody);
  Future<JsonObject> listAllJobs(JsonObject requestBody);
  Future<JsonObject> retrieveJobResults(JsonObject requestBody);
}
