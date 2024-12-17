package ogc.rs.apiserver.router.gisentities.staccollections;

import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.handlers.FailureHandler;
import ogc.rs.apiserver.router.gisentities.GisEntityInterface;
import ogc.rs.apiserver.router.gisentities.ogccollections.OgcCollectionMetadata;
import ogc.rs.apiserver.router.gisentities.ogccollections.OgcCollectionsEntity;
import ogc.rs.apiserver.router.routerbuilders.OgcRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.StacRouterBuilder;
import ogc.rs.apiserver.router.util.OasFragments;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to handle creation of routes for /stac/collection/<collection-id></em> for OGC collections.
 */
@AutoService(GisEntityInterface.class)
public class StacCollectionsEntity implements GisEntityInterface {

  private static final String UUID_REGEX =
      "[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}";
  private static final String STAC_COLLECTION_PATH_REGEX = "^stac/collections/" + UUID_REGEX;

  private static final Logger LOGGER = LogManager.getLogger(StacCollectionsEntity.class);

  @Override
  public void giveOgcRoutes(OgcRouterBuilder routerBuilder) {}

  @Override
  public void giveStacRoutes(StacRouterBuilder stacRouterBuilder) {
    RouterBuilder builder = stacRouterBuilder.routerBuilder;
    ApiServerVerticle apiServerVerticle = stacRouterBuilder.apiServerVerticle;
    FailureHandler failureHandler = stacRouterBuilder.failureHandler;

    List<String> collectionSpecificOpIds =
        builder.operations().stream()
            .filter(
                op -> op.getOperationId().matches(StacCollectionMetadata.STAC_OP_ID_PREFIX_REGEX))
            .map(op -> op.getOperationId())
            .collect(Collectors.toList());

    collectionSpecificOpIds.forEach(
        opId -> {
          if (opId.matches(
              StacCollectionMetadata.STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_OP_ID_REGEX)) {
            builder
                .operation(opId)
                .handler(apiServerVerticle::getStacCollection)
                .handler(apiServerVerticle::putCommonResponseHeaders)
                .handler(apiServerVerticle::buildResponse)
                .failureHandler(failureHandler);
          } else if (opId.matches(StacCollectionMetadata.STAC_GET_ITEMS_COLLECTION_OP_ID_REGEX)) {
            builder
                .operation(opId)
                .handler(apiServerVerticle::getStacItems)
                .handler(apiServerVerticle::putCommonResponseHeaders)
                .handler(apiServerVerticle::buildResponse)
                .failureHandler(failureHandler);
          }
        });
  }

  @Override
  public Future<OasFragments> generateNewSpecFragments(
      JsonObject existingOgcSpec,
      JsonObject existingStacSpec,
      DatabaseService dbService,
      JsonObject config) {
    Promise<OasFragments> promise = Promise.promise();
    Set<UUID> existingCollectionIds = getExistingCollectionsFromOgcSpec(existingStacSpec);

    Future<List<JsonObject>> stacCollectionMetadata =
        dbService.getStacCollectionMetadataForOasSpec(
            existingCollectionIds.stream().map(i -> i.toString()).collect(Collectors.toList()));

    Future<List<StacCollectionMetadata>> metadataObjList =
        stacCollectionMetadata.compose(
            res -> {
              List<StacCollectionMetadata> list =
                  res.stream().map(i -> new StacCollectionMetadata(i)).collect(Collectors.toList());

              List<String> foundCollectionIds =
                  list.stream().map(obj -> obj.getId().toString()).collect(Collectors.toList());

              if (foundCollectionIds.isEmpty()) {
                LOGGER.info("No new STAC collections found!");
              } else {
                LOGGER.info("New STAC collections found : {}", foundCollectionIds);
              }

              return Future.succeededFuture(list);
            });

    Future<OasFragments> result =
        metadataObjList.compose(
            list -> {
              List<JsonObject> stacFrags = new ArrayList<JsonObject>();

              list.forEach(
                  obj -> {
                    stacFrags.add(obj.generateStacOasBlock());
                  });

              OasFragments fragments = new OasFragments();
              fragments.setStac(stacFrags);

              return Future.succeededFuture(fragments);
            });

    result.onSuccess(i -> promise.complete(i)).onFailure(err -> promise.fail(err));
    return promise.future();
  }

  private Set<UUID> getExistingCollectionsFromOgcSpec(JsonObject ogcSpec) {
    Set<UUID> existingIds = new HashSet<UUID>();

    ogcSpec
        .getJsonObject("paths")
        .forEach(
            k -> {
              String key = k.getKey();
              if (key.matches(STAC_COLLECTION_PATH_REGEX)) {
                String collectionId = key.split("/")[2];
                existingIds.add(UUID.fromString(collectionId));
              }
            });

    return existingIds;
  }
}
