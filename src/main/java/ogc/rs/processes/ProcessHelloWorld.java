package ogc.rs.processes;

import io.vertx.core.json.JsonObject;
import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;

public class ProcessHelloWorld implements ProcessService{
  private final PgPool pgPool;
  private final WebClient webClient;
  ProcessHelloWorld(PgPool pgPool, WebClient webClient){
    this.pgPool = pgPool;
    this.webClient = webClient;
  }

  @Override
  public Future<JsonObject> execute(JsonObject input) {
    // validate input using validateInput(input)

    // process will be executed after getting all the required i/p from the json obj

    return null;
  }

  private JsonObject createOutput(){
    // create output according to the output required by the process
    return null;
  }

  private boolean validateInput(JsonObject input){
    // check if the input in the request body is a valid input
  return true;
  }

}
