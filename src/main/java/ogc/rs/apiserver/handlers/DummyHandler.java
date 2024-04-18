package ogc.rs.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DummyHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(DummyHandler.class);

   DummyHandler create(Vertx vertx){
       return new DummyHandler();
   }
    @Override
    public void handle(RoutingContext routingContext) {
       LOGGER.debug("Inside Dummy handler");
    return;
    }
}
