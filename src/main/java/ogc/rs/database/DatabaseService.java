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

    Future<JsonObject> getFeature(String collectionId, Integer featureId, Map<String, String> queryParams, Map<String,
        Integer> crs);

    Future<Map<String, Integer>> isCrsValid(String collectionId, Map<String, String> queryParams);

    Future<List<JsonObject>>  getStacCollections();

    Future<JsonObject> getStacCollection(String collectionId);

    Future<List<JsonObject>> getTileMatrixSets();

    Future<List<JsonObject>> getStacItems(String collectionId, int limit, int offset);

    Future<JsonObject> getStacItemById(String collectionId, String stacItemId);

    Future<JsonObject> stacItemSearch(Map<String, String> queryParams);

    Future<List<JsonObject>> getTileMatrixSetMetaData(String tileMatrixSet);

    Future<List<JsonObject>> getTileMatrixSetRelation(String collectionId);

    Future<List<JsonObject>> getTileMatrixSetRelationOverload(String collectionId, String tileMatrixSetId);

    Future<JsonObject> getAssets(String assetId);

    Future<JsonObject> getProcesses(int limit);
    Future<JsonObject> getProcess(String processId);


    /**
     * Get OGC Feature Collection metadata to be used for OpenAPI spec generation.
     * 
     * @param existingCollectionUuidIds UUID IDs of collections that are already part of the spec.
     * @return list of {@link JsonObject}, which is cast to the required type by the caller.
     */
    Future<List<JsonObject>> getOgcFeatureCollectionMetadataForOasSpec(List<String> existingCollectionUuidIds);

    Future<Boolean> getAccess(String id);

    /**
     * Retrieves all the details of coverage for the collection whose id is provided.
     *
     * @param id The ID of the collection for which to retrieve the schema. Must be a valid UUID string.
     * @return The future will return with a JsonObject containing the coverageJson data if successful,
     * or it will fail with an OgcException if the collection is not found or if there is an internal server error.
     */
    Future<JsonObject> getCoverageDetails(String id);

    /**
     * Get all collections metadata to be used for OpenAPI spec generation.
     * 
     * @param existingCollectionUuidIds UUID IDs of collections that are already part of the spec.
     * @return list of {@link JsonObject}, which is cast to the required type by the caller.
     */
    Future<List<JsonObject>> getCollectionMetadataForOasSpec(List<String> existingCollectionUuidIds);

    /**
     * Get all STAC collections metadata to be used for OpenAPI spec generation.
     *
     * @param existingCollectionUuidIds UUID IDs of collections that are already part of the spec.
     * @return list of {@link JsonObject}, which is cast to the required type by the caller.
     */
    Future<List<JsonObject>> getStacCollectionMetadataForOasSpec(List<String> existingCollectionUuidIds);

}
