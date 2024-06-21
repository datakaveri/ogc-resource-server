package ogc.rs.apiserver.router.gisentities.ogccollections;

import java.util.UUID;
import java.util.function.Supplier;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Class used to hold OGC Collection metadata.
 * 
 */
public class OgcCollectionMetadata {

  public static final String OGC_OP_ID_PREFIX_REGEX = "^ogc-.*";
  public static final String STAC_OP_ID_PREFIX_REGEX = "^itemlessStacCollection-.*";

  public static final String OGC_GET_SPECIFIC_COLLECTION_OP_ID_REGEX =
      "^ogc-.*-get-specific-collection$";
  public static final String STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_OP_ID_REGEX =
      "^itemlessStacCollection-.*-get-specific-collection$";

  private UUID id;
  private String title;
  private String description;

  private final Supplier<String> OGC_GET_SPECIFIC_COLLECTION_SUMMARY =
      () -> "Metadata about " + description;
  private final Supplier<String> OGC_GET_SPECIFIC_COLLECTION_OPERATION_ID =
      () -> "ogc-" + id.toString() + "-get-specific-collection";
  private final Supplier<String> OGC_GET_SPECIFIC_COLLECTION_ENDPOINT =
      () -> "/collections/" + id.toString();

  private final Supplier<String> STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_SUMMARY =
      () -> "Metadata about " + description;
  private final Supplier<String> STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_OPERATION_ID =
      () -> "itemlessStacCollection-" + id.toString() + "-get-specific-collection";
  private final Supplier<String> STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_ENDPOINT =
      () -> "/stac/collections/" + id.toString();

  public OgcCollectionMetadata(JsonObject obj) {
    id = UUID.fromString(obj.getString("id"));
    title = obj.getString("title", "Undefined title");
    description = obj.getString("description", "Undefined description");
  }
  
  public UUID getId() {
    return id;
  }

  /**
   * Generate OpenAPI JSON block for all OGC routes that this collection can have.
   * 
   * @return JSON object containing OpenAPI paths for all OGC routes for this collection.
   */
  public JsonObject generateOgcOasBlock() {
    JsonObject block = new JsonObject();

    /* GET /collections/<collection-ID> */
    JsonObject collectionSpecificApi = new JsonObject();

    collectionSpecificApi.put("tags", new JsonArray().add(title));
    collectionSpecificApi.put("summary", OGC_GET_SPECIFIC_COLLECTION_SUMMARY.get());
    collectionSpecificApi.put("operationId", OGC_GET_SPECIFIC_COLLECTION_OPERATION_ID.get());
    collectionSpecificApi.put("responses",
        new JsonObject()
            .put("200", new JsonObject().put("$ref", "#/components/responses/Collection"))
            .put("500", new JsonObject().put("$ref", "#/components/responses/ServerError")));

    block.put(OGC_GET_SPECIFIC_COLLECTION_ENDPOINT.get(),
        new JsonObject().put("get", collectionSpecificApi));

    return block;
  }

  /**
   * 
   * Generate OpenAPI JSON block for all STAC routes that this collection can have.
   * 
   * @return JSON object containing OpenAPI paths for all STAC routes for this collection.
   */
  public JsonObject generateStacOasBlock() {
    JsonObject block = new JsonObject();

    /* GET /stac/collections/<collection-ID> */
    JsonObject collectionSpecific = new JsonObject();

    collectionSpecific.put("tags", new JsonArray().add(title));
    collectionSpecific.put("summary", STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_SUMMARY.get());
    collectionSpecific.put("operationId", STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_OPERATION_ID.get());
    collectionSpecific.put("responses",
        new JsonObject()
            .put("200", new JsonObject().put("$ref", "#/components/responses/stacCollection"))
            .put("404", new JsonObject().put("$ref", "#/components/responses/NotFound"))
            .put("500", new JsonObject().put("$ref", "#/components/responses/ServerError")));

    block.put(STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_ENDPOINT.get(),
        new JsonObject().put("get", collectionSpecific));

    return block;
  }
}
