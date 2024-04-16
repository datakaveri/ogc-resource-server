package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.RequestPredicateException;
import io.vertx.json.schema.ValidationException;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.ProcessException;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.apiserver.util.Constants.*;

public class FailureHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(FailureHandler.class);
  @Override
  public void handle(RoutingContext routingContext) {
    Throwable failure = routingContext.failure();
    failure.printStackTrace();
    /* exceptions from OpenAPI specification*/
    if (failure instanceof ValidationException || failure instanceof BodyProcessorException ||
      failure instanceof RequestPredicateException ||
      failure instanceof ParameterProcessorException) {
      String failureMessage =
        failure.getCause() == null ? "Bad Request" : failure.getMessage();
      OgcException ogcException = new OgcException(400, "Bad Request", failureMessage);
      routingContext.response().putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .setStatusCode(HttpStatus.SC_BAD_REQUEST).end(ogcException.getJson().toString());
    }
    else if (failure instanceof OgcException) {
    LOGGER.debug("failure in handler ogc exception");
      routingContext.response().putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .setStatusCode((((OgcException) failure).getStatusCode())).end(((OgcException) failure).getJson().toString());
      return;
    }
    else if (failure instanceof ProcessException) {
      LOGGER.debug("failure in handler process exception");
      routingContext.response().putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .setStatusCode((((ProcessException) failure).getStatusCode())).end(((ProcessException) failure).getJson().toString());
      return;
    }
    else if(failure instanceof NullPointerException) {
      LOGGER.error("NPE Internal error "+failure.fillInStackTrace());

      routingContext.response().putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(500)
          .end(new JsonObject()
              .put("code", "Internal Server Error")
              .put("description","Internal Server Error").toString());

      return;

    }

    else {
      LOGGER.error("Internal server Error "+failure.fillInStackTrace());
      routingContext.response().putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .setStatusCode(500)
          .end(new JsonObject()
              .put("code", "Internal Server Error")
              .put("description","Internal Server Error").toString());
      return;

    }
    if (routingContext.response().ended()) {
      LOGGER.debug("Already ended");
      return;
    }
    routingContext.next();
  }
}
