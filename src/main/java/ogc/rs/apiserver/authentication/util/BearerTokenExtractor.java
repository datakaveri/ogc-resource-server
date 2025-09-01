package ogc.rs.apiserver.authentication.util;

import io.vertx.ext.web.RoutingContext;

public class BearerTokenExtractor {
  public static String extract(RoutingContext ctx) {
    String auth = ctx.request().getHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) {
      return auth.substring(7).trim();
    }
    return null;
  }
}