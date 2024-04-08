package ogc.rs.metering;

import static ogc.rs.metering.util.MeteringConstant.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.catalogue.CatalogueService;
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
  CatalogueService catalogueService;
  JsonObject validationCheck = new JsonObject();
  String queryCount;
  long total;
  String queryPg;
  String queryOverview;
  String summaryOverview;
  JsonArray jsonArray;
  JsonArray resultJsonArray;
  int loopi;
  PgPool meteringpgClient;

  public MeteringServiceImpl(
      Vertx vertx,
      PgPool meteringpgClient,
      JsonObject config,
      DataBrokerService dataBrokerService) {
    this.dataBrokerService = dataBrokerService;
    this.meteringpgClient = meteringpgClient;
    catalogueService = new CatalogueService(vertx, config);
  }

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
                promise.fail(fail.getMessage());
              });
    } else {
      countQuery(request)
          .onSuccess(
              successCount -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.put(TOTALHITS, successCount);
                promise.complete(jsonObject);
              })
          .onFailure(
              fail -> {
                promise.fail(fail.getMessage());
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
                promise.complete();
                return;
              } else {
                request.put(TOTALHITS, successCount);
                readMethod(request).onSuccess(promise::complete);
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure.getMessage());
              promise.fail(failure.getMessage());
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

  @Override
  public Future<JsonObject> insertMeteringValuesInRmq(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject writeMessage = queryBuilder.buildMessageForRmq(request);
    LOGGER.info("write message =  {}", writeMessage);
    dataBrokerService
        .publishMessage(EXCHANGE_NAME, ROUTING_KEY, writeMessage)
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
                promise.fail(failureHandler.toString());
              } catch (Exception e) {
                LOGGER.error("Failure message not in format [type,title,detail]");
                promise.fail(e.getMessage());
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
      promise.fail("Bad Request");
      return promise.future();
    }

    if (startTime != null && endTime != null) {
      validationCheck = dateValidation.dateParamCheck(request);

      if (validationCheck != null && validationCheck.containsKey(ERROR)) {
        promise.fail(validationCheck.getString(ERROR));
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
              promise.fail(handlers.cause().getMessage());
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
                        promise.fail(monthlyHandlers.cause().getMessage());
                      }
                    });
              })
          .onFailure(fail -> LOGGER.debug(fail.getMessage()));
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> summaryOverview(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String startTime = request.getString(STARTT);
    String endTime = request.getString(ENDT);

    if (startTime != null && endTime == null || startTime == null && endTime != null) {
      promise.fail("Bad Request");
      return promise.future();
    }

    if (startTime != null && endTime != null) {
      validationCheck = dateValidation.dateParamCheck(request);

      if (validationCheck != null && validationCheck.containsKey(ERROR)) {
        promise.fail(validationCheck.getString(ERROR));
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
                OgcException ogcException = new OgcException(204, "No Content", "Zero count");
                LOGGER.debug(ogcException.getJson().toString());
                promise.fail(ogcException.getJson().toString());
                return;
              }
              collectionDetailsCall(jsonArray)
                  .onSuccess(
                      resultHandler -> {
                        JsonObject resultJson =
                            new JsonObject()
                                .put("type", "urn:dx:dm:Success")
                                .put("title", "Success")
                                .put("results", resultHandler);
                        promise.complete(resultJson);
                      });
            } else {
              LOGGER.debug("Could not read from DB : " + handlers.cause());
              promise.fail(handlers.cause().getMessage());
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
                          OgcException ogcException =
                              new OgcException(204, "No Content", "Zero count");
                          LOGGER.debug(ogcException.getJson().toString());
                          promise.fail(ogcException.getJson().toString());
                          return;
                        }
                        collectionDetailsCall(jsonArray)
                            .onSuccess(
                                resultHandler -> {
                                  JsonObject resultJson =
                                      new JsonObject()
                                          .put("type", "urn:dx:dm:Success")
                                          .put("title", "Success")
                                          .put("results", resultHandler);
                                  promise.complete(resultJson);
                                });
                      } else {
                        LOGGER.debug("Could not read from DB : " + handlers.cause());
                        promise.fail(handlers.cause().getMessage());
                      }
                    });
              })
          .onFailure(
              fail -> {
                LOGGER.debug(fail.getMessage());
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
              promise.fail(failure.getMessage());
            });

    return promise.future();
  }

  public Future<JsonObject> executeQuery(final String query) {
    Promise<JsonObject> promise = Promise.promise();
    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());
    meteringpgClient
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
              promise.fail(failureHandler.getMessage());
            });
    return promise.future();
  }
}
