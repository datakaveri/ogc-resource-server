package ogc.rs.apiserver.router.gisentities.ogcfeatures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.handlers.AuthHandler;
import ogc.rs.apiserver.router.gisentities.GisEntityInterface;
import ogc.rs.apiserver.router.routerbuilders.OgcRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.StacRouterBuilder;
import ogc.rs.apiserver.router.util.OasFragments;
import ogc.rs.database.DatabaseService;

/**
 * Class to handle creation of routes for OGC Feature datasets.
 * 
 */
@AutoService(GisEntityInterface.class)
public class OgcFeaturesEntity implements GisEntityInterface {

  private static final String OGC_COLLECTION_PATH_REGEX =
      "^/collections/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}";
  
  private static final Logger LOGGER = LogManager.getLogger(OgcFeaturesEntity.class);

  @Override
  public void giveOgcRoutes(OgcRouterBuilder ogcRouterBuilder) {

    RouterBuilder builder = ogcRouterBuilder.routerBuilder;
    ApiServerVerticle apiServerVerticle = ogcRouterBuilder.apiServerVerticle;
    Vertx vertx = ogcRouterBuilder.vertx;

    builder.operation("getCollections").handler(apiServerVerticle::getCollections)
        .handler(apiServerVerticle::putCommonResponseHeaders)
        .handler(apiServerVerticle::buildResponse);

    List<String> collectionSpecificOpIds = builder.operations().stream()
        .filter(op -> op.getOperationId().matches(CollectionMetadata.OGC_OP_ID_PREFIX_REGEX))
        .map(op -> op.getOperationId()).collect(Collectors.toList());

    collectionSpecificOpIds.forEach(opId -> {
      if (opId.matches(CollectionMetadata.OGC_GET_SPECIFIC_COLLECTION_OP_ID_REGEX)) {
        builder.operation(opId).handler(apiServerVerticle::getCollection)
            .handler(apiServerVerticle::putCommonResponseHeaders)
            .handler(apiServerVerticle::buildResponse);

      } else if (opId.matches(CollectionMetadata.OGC_GET_COLLECTION_ITEMS_OP_ID_REGEX)) {
        builder.operation(opId).handler(AuthHandler.create(vertx))
            .handler(apiServerVerticle::validateQueryParams)
            .handler(apiServerVerticle::getFeatures)
            .handler(apiServerVerticle::putCommonResponseHeaders)
            .handler(apiServerVerticle::buildResponse);

      } else if (opId.matches(CollectionMetadata.OGC_GET_SPECIFIC_FEATURE_OP_ID_REGEX)) {
        builder.operation(opId).handler(AuthHandler.create(vertx))
            .handler(apiServerVerticle::validateQueryParams)
            .handler(apiServerVerticle::getFeature)
            .handler(apiServerVerticle::putCommonResponseHeaders)
            .handler(apiServerVerticle::buildResponse);
      }
    });
  }

  @Override
  public void giveStacRoutes(StacRouterBuilder stacRouterBuilder) {

    RouterBuilder builder = stacRouterBuilder.routerBuilder;
    ApiServerVerticle apiServerVerticle = stacRouterBuilder.apiServerVerticle;

    List<String> collectionSpecificOpIds = builder.operations().stream()
        .filter(op -> op.getOperationId().matches(CollectionMetadata.STAC_OP_ID_PREFIX_REGEX))
        .map(op -> op.getOperationId()).collect(Collectors.toList());

    collectionSpecificOpIds.forEach(opId -> {
      if (opId.matches(CollectionMetadata.STAC_GET_SPECIFIC_ITEMLESS_COLLECTION_OP_ID_REGEX)) {
        builder.operation(opId).handler(apiServerVerticle::getStacCollection)
            .handler(apiServerVerticle::putCommonResponseHeaders)
            .handler(apiServerVerticle::buildResponse);
      }
    });
  }

  @Override
  public Future<OasFragments> generateNewSpecFragments(JsonObject existingOgcSpec,
      JsonObject existingStacSpec, DatabaseService dbService, JsonObject config) {

    Promise<OasFragments> promise = Promise.promise();
    
    Set<UUID> existingCollectionIds = getExistingCollectionsFromOgcSpec(existingOgcSpec);

    Future<List<JsonObject>> collectionMetadata =
        dbService.getOgcFeatureCollectionMetadataForOasSpec(
            existingCollectionIds.stream().map(i -> i.toString()).collect(Collectors.toList()));

    Future<List<CollectionMetadata>> metadataObjList = collectionMetadata.compose(res -> {
      List<CollectionMetadata> list =
          res.stream().map(i -> new CollectionMetadata(i)).collect(Collectors.toList());
      
      List<String> foundCollectionIds =
          list.stream().map(obj -> obj.getId().toString()).collect(Collectors.toList());

      if (foundCollectionIds.isEmpty()) {
        LOGGER.info("No new OGC Feature collections found!");
      } else {
        LOGGER.info("New OGC Feature collections found : {}", foundCollectionIds);
      }
      
      return Future.succeededFuture(list);
    });

    Future<OasFragments> result = metadataObjList.compose(list -> {
      List<JsonObject> ogcFrags = new ArrayList<JsonObject>();
      List<JsonObject> stacFrags = new ArrayList<JsonObject>();

      JsonObject geomMaxLimitConfig = config.getJsonObject("geomSpecificMaxLimits", new JsonObject());

      /*
       * if geomSpecificMaxLimits is not in config, then set max for all geometries to
       * CollectionMetadata.OGC_LIMIT_PARAM_MAX_DEFAULT)
       */
      Map<PostgisGeomTypes, Integer> geomSpecificMaxLimits = Arrays.stream(PostgisGeomTypes.values())
          .collect(Collectors.toMap(type -> type, type -> geomMaxLimitConfig
              .getInteger(type.toString(), CollectionMetadata.OGC_LIMIT_PARAM_MAX_DEFAULT)));
      
      list.forEach(obj -> {
        ogcFrags.add(obj.generateOgcOasBlock(geomSpecificMaxLimits));
        stacFrags.add(obj.generateStacOasBlock());
      });

      OasFragments fragments = new OasFragments();
      fragments.setOgc(ogcFrags);
      fragments.setStac(stacFrags);

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
