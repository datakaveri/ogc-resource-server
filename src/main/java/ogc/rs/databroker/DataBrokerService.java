package ogc.rs.databroker;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface DataBrokerService {
    @GenIgnore
    static DataBrokerService createProxy(Vertx vertx, String address) {
        return new DataBrokerServiceVertxEBProxy(vertx, address);
    }

    Future<Void> publishMessage(String toExchange,
                                      String routingKey,
                                      JsonObject body);
}
