package ogc.rs.apiserver.handlers;

import static ogc.rs.apiserver.util.Constants.APPLICATION_JSON;
import static ogc.rs.apiserver.util.Constants.CONTENT_TYPE;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.RequestPredicateException;
import io.vertx.json.schema.ValidationException;
import ogc.rs.apiserver.util.OgcException;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FailureHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(FailureHandler.class);
  @Override
  public void handle(RoutingContext routingContext) {
    Throwable failure = routingContext.failure();
    /* exceptions from OpenAPI specification*/
    if (failure instanceof ValidationException || failure instanceof BodyProcessorException ||
      failure instanceof RequestPredicateException ||
      failure instanceof ParameterProcessorException || failure instanceof NumberFormatException ) {
      String failureMessage =
        failure.getCause() == null ? "Bad Request" : failure.getCause().getMessage();
      OgcException ogcException = new OgcException(400, "Bad Request", failureMessage);
      routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatus.SC_BAD_REQUEST).end(ogcException.getJson().toString());
    }
    else if (failure instanceof OgcException) {
    LOGGER.debug("failure in handler ");
      routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode((((OgcException) failure).getStatusCode())).end(((OgcException) failure).getJson().toString());
      return;
    }
    else if(failure instanceof NullPointerException) {
      LOGGER.error("NPE Internal error "+failure.fillInStackTrace());

      routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode((((OgcException) failure).getStatusCode())).end(((OgcException) failure).getJson().toString());

      return;

    }

    else {
      LOGGER.error("Internal server Error "+failure.fillInStackTrace());

      routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode((((OgcException) failure).getStatusCode())).end(((OgcException) failure).getJson().toString());

      return;

    }
    if (routingContext.response().ended()) {
      LOGGER.debug("Already ended");
      return;
    }
    routingContext.next();
  }
}
