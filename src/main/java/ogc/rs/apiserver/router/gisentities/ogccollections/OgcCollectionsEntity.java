package ogc.rs.apiserver.router.gisentities.ogccollections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.handlers.FailureHandler;
import ogc.rs.apiserver.router.gisentities.GisEntityInterface;
import ogc.rs.apiserver.router.routerbuilders.OgcRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.StacRouterBuilder;
import ogc.rs.apiserver.router.util.OasFragments;
import ogc.rs.database.DatabaseService;

/**
 * Class to handle creation of routes for <em>/collections/<collection-id></em> and
 * <em>/stac/collection/<collection-id></em> for OGC collections.
 * 
 */
@AutoService(GisEntityInterface.class)
public class OgcCollectionsEntity implements GisEntityInterface {

  private static final String UUID_REGEX =
      "[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}";

  private static final String OGC_COLLECTION_PATH_REGEX = "^/collections/" + UUID_REGEX;
  
  private static final Logger LOGGER = LogManager.getLogger(OgcCollectionsEntity.class);

  @Override
  public void giveOgcRoutes(OgcRouterBuilder ogcRouterBuilder) {

    RouterBuilder builder = ogcRouterBuilder.routerBuilder;
    ApiServerVerticle apiServerVerticle = ogcRouterBuilder.apiServerVerticle;
    FailureHandler failureHandler = ogcRouterBuilder.failureHandler;

    List<String> collectionSpecificOpIds = builder.operations().stream()
        .filter(op -> op.getOperationId().matches(OgcCollectionMetadata.OGC_OP_ID_PREFIX_REGEX))
        .map(op -> op.getOperationId()).collect(Collectors.toList());

    collectionSpecificOpIds.forEach(opId -> {
      if (opId.matches(OgcCollectionMetadata.OGC_GET_SPECIFIC_COLLECTION_OP_ID_REGEX)) {
        builder.operation(opId).handler(apiServerVerticle::getCollection)
            .handler(apiServerVerticle::putCommonResponseHeaders)
            .handler(apiServerVerticle::buildResponse)
            .failureHandler(failureHandler);
      } 
    });
  }

  @Override
  public void giveStacRoutes(StacRouterBuilder stacRouterBuilder) {

  }

  @Override
  public Future<OasFragments> generateNewSpecFragments(JsonObject existingOgcSpec,
      JsonObject existingStacSpec, DatabaseService dbService, JsonObject config) {

    Promise<OasFragments> promise = Promise.promise();
    
    Set<UUID> existingCollectionIds = getExistingCollectionsFromOgcSpec(existingOgcSpec);

    Future<List<JsonObject>> collectionMetadata =
        dbService.getCollectionMetadataForOasSpec(
            existingCollectionIds.stream().map(i -> i.toString()).collect(Collectors.toList()));

    Future<List<OgcCollectionMetadata>> metadataObjList = collectionMetadata.compose(res -> {
      List<OgcCollectionMetadata> list =
          res.stream().map(i -> new OgcCollectionMetadata(i)).collect(Collectors.toList());
      
      List<String> foundCollectionIds =
          list.stream().map(obj -> obj.getId().toString()).collect(Collectors.toList());

      if (foundCollectionIds.isEmpty()) {
        LOGGER.info("No new OGC collections found!");
      } else {
        LOGGER.info("New OGC collections found : {}", foundCollectionIds);
      }
      
      return Future.succeededFuture(list);
    });

    Future<OasFragments> result = metadataObjList.compose(list -> {
      List<JsonObject> ogcFrags = new ArrayList<JsonObject>();

      list.forEach(obj -> {
        ogcFrags.add(obj.generateOgcOasBlock());
      });

      OasFragments fragments = new OasFragments();
      fragments.setOgc(ogcFrags);

      return Future.succeededFuture(fragments);
    });

    result.onSuccess(i -> promise.complete(i)).onFailure(err -> promise.fail(err));

    return promise.future();
  }

  /**
   * Get existing OGC collections from the OGC spec. These IDs don't need to be fetched from the DB
   * and have spec generated for them. We only check the OGC spec and not the STAC spec since all
   * IDs that are there in the STAC spec will be there in the OGC spec as well.
   * 
   * @param ogcSpec the OGC OpenAPI spec JSON
   * @return
   */
  private Set<UUID> getExistingCollectionsFromOgcSpec(JsonObject ogcSpec) {
    Set<UUID> existingIds = new HashSet<UUID>();

    ogcSpec.getJsonObject("paths").forEach(k -> {
      String key = k.getKey();
      if (key.matches(OGC_COLLECTION_PATH_REGEX)) {
        String collectionId = key.split("/")[2];
        existingIds.add(UUID.fromString(collectionId));
      }
    });

    return existingIds;
  }
}
