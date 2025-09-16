package ogc.rs.catalogue;

import static ogc.rs.apiserver.authorization.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import java.util.List;
import ogc.rs.apiserver.authorization.model.Asset;
import ogc.rs.apiserver.authorization.model.AssetType;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CatalogueService implements CatalogueInterface {
  private static final Logger LOGGER = LogManager.getLogger(CatalogueService.class);
  public static WebClient catWebClient;
  final String host;
  final int port;
  final String catBasePath;
  final String path;
  final String catalogueItemPath;

  public CatalogueService(WebClient webclient, String host, int port, String catBasePath, String catalogueItemPath,
                          String catSearchPath) {
    catWebClient = webclient;
    this.host = host;
    this.port = port;
    this.catBasePath = catBasePath;
    this.path = catBasePath + catSearchPath;
    this.catalogueItemPath = catalogueItemPath;
  }

  @Override
  public Future<JsonObject> getCatItem(String id) {
    LOGGER.debug("get item for id: {} ", id);
    Promise<JsonObject> promise = Promise.promise();

    catWebClient
        .get(port, host, path)
        .addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + id + "]]")
        .addQueryParam(
            "filter",
            "[id,provider,name,description,authControlGroup,accessPolicy,type,iudxResourceAPIs,instance,resourceGroup]")
        .expect(ResponsePredicate.JSON)
        .send(
            relHandler -> {
              if (relHandler.succeeded()
                  && relHandler.result().bodyAsJsonObject().getInteger("totalHits") > 0) {
                LOGGER.debug(
                    "catalogue call search api succeeded "
                        + relHandler.result().bodyAsJsonObject().getInteger("totalHits"));
                JsonArray resultArray =
                    relHandler.result().bodyAsJsonObject().getJsonArray("results");
                JsonObject response = resultArray.getJsonObject(0);
                promise.complete(response);
              } else {
                LOGGER.debug("catalogue call search api failed: " + relHandler.cause());
                promise.fail("catalogue call search api failed");
              }
            });

    return promise.future();
  }

  @Override
  public Future<Asset> getCatalogueAsset(String itemId) {
    LOGGER.info("Fetching asset from catalogue for id: {}", itemId);
    Promise<Asset> promise = Promise.promise();
    LOGGER.info("port {}, host: {}, catalogueItemPath: {}", port, host, catalogueItemPath);

    catWebClient
        .get(port, host, catalogueItemPath)
        .addQueryParam("id", itemId)
        .expect(ResponsePredicate.JSON)
        .send(relHandler -> {
          if (relHandler.succeeded()) {
            JsonObject body = relHandler.result().bodyAsJsonObject();
            JsonArray resultArray = body.getJsonArray("result");
            if (resultArray != null && !resultArray.isEmpty()) {
              JsonObject response = resultArray.getJsonObject(0);
              promise.complete(parseAndGetAsset(response, itemId));
            } else {
              LOGGER.debug("Item not found in catalogue : {}", itemId);
              promise.fail(new OgcException(404, "Not Found", "Catalogue item not found"));
            }
          } else {
            LOGGER.debug("catalogue call to item api failed:{} ", relHandler.cause());
            promise.fail(new OgcException(500, "Internal Server Error", "catalogue call to item api failed"));

          }
        });
    return promise.future();

  }

  private Asset parseAndGetAsset(JsonObject result, String id) {
    LOGGER.debug("Asset info : {}", result.encodePrettily());
    try {
      String assetName = result.getString(ASSET_NAME_KEY, "").trim();
      String provider = result.getString(OWNER_ID);
      String organizationId = result.getString(ORGANIZATION_ID);
      String shortDescription = result.getString(SHORT_DESCRIPTION, "").trim();
      String accessPolicy = result.getString(ACCESS_POLICY);
      List<String> tags = result.getJsonArray(TAGS).getList();
      String createdAt = result.getString(CREATED_AT);


      AssetType catAssetType = null;
      JsonArray typeArray = result.getJsonArray(TYPE);
      if (typeArray != null) {
        for (Object type : typeArray) {
          String typeStr = type.toString();
          catAssetType = AssetType.fromString(typeStr);
        }
      }

      // Validation
      if (provider == null
          || assetName.isEmpty()
          || catAssetType == null
          || organizationId == null
          || shortDescription == null
          || tags == null
          || tags.isEmpty()
          || createdAt == null
          || accessPolicy == null) {
        LOGGER.error("Asset metadata invalid for id: {}", id);
        LOGGER.error(
            "Provider: {}, AssetName: {}, AssetType: {}, OrgId: {}, shortDescription : {}, accessPolicy : {}, " +
                "tags: {}, createdAt : {}",
            provider,
            assetName,
            catAssetType,
            organizationId,
            shortDescription,
            accessPolicy,
            tags,
            createdAt);
        throw new OgcException(500, "Internal Server Error", "Incomplete asset metadata from catalogue");
      }

      return new Asset()
          .setItemId(id)
          .setProviderId(provider)
          .setOrganizationId(organizationId)
          .setAssetType(catAssetType.getAssetType())
          .setAssetName(assetName)
          .setShortDescription(shortDescription)
          .setAccessPolicy(accessPolicy)
          .setTags(tags)
          .setCreatedAt(createdAt);

    } catch (Exception e) {
      LOGGER.error("Error building asset from catalogue metadata: {}", e.getMessage(), e);
      throw new OgcException(500, "Internal Server Error", "Incomplete asset metadata from catalogue");
    }
  }
}