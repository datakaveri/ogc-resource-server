package ogc.rs.database;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

@VertxGen
@ProxyGen
public interface DatabaseService {
    @GenIgnore
    static DatabaseService createProxy(Vertx vertx, String address) {
        return new DatabaseServiceVertxEBProxy(vertx, address);
    }
    // TODO:
    // Future<JsonObject> to Future<DO> class.

    Future<List<JsonObject>>  getCollections();
    Future<List<JsonObject>>  getCollection(final String collectionId);

    Future<JsonObject> getFeatures(String collectionId, Map<String, String> queryParams, Map<String, Integer> crs);

    Future<JsonObject> getFeature(String collectionId, String featureId, Map<String, String> queryParams, Map<String,
        Integer> crs);

    Future<Void> matchFilterWithProperties(String collectionId, Map<String, String> queryParams);

    Future<Map<String, Integer>> isCrsValid(String collectionId, Map<String, String> crs);

    Future<List<JsonObject>>  getStacCollections();

}
