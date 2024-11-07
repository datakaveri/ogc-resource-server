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
  private final LocalMap<MeteringInfo, Integer> meteringDataMap;

  /**
   * Constructs a TilesMeteringHandler instance.
   *
   * @param vertx the Vert.x instance used for shared data and scheduling periodic tasks.
   * @param config a JsonObject containing configuration for initializing CatalogueService. This
   *     handler initializes: 1. A connection to the CatalogueService using the provided
   *     configuration. 2. A metering service proxy for publishing data to RMQ.
   *     <p>A periodic task is scheduled every 2 seconds, which: - Retrieves the shared data map
   *     (LocalMap) storing metering information. - Iterates through each entry in the map: -
   *     Removes the entry after retrieving its value. - Logs the removed entry and its value. -
   *     Publishes the metering data (converted to JSON) to RMQ. - Logs success or failure for each
   *     message published to RMQ.
   */
  public TilesMeteringHandler(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.catalogueService = new CatalogueService(vertx, config);
    this.meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    this.meteringDataMap = vertx.sharedData().getLocalMap("MeteringDataMap");

    // Set up a periodic task to clean up the shared data map
    vertx.setPeriodic(
        METERING_UPDATE_PERIOD,
        id -> {
          meteringDataMap
              .keySet()
              .forEach(
                  key -> {
                    Integer value = meteringDataMap.remove(key);
                    if (value != null) {
                      LOGGER.debug(this + " removed " + key.toJson(value) + " " + value);
                      meteringService
                          .insertMeteringValuesInRmq(key.toJson(value))
                          .onComplete(
                              handler -> {
                                if (handler.succeeded()) {
                                  LOGGER.debug("message published in RMQ.");
                                } else {
                                  LOGGER.error("failed to publish message in RMQ.");
                                }
                              });

                    } else {
                      LOGGER.error(this + " NOT removed " + key);
                    }
                  });
        });
  }

  /**
   * Handles metering for tile requests based on response status and user authentication.
   *
   * @param routingContext the RoutingContext for the current request, providing request and
   *     response details. This method: 1. Filters responses to audit only specific status codes
   *     (200, 201). If the response status code does not match, it returns immediately. 2.
   *     Retrieves authentication information, including the resource ID, from the RoutingContext.
   *     3. Initializes a JSON request object containing the request body, if available. 4. Uses
   *     `catalogueService` to fetch catalog data for the resource. On successful fetch: - Extracts
   *     the resource group and provider ID. - Builds a base URL for the tile request by stripping
   *     the last three path segments (tileMatrix, tileRow, tileColumn). 5. Creates a `MeteringInfo`
   *     instance with the necessary metering data, including user info, resource details, and
   *     request size. 6. Inserts or updates the entry in a shared `LocalMap` (meteringMap) using
   *     `api path and userId' as the key. - If the key exists, increments the stored size value; if
   *     not, sets it to the initial size. 7. Logs if the catalog item is not found or if the
   *     metering service call fails.
   */
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

    catalogueService
        .getCatItem(resourceId)
        .onComplete(
            relHandler -> {
              if (relHandler.succeeded()) {
                JsonObject cacheResult = relHandler.result();
                String resourceGroup =
                    cacheResult.getString(RESOURCE_GROUP, cacheResult.getString(ID));

                String providerId = cacheResult.getString("provider");
                // Extract the base URL dynamically from the request path
                String fullPath = routingContext.request().path();

                String[] segments = fullPath.split("/");
                // Join the segments up to the last three removing tileMatrix, tileRow, tileColumn
                String baseTileUrl = String.join("/", Arrays.copyOf(segments, segments.length - 3));

                MeteringInfo meteringInfo =
                    new MeteringInfo(
                        authInfo,
                        resourceGroup,
                        providerId,
                        baseTileUrl,
                        routingContext.response().bytesWritten(),
                        reqBody);

                meteringDataMap.compute(
                    meteringInfo,
                    (k, v) -> (v == null) ? meteringInfo.getSize() : v + meteringInfo.getSize());

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
