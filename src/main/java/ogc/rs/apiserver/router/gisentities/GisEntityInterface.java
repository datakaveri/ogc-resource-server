package ogc.rs.apiserver.router.gisentities;

import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.OgcRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.StacRouterBuilder;
import ogc.rs.apiserver.router.util.OasFragments;
import ogc.rs.database.DatabaseService;

/**
 * An interface that a GIS entity can use to support OpenAPI spec generation and to supply routes
 * when building the router via a {@link RouterBuilder}. Classes that implement this interface are
 * accessed using Java SPI and can be annotated with
 * <code>@AutoService(GisEntityInterface.class)</code> ({@link AutoService}).
 * 
 */
public interface GisEntityInterface {

  /**
   * Add OGC-specific routes to the {@link OgcRouterBuilder}.
   * 
   * @param routerBuilder an instance of {@link OgcRouterBuilder}, which contains a
   *        {@link RouterBuilder}
   */
  void giveOgcRoutes(OgcRouterBuilder routerBuilder);

  /**
   * Add STAC-specific routes to the {@link StacRouterBuilder}.
   * 
   * @param routerBuilder an instance of {@link StacRouterBuilder}, which contains a
   *        {@link RouterBuilder}
   */
  void giveStacRoutes(StacRouterBuilder routerBuilder);

  /**
   * Generate OGC-specific and STAC-specific OpenAPI spec JSON fragments based on the GIS entities
   * requirements.
   * 
   * @param existingOgcSpec the existing OGC OpenAPI spec in JSON
   * @param existingStacSpec the existing STAC OpenAPI spec in JSON
   * @param dbService an instance of the {@link DatabaseService}
   * @param config {@link JsonObject} config
   * @return {@link OasFragments} class, which contains OGC and STAC OpenAPI spec fragments in JSON
   */
  Future<OasFragments> generateNewSpecFragments(JsonObject existingOgcSpec,
      JsonObject existingStacSpec, DatabaseService dbService, JsonObject config);
}
