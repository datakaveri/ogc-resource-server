package ogc.rs.apiserver.router.gisentities.ogcRecords;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.UUID;
import java.util.function.Supplier;
import ogc.rs.apiserver.router.gisentities.ogcfeatures.OasTypes;

import static ogc.rs.common.Constants.DEFAULT_SERVER_CRS;

public class OgcRecordsMetadata {

  public static final String OGC_RECORD_OP_ID_PREFIX_REGEX = "^recordCollection-.*";
  public static final String OGC_GET_SPECIFIC_RECORD_COLLECTION_OP_ID_REGEX =
      "^recordCollection-.*-get-specific-collection$";
  public static final String OGC_RECORD_GET_ITEMS_COLLECTION_OP_ID_REGEX =
      "^recordCollection-.*-get-items$";
  public static final String OGC_RECORD_GET_SPECIFIC_RECORD_ITEM_OP_ID_REGEX =
          "^recordCollection-.*-get-specific-item$";




  private UUID id;
  private String title;
  private String description;

  private final Supplier<String> OGC_RECORD_GET_SPECIFIC_COLLECTION_SUMMARY =
      () -> "Metadata about " + description;
  private final Supplier<String> OGC_RECORD_GET_SPECIFIC_COLLECTION_OPERATION_ID =
      () -> "recordCollection-" + id.toString() + "-get-specific-collection";
  private final Supplier<String> OGC_RECORD_GET_SPECIFIC_ENDPOINT =
      () -> "/collections/" + id.toString();

  private final Supplier<String> OGC_RECORD_GET_COLLECTION_ITEMS_SUMMARY =
      () -> "Get items from " + description;
  private final Supplier<String> OGC_RECORD_GET_COLLECTION_ITEMS_OPERATION_ID =
      () -> "recordCollection-" + id.toString() + "-get-items";
  private final Supplier<String> OGC_RECORD_COLLECTION_ITEMS_ENDPOINT =
      () -> "/collections/" + id.toString() + "/items";

  private final Supplier<String> OGC_RECORD_GET_SPECIFIC_COLLECTION_ITEM_SUMMARY =
          () -> "get single item from "+description;
  private final Supplier<String> OGC_RECORD_GET_SPECIFIC_COLLECTION_ITEM_OPERATION_ID =
          () -> "recordCollection-" + id.toString() + "-get-specific-item";
  private final Supplier<String> OGC_RECORD_GET_SPECIFIC_COLLECTION_ITEM_ENDPOINT =
          () -> "/collections/" + id.toString() + "/items/{recordId}";

  public OgcRecordsMetadata(JsonObject obj) {
    id = UUID.fromString(obj.getString("id"));
    title = obj.getString("title", "Undefined title");
    description = obj.getString("description", "Undefined description");
  }

  public UUID getId() {
    return id;
  }

  public JsonObject generateRecordOasBlock() {
    JsonObject block = new JsonObject();

    /* GET /collections/<collection-ID> */
    JsonObject recordSpecific = new JsonObject();

    recordSpecific.put("tags", new JsonArray().add(title));
    recordSpecific.put("summary", OGC_RECORD_GET_SPECIFIC_COLLECTION_SUMMARY.get());
    recordSpecific.put("operationId", OGC_RECORD_GET_SPECIFIC_COLLECTION_OPERATION_ID.get());
    recordSpecific.put(
        "responses",
        new JsonObject()
            .put("200", new JsonObject().put("$ref", "#/components/responses/RecordCatalog"))
            .put("404", new JsonObject().put("$ref", "#/components/responses/NotFound"))
            .put("500", new JsonObject().put("$ref", "#/components/responses/ServerError")));

    block.put(OGC_RECORD_GET_SPECIFIC_ENDPOINT.get(),
            new JsonObject().put("get", recordSpecific));

    /*GET /collections/<collection-ID>/items */

    JsonObject limitParam = new JsonObject().put("in", "query").put("name", "limit")
            .put("required", false).put("style", "form").put("explode", false).put("schema",
                    new JsonObject().put("type", "integer").put("minimum", 1)
                            .put("maximum", 10000).put("default", 10));

    JsonObject titleParam = new JsonObject().put("in", "query").put("name", "title")
            .put("required", false).put("style", "form").put("explode", false).put("schema",
                    new JsonObject().put("type", "string"));

    JsonObject idParam = new JsonObject().put("in", "query").put("name", "id")
            .put("required", false).put("style", "form").put("explode", false).put("schema",
                    new JsonObject().put("type", "integer"));

    JsonObject providerNameParam = new JsonObject().put("in", "query").put("name", "provider_name")
            .put("required", false).put("style", "form").put("explode", false).put("schema",
                    new JsonObject().put("type", "string"));

    JsonObject providerContactsParam = new JsonObject().put("in", "query").put("name", "provider_contacts")
            .put("required", false).put("style", "form").put("explode", false).put("schema",
                    new JsonObject().put("type", "string"));


      JsonObject recordItems = new JsonObject();
      recordItems.put("tags", new JsonArray().add(title));
      recordItems.put("summary", OGC_RECORD_GET_COLLECTION_ITEMS_SUMMARY.get());
      recordItems.put("operationId", OGC_RECORD_GET_COLLECTION_ITEMS_OPERATION_ID.get());
     JsonArray parameters = new JsonArray();
     parameters
             .add(titleParam)
             .add(idParam)
             .add(providerContactsParam)
             .add(providerNameParam)
             .add(limitParam)
             .add(new JsonObject().put("$ref", "#/components/parameters/bbox"))
             .add(new JsonObject().put("$ref", "#/components/parameters/keywords"))
             .add(new JsonObject().put("$ref", "#/components/parameters/created"))
             .add(new JsonObject().put("$ref", "#/components/parameters/q"))
             .add(new JsonObject().put("$ref", "#/components/parameters/ids"))
             .add(new JsonObject().put("$ref", "#/components/parameters/offset"))
             .add(new JsonObject().put("$ref", "#/components/parameters/datetime"));

    recordItems.put("parameters", parameters);
     recordItems.put(
              "responses",
              new JsonObject()
                      .put("200", new JsonObject().put("$ref", "#/components/responses/RecordItems"))
                      .put("404", new JsonObject().put("$ref", "#/components/responses/NotFound"))
                      .put("500", new JsonObject().put("$ref", "#/components/responses/ServerError")));
      block.put(OGC_RECORD_COLLECTION_ITEMS_ENDPOINT.get(), new JsonObject().put("get", recordItems));


      /*GET /collections/<collection-ID>/items/<record-ID> */
      JsonObject recordSpecificApi = new JsonObject();
      recordSpecificApi.put("tags", new JsonArray().add(title));
      recordSpecificApi.put("summary", OGC_RECORD_GET_SPECIFIC_COLLECTION_ITEM_SUMMARY.get());
      recordSpecificApi.put("operationId", OGC_RECORD_GET_SPECIFIC_COLLECTION_ITEM_OPERATION_ID.get());
    recordSpecificApi.put(
            "responses",
            new JsonObject()
                    .put("200", new JsonObject().put("$ref", "#/components/responses/RecordItem"))
                    .put("404", new JsonObject().put("$ref", "#/components/responses/NotFound"))
                    .put("500", new JsonObject().put("$ref", "#/components/responses/ServerError")));

    JsonObject featureIdQueryParam =
            new JsonObject().put("in", "path").put("name", "recordId").put("required", true)
                    .put("schema", new JsonObject().put("type", OasTypes.STRING.toString().toLowerCase()));

    recordSpecificApi.put("parameters",
            new JsonArray().add(featureIdQueryParam));



    block.put(OGC_RECORD_GET_SPECIFIC_COLLECTION_ITEM_ENDPOINT.get(),
              new JsonObject().put("get", recordSpecificApi));
    return block;
  }
}
