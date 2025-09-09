package ogc.rs.catalogue;

import static ogc.rs.apiserver.authorization.util.Constants.*;
import static ogc.rs.common.Constants.CAT_SEARCH_PATH;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import java.util.Map;
import ogc.rs.apiserver.authorization.model.Asset;
import ogc.rs.apiserver.authorization.model.AssetType;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CatalogueService {
  private static final Logger LOGGER = LogManager.getLogger(CatalogueService.class);
  public static WebClient catWebClient;
  final String host;
  final int port;
  final String catBasePath;
  final String path;
  final String catalogueItemPath;

  public CatalogueService(Vertx vertx, JsonObject config) {
    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(false).setVerifyHost(true).setSsl(true);
    catWebClient = WebClient.create(vertx, options);
    host = config.getString("catServerHost");
    port = config.getInteger("catServerPort");
    this.catBasePath = config.getString("dxCatalogueBasePath");
    this.path = catBasePath + CAT_SEARCH_PATH;
    this.catalogueItemPath = config.getString("catRequestItemsUri");
  }

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


  public Future<Asset> getCatalogueAsset(String itemId) {
    LOGGER.info("Fetching asset from catalogue for id: {}", itemId);
    Promise<Asset> promise = Promise.promise();
    //TODO: Remove the hardcoded item id

    catWebClient
        .get(port, host, catalogueItemPath)
        .addQueryParam("id", "a7815cb3-fcf2-4616-961a-07913560db81")
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
            LOGGER.debug("catalogue call to item api failed:{} " , relHandler.cause());
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
      || accessPolicy == null) {
        LOGGER.error("Asset metadata invalid for id: {}", id);
        LOGGER.error(
            "Provider: {}, AssetName: {}, AssetType: {}, OrgId: {}, shortDescription : {}, accessPolicy : {}",
            provider,
            assetName,
            catAssetType,
            organizationId,
            shortDescription,
            accessPolicy);
        throw new OgcException(500, "Internal Server Error","Incomplete asset metadata from catalogue");
      }

      return new Asset()
          .setItemId(id)
          .setProviderId(provider)
          .setOrganizationId(organizationId)
          .setAssetType(catAssetType.getAssetType())
          .setAssetName(assetName)
          .setShortDescription(shortDescription)
          .setAccessPolicy(accessPolicy);

    } catch (Exception e) {
      LOGGER.error("Error building asset from catalogue metadata: {}", e.getMessage(), e);
      throw new OgcException(500, "Internal Server Error","Incomplete asset metadata from catalogue");
    }
  }
}
