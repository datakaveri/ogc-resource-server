package ogc.rs.apiserver.router.gisentities.ogcRecords;

import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.util.*;
import java.util.stream.Collectors;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.handlers.FailureHandler;
import ogc.rs.apiserver.router.gisentities.GisEntityInterface;
import ogc.rs.apiserver.router.routerbuilders.OgcRouterBuilder;
import ogc.rs.apiserver.router.routerbuilders.StacRouterBuilder;
import ogc.rs.apiserver.router.util.OasFragments;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@AutoService(GisEntityInterface.class)

public class OgcRecordsEntity implements GisEntityInterface{

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}";

    private static final String OGC_FEATURE_COLLECTION_PATH_REGEX =
            "^/collections/" + UUID_REGEX + "/items";

    private static final Logger LOGGER = LogManager.getLogger(OgcRecordsEntity.class);


    @Override
    public void giveOgcRoutes(OgcRouterBuilder ogcRouterBuilder) {
        RouterBuilder builder = ogcRouterBuilder.routerBuilder;
        ApiServerVerticle apiServerVerticle = ogcRouterBuilder.apiServerVerticle;
        FailureHandler failureHandler = ogcRouterBuilder.failureHandler;

        List<String> recordSpecificOpIds =
                builder.operations().stream()
                        .filter(
                                op-> op.getOperationId().matches(OgcRecordsMetadata.OGC_RECORD_OP_ID_PREFIX_REGEX))
                        .map(op-> op.getOperationId())
                        .collect(Collectors.toList());

        recordSpecificOpIds.forEach(
                opId -> {

                    if (opId.matches(
                            OgcRecordsMetadata.OGC_GET_SPECIFIC_RECORD_COLLECTION_OP_ID_REGEX)) {
                        builder
                                .operation(opId)
                                .handler(apiServerVerticle::getRecordCatalog)
                                .handler(apiServerVerticle::putCommonResponseHeaders)
                                .handler(apiServerVerticle::buildResponse)
                                .failureHandler(failureHandler);

                    } else if(opId.matches(
                            OgcRecordsMetadata.OGC_RECORD_GET_ITEMS_COLLECTION_OP_ID_REGEX)) {
                        builder
                                .operation(opId)
                                .handler(apiServerVerticle::getRecordItems)
                                .handler(apiServerVerticle::putCommonResponseHeaders)
                                .handler(apiServerVerticle::buildResponse)
                                .failureHandler(failureHandler);
                    } else if(opId.matches(
                            OgcRecordsMetadata.OGC_RECORD_GET_SPECIFIC_RECORD_ITEM_OP_ID_REGEX)) {
                        builder
                                .operation(opId)
                                .handler(apiServerVerticle::getRecordItem)
                                .handler(apiServerVerticle::putCommonResponseHeaders)
                                .handler(apiServerVerticle::buildResponse)
                                .failureHandler(failureHandler);
                    }
                });

    }

    @Override
    public void giveStacRoutes(StacRouterBuilder routerBuilder) {

    }

    @Override
    public Future<OasFragments> generateNewSpecFragments(JsonObject existingOgcSpec, JsonObject existingStacSpec, DatabaseService dbService, JsonObject config) {

        Promise<OasFragments> promise = Promise.promise();
        Set<UUID> existingRecordCatalogIds = getExistingCollectionsFromOgcSpec(existingOgcSpec);

        Future<List<JsonObject>> recordMetadata =
                dbService.getOgcRecordMetadataForOasSpec(
                        existingRecordCatalogIds.stream().map(i-> i.toString()).collect(Collectors.toList()));

        Future<List<OgcRecordsMetadata>> metadataObjList =
                recordMetadata.compose(
                        res-> {
                           List<OgcRecordsMetadata> list =
                           res.stream().map(i-> new OgcRecordsMetadata(i)).collect(Collectors.toList());

                           List<String> foundCollectionIds =
                                   list.stream().map(obj-> obj.getId().toString()).collect(Collectors.toList());


                            if (foundCollectionIds.isEmpty()) {
                                LOGGER.info("No new Record collections found!");
                            } else {
                                LOGGER.info("New RECORD collections found : {}", foundCollectionIds);
                            }

                            return Future.succeededFuture(list);

                        });
        Future<OasFragments> result =
                metadataObjList.compose(
                        list ->{
                            List<JsonObject> ogcRecordFrags = new ArrayList<JsonObject>();

                            list.forEach(
                                    obj->{
                                        ogcRecordFrags.add(obj.generateRecordOasBlock());
                                    });
                            OasFragments fragments = new OasFragments();
                            fragments.setOgc(ogcRecordFrags);

                            return Future.succeededFuture(fragments);
                        });

        result.onSuccess(i -> promise.complete(i)).onFailure(err -> promise.fail(err));
        return  promise.future();


    }

    private Set<UUID> getExistingCollectionsFromOgcSpec(JsonObject ogcSpec) {
        Set<UUID> existingIds = new HashSet<UUID>();
        ogcSpec
                .getJsonObject("paths")
                .forEach(
                        k->{
                            String key = k.getKey();
                            if(key.matches(OGC_FEATURE_COLLECTION_PATH_REGEX)) {
                                String recordCatalogId = key.split("/")[2];
                                existingIds.add(UUID.fromString(recordCatalogId));
                            }
                        }
                );
        return existingIds;
    }
}
