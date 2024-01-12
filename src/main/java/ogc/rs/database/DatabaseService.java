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

    Future<JsonObject> getStacCollection(String collectionId);

    Future<List<JsonObject>> getTileMatrixSets();

    Future<List<JsonObject>> getTileMatrixSetMetaData(String tileMatrixSet);

    Future<List<JsonObject>> getTileMatrixSetRelation(String collectionId);

    Future<List<JsonObject>> getTileMatrixSetRelationOverload(String collectionId, String tileMatrixSetId);

    Future<JsonObject> getProcesses(int limit);
    Future<JsonObject> getProcess(String processId);
}
