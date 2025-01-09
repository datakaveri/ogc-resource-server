package ogc.rs.apiserver.router.gisentities.staccollections;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.UUID;
import java.util.function.Supplier;

/** Class used to hold STAC Collection metadata */
public class StacCollectionMetadata {
  public static final String STAC_OP_ID_PREFIX_REGEX = "^itemlessStacCollection-.*";
  public static final String STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_OP_ID_REGEX =
      "^itemlessStacCollection-.*-get-specific-collection$";
  public static final String STAC_GET_ITEMS_COLLECTION_OP_ID_REGEX =
      "^itemlessStacCollection-.*-get-items$";

  private UUID id;
  private String title;
  private String description;

  private final Supplier<String> STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_SUMMARY =
      () -> "Metadata about " + description;
  private final Supplier<String> STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_OPERATION_ID =
      () -> "itemlessStacCollection-" + id.toString() + "-get-specific-collection";
  private final Supplier<String> STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_ENDPOINT =
      () -> "/stac/collections/" + id.toString();

  private final Supplier<String> STAC_GET_COLLECTION_ITEMS_SUMMARY =
      () -> "Get items from " + description;
  private final Supplier<String> STAC_GET_COLLECTION_ITEMS_OPERATION_ID =
      () -> "itemlessStacCollection-" + id.toString() + "-get-items";
  private final Supplier<String> STAC_GET_COLLECTION_ITEMS_ENDPOINT =
      () -> "/stac/collections/" + id.toString() + "/items";

  public StacCollectionMetadata(JsonObject obj) {
    id = UUID.fromString(obj.getString("id"));
    title = obj.getString("title", "Undefined title");
    description = obj.getString("description", "Undefined description");
  }

  public UUID getId() {
    return id;
  }

  /**
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
    collectionSpecific.put(
        "responses",
        new JsonObject()
            .put("200", new JsonObject().put("$ref", "#/components/responses/stacCollection"))
            .put("404", new JsonObject().put("$ref", "#/components/responses/NotFound"))
            .put("500", new JsonObject().put("$ref", "#/components/responses/ServerError")));

    block.put(
        STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_ENDPOINT.get(),
        new JsonObject().put("get", collectionSpecific));

    /* GET stac/collections/<collection-ID>/items */
    JsonObject stacItems = new JsonObject();
    stacItems.put("tags", new JsonArray().add(title));
    stacItems.put("summary", STAC_GET_COLLECTION_ITEMS_SUMMARY.get());
    stacItems.put("operationId", STAC_GET_COLLECTION_ITEMS_OPERATION_ID.get());
    stacItems.put(
        "responses",
        new JsonObject()
            .put("200", new JsonObject().put("$ref", "#/components/responses/StacFeatures"))
            .put("404", new JsonObject().put("$ref", "#/components/responses/NotFound"))
            .put("500", new JsonObject().put("$ref", "#/components/responses/ServerError")));
    block.put(STAC_GET_COLLECTION_ITEMS_ENDPOINT.get(), new JsonObject().put("get", stacItems));

    return block;
  }
}
