package ogc.rs.apiserver.handlers;

import static ogc.rs.common.Constants.*;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import java.util.Arrays;
import java.util.List;
import ogc.rs.apiserver.util.AuthInfo;
import ogc.rs.apiserver.util.MeteringInfo;
import ogc.rs.catalogue.CatalogueService;
import ogc.rs.metering.MeteringService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TilesMeteringHandler implements Handler<Void> {

    private static final Logger LOGGER = LogManager.getLogger(TilesMeteringHandler.class);

    private final CatalogueService catalogueService;
    private final MeteringService meteringService;
    private final Vertx vertx;
    Promise<Void> promise = Promise.promise();


    public TilesMeteringHandler(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.catalogueService = new CatalogueService(vertx, config);
        this.meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);

        // Set up a periodic task to clean up the shared data map
        vertx.setPeriodic(2000, id -> {
            LocalMap<MeteringInfo, Integer> meteringMap = vertx.sharedData().getLocalMap("NAME");
            meteringMap.keySet().forEach(key -> {
                Integer value = meteringMap.remove(key);
                if (value != null) {
                    LOGGER.info(this + " removed " + key.toJson() + " " + value);
                    meteringService.insertMeteringValuesInRmq(key.toJson())
                            .onComplete(
                                    handler -> {
                                        if (handler.succeeded()) {
                                            LOGGER.debug("message published in RMQ.");
                                            promise.complete();
                                        } else {
                                            LOGGER.error("failed to publish message in RMQ.");
                                            promise.complete();
                                        }
                                    });

                } else {
                    LOGGER.info(this + " NOT removed " + key);
                }
            });
        });
    }

    public void handleMetering(RoutingContext routingContext) {

        final List<Integer> STATUS_CODES_TO_AUDIT = List.of(200, 201);
        if (!STATUS_CODES_TO_AUDIT.contains(routingContext.response().getStatusCode())) {
            return;
        }

        AuthInfo authInfo = (AuthInfo) routingContext.data().get(DxTokenAuthenticationHandler.USER_KEY);
        String resourceId = authInfo.getResourceId().toString();
        JsonObject request = new JsonObject();
        JsonObject reqBody = routingContext.body().asJsonObject();
        request.put(REQUEST_JSON, reqBody != null ? reqBody : new JsonObject());

        catalogueService.getCatItem(resourceId).onComplete(relHandler -> {
            if (relHandler.succeeded()) {
                JsonObject cacheResult = relHandler.result();
                String resourceGroup = cacheResult.getString(RESOURCE_GROUP, cacheResult.getString(ID));

                String providerId = cacheResult.getString("provider");
                // Extract the base URL dynamically from the request path
                String fullPath = routingContext.request().path();

                String[] segments = fullPath.split("/");
                // Join the segments up to the last three removing tileMatrix, tileRow, tileColumn
                String baseTileUrl = String.join("/", Arrays.copyOf(segments, segments.length - 3));

                MeteringInfo meteringInfo = new MeteringInfo(
                        authInfo, resourceGroup, providerId, baseTileUrl,
                        routingContext.response().bytesWritten(),
                        reqBody
                );

                LocalMap<MeteringInfo, Integer> meteringMap = vertx.sharedData().getLocalMap("NAME");

                meteringMap.compute(meteringInfo, (k, v) -> (v == null) ? meteringInfo.getSize() : v + meteringInfo.getSize());

            } else {
                LOGGER.debug("Item not found, metering service call failed");
            }
        });
    }

    @Override
    public void handle(Void event) {
        // Implement any necessary actions when the handler is triggered
    }
}