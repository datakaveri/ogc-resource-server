package ogc.rs.database;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface DatabaseService {
    @GenIgnore
    static DatabaseService createProxy(Vertx vertx, String address) {
        return new DatabaseServiceVertxEBProxy(vertx, address);
    }
    // TODO:
    // Future<JsonObject> to Future<DO> class.

    Future<JsonObject>  getCollection(final String collectionId);

}
