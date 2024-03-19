package ogc.rs.processes;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ProcessDummyFlow implements ProcessService{
  Logger LOGGER = LogManager.getLogger(ProcessDummyFlow.class);
  private final PgPool pgPool;
  private final WebClient webClient;
  public ProcessDummyFlow(PgPool pgPool, WebClient webClient) {
  this.pgPool= pgPool;
  this.webClient = webClient;
  }


  public void executeLongRunningBlockingOperation() throws InterruptedException {
    LOGGER.info("Thread sleeping start");
    Thread.sleep(11000);
    LOGGER.info("Thread sleeping done");

  }
  @Override
  public Future<JsonObject> execute(JsonObject input) throws InterruptedException {
    LOGGER.info("Doing the job...");
    Promise<JsonObject> result = Promise.promise();
    executeLongRunningBlockingOperation();

    return result.future();
  }
}
