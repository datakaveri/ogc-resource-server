package ogc.rs.apiserver.authorization.util;

import io.vertx.ext.web.RoutingContext;

public class RoutingContextHelper {

  public static void addCollectionId(RoutingContext routingContext, String collectionId) {
    routingContext.put("collectionId", collectionId);
  }

  public static String getCollectionId(RoutingContext routingContext) {
    return routingContext.get("collectionId");
  }
}
