package ogc.rs.metering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import ogc.rs.apiserver.service.CatalogueService;
import ogc.rs.apiserver.util.Response;
import ogc.rs.apiserver.util.ResponseUrn;
import ogc.rs.database.DatabaseService;
import ogc.rs.databroker.DataBrokerService;
import ogc.rs.metering.util.QueryBuilder;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.common.Constants.DATABROKER_SERVICE_ADDRESS;
import static ogc.rs.metering.util.MeteringConstant.*;

public class MeteringServiceImpl implements MeteringService {
  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  final String host;
  final int port;
  final String catBasePath;
  final String path;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private final ObjectMapper objectMapper = new ObjectMapper();
  public DataBrokerService rmqService;
  public CatalogueService catalogueService;
  WebClient catWebClient;

  public MeteringServiceImpl(
      Vertx vertx,
      DatabaseService databaseService,
      JsonObject config,
      DataBrokerService dataBrokerService) {
    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true).setVerifyHost(false).setSsl(true);
    catWebClient = WebClient.create(vertx, options);
    host = config.getString("catServerHost");
    port = config.getInteger("catServerPort");
    this.rmqService = dataBrokerService;
    this.catBasePath = config.getString("dxCatalogueBasePath");
    this.path = catBasePath + CAT_SEARCH_PATH;
    catalogueService = new CatalogueService(vertx, config);
  }

  @Override
  public Future<JsonObject> executeReadQuery(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> insertMeteringValuesInRmq(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject writeMessage = queryBuilder.buildMessageForRmq(request);
    LOGGER.info("write message =  {}", writeMessage);
    // TODO: Change Exchange Name after discussion
    rmqService
        .publishMessage(EXCHANGE_NAME, ROUTING_KEY, writeMessage)
        .onSuccess(
            successHandler -> {
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.SUCCESS_URN.getUrn())
                      .withStatus(HttpStatus.SC_OK)
                      .withDetail(ResponseUrn.SUCCESS_URN.getMessage())
                      .build();
              promise.complete(response.toJson());
              LOGGER.info("inserted into rmq");
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler.getMessage());
              try {
                Response resp = objectMapper.readValue(failureHandler.getMessage(), Response.class);
                LOGGER.debug("response from rmq " + resp);
                promise.fail(resp.toString());
              } catch (JsonProcessingException e) {
                LOGGER.error("Failure message not in format [type,title,detail]");
                promise.fail(e.getMessage());
              }
            });

    return promise.future();
  }

  @Override
  public Future<JsonObject> monthlyOverview(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> summaryOverview(JsonObject request) {
    return null;
  }
}
