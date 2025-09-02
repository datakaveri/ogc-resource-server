package ogc.rs.catalogue;

import static ogc.rs.common.Constants.CAT_SEARCH_PATH;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
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

  public Future<String> getCatalogueItemAccessPolicy(String itemId) {
    LOGGER.debug("Calling catalogue item endpoint for id: {} ", itemId);
    Promise<String> promise = Promise.promise();

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
              String accessPolicy = response.getString("accessPolicy");
              LOGGER.debug("Access policy for item {} is {}", itemId, accessPolicy);
              promise.complete(accessPolicy);
            } else {
              LOGGER.debug("Item not found in catalogue : {}", itemId);
              promise.fail("Item not found in catalogue : " + itemId);
            }
          } else {
            LOGGER.debug("catalogue call to item api failed: " + relHandler.cause());
            promise.fail("catalogue call to item api failed");
          }
        });

    return promise.future();
  }

    public Future<JsonObject> getCatItemUsingFilter(String id, String filter) {
        LOGGER.debug("get item for id: {} ", id);
        Promise<JsonObject> promise = Promise.promise();

        catWebClient
                .get(port, host, path)
                .addQueryParam("property", "[id]")
                .addQueryParam("value", "[[" + id + "]]")
                .addQueryParam(
                        "filter",
                        filter)
                .expect(ResponsePredicate.JSON)
                .send(
                        relHandler -> {
                            if (relHandler.succeeded()) {
                                LOGGER.debug(
                                        "catalogue call search api succeeded "
                                                + relHandler.result().bodyAsJsonObject().getInteger("totalHits"));
                                if (relHandler.result().bodyAsJsonObject().getInteger("totalHits") == 0) {
                                    LOGGER.debug("Item " +id+" doesn't exist in catalogue");
                                    promise.fail(new OgcException(404, "Item Not Found", "Item doesn't exist in catalogue"));
                                    return;
                                }
                                JsonArray resultArray =
                                        relHandler.result().bodyAsJsonObject().getJsonArray("results");
                                JsonObject response = resultArray.getJsonObject(0);
                                promise.complete(response);
                            } else {
                                LOGGER.debug(
                                        "catalogue call search api failed: " + relHandler.toString());
                                promise.fail("catalogue call search api failed");

                            }
                        });

        return promise.future();
    }
}
