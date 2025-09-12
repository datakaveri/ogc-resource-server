package ogc.rs.catalogue;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import ogc.rs.apiserver.authorization.model.Asset;

@VertxGen
@ProxyGen
public interface CatalogueInterface {

  @GenIgnore
  static CatalogueInterface createProxy(Vertx vertx, String address) {
    return new CatalogueInterfaceVertxEBProxy(vertx, address);
  }


  Future<JsonObject> getCatItem(String id);

  Future<Asset> getCatalogueAsset(String itemId);
}