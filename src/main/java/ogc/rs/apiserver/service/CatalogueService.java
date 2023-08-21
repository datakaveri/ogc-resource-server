package ogc.rs.apiserver.service;

import static ogc.rs.metering.util.MeteringConstant.CAT_SEARCH_PATH;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// TODO: Replace this with odc-db
public class CatalogueService {
  private static final Logger LOGGER = LogManager.getLogger(CatalogueService.class);
  final String host;
  final int port;
  final String catBasePath;
  final String path;
  WebClient catWebClient;

  public CatalogueService(Vertx vertx, JsonObject config) {
    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true).setVerifyHost(false).setSsl(true);
    catWebClient = WebClient.create(vertx, options);
    host = config.getString("catServerHost");
    port = config.getInteger("catServerPort");
    this.catBasePath = config.getString("dxCatalogueBasePath");
    this.path = catBasePath + CAT_SEARCH_PATH;
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
                JsonArray resultArray =
                    relHandler.result().bodyAsJsonObject().getJsonArray("results");
                JsonObject response = resultArray.getJsonObject(0);
                promise.complete(response);
              } else {
                LOGGER.error("catalogue call search api failed: " + relHandler.cause());
                promise.fail("catalogue call search api failed");
              }
            });

    return promise.future();
  }
}
