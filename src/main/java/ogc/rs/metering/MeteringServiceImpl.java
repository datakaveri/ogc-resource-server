package ogc.rs.metering;

import static ogc.rs.common.Constants.CATALOGUE_SERVICE_ADDRESS;
import static ogc.rs.metering.util.MeteringConstant.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueInterface;
import ogc.rs.databroker.service.DataBrokerService;
import ogc.rs.metering.util.DateValidation;
import ogc.rs.metering.util.ParamsValidation;
import ogc.rs.metering.util.QueryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService {
  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private final DateValidation dateValidation = new DateValidation();
  private final ParamsValidation paramValidation = new ParamsValidation();
  DataBrokerService dataBrokerService;
  CatalogueInterface catalogueService;
  JsonObject validationCheck = new JsonObject();
  String queryCount;
  long total;
  String queryPg;
  String queryOverview;
  String summaryOverview;
  JsonArray jsonArray;
  JsonArray resultJsonArray;
  int loopi;
  PgPool meteringPgClient;
  PgPool ogcPgClient;

    public MeteringServiceImpl(
      Vertx vertx,
      PgPool meteringPgClient,
      PgPool ogcPgClient,
      JsonObject config,
      DataBrokerService dataBrokerService) {
    this.dataBrokerService = dataBrokerService;
    this.meteringPgClient = meteringPgClient;
    this.ogcPgClient = ogcPgClient;
    catalogueService = CatalogueInterface.createProxy(vertx, CATALOGUE_SERVICE_ADDRESS);  }

  @Override
  public Future<JsonObject> executeReadQuery(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.trace("Info: Read Query" + request.toString());

    validationCheck = paramValidation.paramsCheck(request);
    if (validationCheck != null && validationCheck.containsKey(ERROR)) {
      promise.fail("Error : " + validationCheck.getString(ERROR));
      return promise.future();
    }

    String count = request.getString("options");
    if (count == null) {
      countQueryForRead(request)
          .onSuccess(promise::complete)
          .onFailure(
              fail -> {
                promise.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
              });
    } else {
      countQuery(request)
          .onSuccess(
              successCount -> {
                  JsonObject jsonObject =  new JsonObject().put(TOTALHITS, successCount);
                JsonObject jsonObjectResult = new JsonObject();
                jsonObjectResult.put("result",new JsonArray().add(jsonObject));
                promise.complete(jsonObjectResult);
              })
          .onFailure(
              fail -> {
                promise.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
              });
    }
    return promise.future();
  }

  private Future<JsonObject> countQueryForRead(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    countQuery(request)
        .onSuccess(
            successCount -> {
              if (successCount == 0) {
                OgcException ogcException = new OgcException(204, "No Content", "Zero count");
                LOGGER.info(ogcException.getJson().toString());
                  JsonObject jsonObject =  new JsonObject().put(TOTALHITS, successCount);
                  JsonObject jsonObjectResult = new JsonObject();
                  jsonObjectResult.put("result",new JsonArray().add(jsonObject));
                promise.complete(jsonObjectResult);
                return;
              } else {
                request.put(TOTALHITS, successCount);
                readMethod(request).onSuccess(promise::complete);
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure.getMessage());
              promise.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
            });
    return promise.future();
  }

  private Future<JsonObject> readMethod(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    int offset = request.getInteger(OFFSETPARAM);
    int limit = request.getInteger(LIMITPARAM);

    queryPg = queryBuilder.buildReadQueryForPg(request);
    LOGGER.info("read query = {}", queryPg);

    Future<JsonObject> resultsPg = executeQueryDatabaseOperation(queryPg);
    resultsPg.onComplete(
        readHandler -> {
          if (readHandler.succeeded()) {
            LOGGER.info("Read Completed successfully");
            JsonObject resultJsonObject = readHandler.result();
            resultJsonObject.put(LIMITPARAM, limit);
            resultJsonObject.put(OFFSETPARAM, offset);
            resultJsonObject.put(TOTALHITS, request.getLong(TOTALHITS));
            promise.complete(resultJsonObject);
          } else {
            LOGGER.debug("Could not read from DB : " + readHandler.cause());
            promise.fail(readHandler.cause().getMessage());
          }
        });
    return promise.future();
  }

  private Future<Long> countQuery(JsonObject request) {
    Promise<Long> promise = Promise.promise();
    queryCount = queryBuilder.buildCountReadQueryFromPg(request);
    LOGGER.info("count query = {}", queryCount);
    Future<JsonObject> resultCountPg = executeQueryDatabaseOperation(queryCount);
    resultCountPg.onComplete(
        countHandler -> {
          if (countHandler.succeeded()) {
            try {
              var countHandle = countHandler.result().getJsonArray("result");
              total = countHandle.getJsonObject(0).getInteger("count");
              LOGGER.info("total {}", total);
              if (total == 0) {
                OgcException ogcException = new OgcException(204, "No Content", "Zero count");
                LOGGER.info(ogcException.getJson().toString());
                promise.complete(total);
              } else {
                promise.complete(total);
              }
            } catch (NullPointerException nullPointerException) {
              LOGGER.debug(nullPointerException.toString());
            }
          }
        });
    return promise.future();
  }
    /**
     * Inserts an audit entry into the PostgreSQL metering table.
     *
     * @param request JsonObject containing metering information to be inserted.
     * @return A {@link Future<Void>} which completes when the record is inserted or fails with the corresponding error.
     */
    @Override
    public Future<Void> insertIntoPostgresAuditTable(JsonObject request) {
        Promise<Void> promise = Promise.promise();

        String sql = "INSERT INTO metering (user_id, collection_id, api_path, timestamp, resp_size) " +
                "VALUES ($1, $2, $3, $4, $5)";

        //LOGGER.debug("Metering payload: {}", request.encodePrettily());

        UUID userId = UUID.fromString(request.getString("user_id"));
        UUID collectionId = UUID.fromString(request.getString("collection_id"));
        String apiPath = request.getString("api_path");
        String timestampStr = request.getString("timestamp");
        long respSize = request.getLong("resp_size");

        LocalDateTime timestamp = ZonedDateTime.parse(timestampStr).toLocalDateTime();

        ogcPgClient.preparedQuery(sql)
                .execute(Tuple.of(userId, collectionId, apiPath, timestamp, respSize))
                .onSuccess(res -> {
                    LOGGER.debug("Inserted into Postgres metering table");
                    promise.complete();
                })
                .onFailure(err -> {
                    LOGGER.error("Failed to insert into Postgres metering table", err);
                    promise.fail(err);
                });

        return promise.future();
    }

    @Override
  public Future<JsonObject> insertMeteringValuesInRmq(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject writeMessage = queryBuilder.buildMessageForRmq(request);
    LOGGER.info("write message =  {}", writeMessage);
    dataBrokerService
        .publishMessageInternal(writeMessage,EXCHANGE_NAME, ROUTING_KEY )
        .onSuccess(
            successHandler -> {
              LOGGER.info("inserted into rmq");
              promise.complete();
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler.getMessage());
              try {
                LOGGER.debug("response from rmq ");
                promise.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
              } catch (Exception e) {
                LOGGER.error("Failure message not in format [type,title,detail]");
                promise.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> monthlyOverview(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String startTime = request.getString(STARTT);
    String endTime = request.getString(ENDT);

    if (startTime != null && endTime == null || startTime == null && endTime != null) {
        promise.fail(new OgcException(400, "Bad Request", "Bad request"));
      return promise.future();
    }

    if (startTime != null && endTime != null) {
      validationCheck = dateValidation.dateParamCheck(request);

      if (validationCheck != null && validationCheck.containsKey(ERROR)) {
          LOGGER.debug("Error:" + validationCheck.getString(ERROR));
          promise.fail(new OgcException(400, "Bad Request", "Bad request"));
        return promise.future();
      }
    }

    String role = request.getString(ROLE);
    if (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("consumer")) {
      queryOverview = queryBuilder.buildMonthlyOverview(request);
      LOGGER.debug("query Overview = " + queryOverview);

      Future<JsonObject> result = executeQueryDatabaseOperation(queryOverview);
      result.onComplete(
          handlers -> {
            if (handlers.succeeded()) {
              LOGGER.debug("Successfully");
              promise.complete(handlers.result());
            } else {
              LOGGER.debug("Could not read from DB : " + handlers.cause());
              promise.fail(new OgcException(400, "Bad Request", "Bad request"));
            }
          });
    } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
      LOGGER.debug(catalogueService);
      String resourceId = request.getString(IID);
      catalogueService
          .getCatItem(resourceId)
          .onSuccess(
              providerHandler -> {
                String providerId = providerHandler.getString("provider");
                request.put("providerid", providerId);

                queryOverview = queryBuilder.buildMonthlyOverview(request);
                LOGGER.debug("query Overview =" + queryOverview);

                Future<JsonObject> result = executeQueryDatabaseOperation(queryOverview);
                result.onComplete(
                    monthlyHandlers -> {
                      if (monthlyHandlers.succeeded()) {
                        LOGGER.debug("Successfully return result");
                        promise.complete(monthlyHandlers.result());
                      } else {
                        LOGGER.debug("Could not read " + monthlyHandlers.cause());
                        promise.fail(new OgcException(400, "Bad Request", "Bad request"));
                      }
                    });
              })
          .onFailure(fail -> {
              LOGGER.debug(fail.getMessage());
              promise.fail(new OgcException(400, "Bad Request", "Bad request"));
                  });
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> summaryOverview(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String startTime = request.getString(STARTT);
    String endTime = request.getString(ENDT);

    if (startTime != null && endTime == null || startTime == null && endTime != null) {
        promise.fail(new OgcException(400, "Bad Request", "Bad request"));
      return promise.future();
    }

    if (startTime != null && endTime != null) {
      validationCheck = dateValidation.dateParamCheck(request);
      if (validationCheck != null && validationCheck.containsKey(ERROR)) {
        LOGGER.debug("Error:" + validationCheck.getString(ERROR));
          promise.fail(new OgcException(400, "Bad Request", "Bad request"));
        return promise.future();
      }
    }

    String role = request.getString(ROLE);
    if (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("consumer")) {
      summaryOverview = queryBuilder.buildSummaryOverview(request);
      LOGGER.debug("summary query = {}", summaryOverview);
      Future<JsonObject> result = executeQueryDatabaseOperation(summaryOverview);
      result.onComplete(
          handlers -> {
            if (handlers.succeeded()) {
              jsonArray = handlers.result().getJsonArray("result");
              if (jsonArray.size() <= 0) {
                LOGGER.debug("NO Content");
                promise.fail(new OgcException(204, "No Content", "Zero count"));
                return;
              }
              collectionDetailsCall(jsonArray)
                  .onSuccess(
                      resultHandler -> {
                        JsonObject resultJson =
                            new JsonObject()
                                .put("result", resultHandler);
                        promise.complete(resultJson);
                      });
            } else {
              LOGGER.debug("Could not read from DB : " + handlers.cause());
              promise.fail(new OgcException(400, "Bad Request", "bad request"));
            }
          });
    } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
      String resourceId = request.getString(IID);

      catalogueService
          .getCatItem(resourceId)
          .onSuccess(
              providerHandler -> {
                String providerId = providerHandler.getString("provider");
                request.put("providerid", providerId);
                summaryOverview = queryBuilder.buildSummaryOverview(request);
                LOGGER.debug("summary query {}", summaryOverview);
                Future<JsonObject> result = executeQueryDatabaseOperation(summaryOverview);
                result.onComplete(
                    handlers -> {
                      if (handlers.succeeded()) {
                        jsonArray = handlers.result().getJsonArray("result");
                        if (jsonArray.size() <= 0) {
                          LOGGER.debug("NO Content");
                          promise.fail(new OgcException(204, "No Content", "Zero count"));
                          return;
                        }
                        collectionDetailsCall(jsonArray)
                            .onSuccess(
                                resultHandler -> {
                                  JsonObject resultJson =
                                      new JsonObject()
                                          .put("result", resultHandler);
                                  promise.complete(resultJson);
                                });
                      } else {
                        LOGGER.debug("Could not read from DB : " + handlers.cause());
                        promise.fail(new OgcException(400, "Bad Request", "bad request"));
                      }
                    });
              })
          .onFailure(
              fail -> {
                LOGGER.debug(fail.getMessage());
                promise.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
              });
    }

    return promise.future();
  }

  public Future<JsonArray> collectionDetailsCall(JsonArray jsonArray) {
    Promise<JsonArray> promise = Promise.promise();
    HashMap<String, Integer> resourceCount = new HashMap<>();
    resultJsonArray = new JsonArray();
    List<Future> list = new ArrayList<>();

    for (loopi = 0; loopi < jsonArray.size(); loopi++) {
      var resultJson = jsonArray.getJsonObject(loopi);
      String resourceId = resultJson.getString("resourceid");
      resourceCount.put(
          jsonArray.getJsonObject(loopi).getString("resourceid"),
          Integer.valueOf(jsonArray.getJsonObject(loopi).getString("count")));
      list.add(catalogueService.getCatItem(resourceId).recover(f -> Future.succeededFuture(null)));
    }

    CompositeFuture.join(list)
        .map(CompositeFuture::list)
        .map(result -> result.stream().filter(Objects::nonNull).collect(Collectors.toList()))
        .onSuccess(
            l -> {
              for (int i = 0; i < l.size(); i++) {
                JsonObject result = (JsonObject) l.get(i);
                JsonObject outputFormat =
                    new JsonObject()
                        .put("resourceid", result.getString("id"))
                        .put("resource_label", result.getString("description"))
                        .put("publisher", result.getString("name"))
                        .put("publisher_id", result.getString("provider"))
                        .put("city", result.getString("instance"))
                        .put("count", resourceCount.get(result.getString("id")));
                resultJsonArray.add(outputFormat);
              }
              promise.complete(resultJsonArray);
            })
        .onFailure(
            failure -> {
              LOGGER.debug(failure.getMessage());
              promise.fail(new OgcException(400, "Bad Request", "bad request"));
            });
    return promise.future();
  }

  private Future<JsonObject> executeQueryDatabaseOperation(String query) {
    Promise<JsonObject> promise = Promise.promise();
    executeQuery(query)
        .onSuccess(
            handler -> {
              promise.complete(handler);
            })
        .onFailure(
            failure -> {
                promise.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
            });

    return promise.future();
  }

  public Future<JsonObject> executeQuery(final String query) {
    Promise<JsonObject> promise = Promise.promise();
    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());
    meteringPgClient
        .withConnection(
            connection ->
                connection.query(query).collecting(rowCollector).execute().map(row -> row.value()))
        .onSuccess(
            successHandler -> {
              JsonArray result = new JsonArray(successHandler);
              JsonObject responseJson = new JsonObject().put("result", result);
              promise.complete(responseJson);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.debug(failureHandler);
              promise.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
            });
    return promise.future();
  }
}
