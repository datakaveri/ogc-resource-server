package ogc.rs.metering;

import static ogc.rs.apiserver.util.Constants.ID;
import static ogc.rs.apiserver.util.Constants.PROVIDER_ID;
import static ogc.rs.metering.util.MeteringConstant.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.apiserver.util.Response;
import ogc.rs.database.DatabaseService;
import ogc.rs.metering.util.DateValidation;
import ogc.rs.metering.util.ParamsValidation;
import ogc.rs.metering.util.QueryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService {
  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ParamsValidation paramValidation = new ParamsValidation();
  private final DateValidation dateValidation = new DateValidation();
  DataBrokerService dataBrokerService;
  DatabaseService databaseService;
  JsonObject validationCheck = new JsonObject();
  String queryCount;
  long total;
  String queryPg;
  String queryOverview;
  String summaryOverview;
  JsonArray jsonArray;
  JsonArray resultJsonArray;
  int loopi;

  public MeteringServiceImpl(
      Vertx vertx,
      DatabaseService databaseService,
      JsonObject config,
      DataBrokerService dataBrokerService) {
    this.dataBrokerService = dataBrokerService;
    this.databaseService = databaseService;
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

    request.put(TABLE_NAME, OGC_DATABASE_TABLE_NAME);

    String count = request.getString("options");
    if (count == null) {
      countQueryForRead(request)
          .onSuccess(
              successRead -> {
                promise.complete(successRead);
              })
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
                readMethod(request)
                    .onSuccess(
                        successRead -> {
                          promise.complete(successRead);
                        });
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
    String limit;
    String offset;
    if (request.getString(LIMITPARAM) == null) {
      limit = "2000";
      request.put(LIMITPARAM, limit);
    } else {
      limit = request.getString(LIMITPARAM);
    }
    if (request.getString(OFFSETPARAM) == null) {
      offset = "0";
      request.put(OFFSETPARAM, offset);
    } else {
      offset = request.getString(OFFSETPARAM);
    }
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
    String id = request.getString(ID);
    StringBuilder queryProvider = new StringBuilder(COLLECTION_DETAILS_QUERY.replace("$1", id));
    LOGGER.info(queryProvider);
    Future<JsonObject> queryProviderResult =
        executeQueryDatabaseOperation(queryProvider.toString());
    queryProviderResult.onComplete(
        providerHandler -> {
          if (providerHandler.succeeded()) {
            var results = providerHandler.result().getJsonArray("result");
            var position = results.getJsonObject(0);
            String providerId = position.getString("owner_id");
            request.put(PROVIDER_ID, providerId);

            JsonObject writeMessage = queryBuilder.buildMessageForRmq(request);
            LOGGER.info("write message =  {}", writeMessage);
            // TODO: Change Exchange Name after discussion
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
                        Response resp =
                            objectMapper.readValue(failureHandler.getMessage(), Response.class);
                        LOGGER.debug("response from rmq " + resp);
                        promise.fail(resp.toString());
                      } catch (JsonProcessingException e) {
                        LOGGER.error("Failure message not in format [type,title,detail]");
                        promise.fail(e.getMessage());
                      }
                    });
          } else {
            LOGGER.debug(providerHandler.cause().getMessage());
            promise.fail(providerHandler.cause().getMessage());
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
      LOGGER.debug("query Overview =" + queryOverview);

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
      String resourceId = request.getString(IID);

      StringBuilder queryProvider =
          new StringBuilder(COLLECTION_DETAILS_QUERY.replace("$1", resourceId));
      LOGGER.info(queryProvider);
      Future<JsonObject> queryProviderResult =
          executeQueryDatabaseOperation(queryProvider.toString());
      queryProviderResult.onComplete(
          providerHandlers -> {
            if (providerHandlers.succeeded()) {
              var results = providerHandlers.result().getJsonArray("result");
              var position = results.getJsonObject(0);
              String providerId = position.getString("owner_id");
              request.put("providerid", providerId);

              queryOverview = queryBuilder.buildMonthlyOverview(request);
              LOGGER.debug("query Overview =" + queryOverview);

              Future<JsonObject> monthlyResult = executeQueryDatabaseOperation(queryOverview);
              monthlyResult.onComplete(
                  monthlyHandlers -> {
                    if (monthlyHandlers.succeeded()) {
                      LOGGER.debug("Successfully return result");
                      promise.complete(monthlyHandlers.result());
                    } else {
                      LOGGER.debug("Could not read from DB : " + monthlyHandlers.cause());
                      promise.fail(monthlyHandlers.cause().getMessage());
                    }
                  });
            } else {
              LOGGER.debug("Could not read from DB : " + providerHandlers.cause());
              promise.fail(providerHandlers.cause().getMessage());
            }
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
                LOGGER.info(ogcException.getJson().toString());
                promise.fail(ogcException.getJson().toString());
                return;
              }
              collectionDetailsCall(jsonArray)
                  .onSuccess(
                      resultHandler -> {
                        JsonObject resultJson = new JsonObject().put("results", resultHandler);
                        promise.complete(resultJson);
                      });
            } else {
              LOGGER.debug("Could not read from DB : " + handlers.cause());
              promise.fail(handlers.cause().getMessage());
            }
          });
    } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
      String resourceId = request.getString(IID);
      StringBuilder queryProvider =
          new StringBuilder(COLLECTION_DETAILS_QUERY.replace("$1", resourceId));
      LOGGER.info(queryProvider);
      Future<JsonObject> queryProviderResult =
          executeQueryDatabaseOperation(queryProvider.toString());
      queryProviderResult.onComplete(
          providerHandler -> {
            if (providerHandler.succeeded()) {
              var results = providerHandler.result().getJsonArray("result");
              var position = results.getJsonObject(0);
              String providerId = position.getString("owner_id");
              request.put("providerid", providerId);

              summaryOverview = queryBuilder.buildSummaryOverview(request);
              LOGGER.debug("summary query = {}", summaryOverview);

              Future<JsonObject> result = executeQueryDatabaseOperation(summaryOverview);
              result.onComplete(
                  handlers -> {
                    if (handlers.succeeded()) {
                      jsonArray = handlers.result().getJsonArray("result");
                      if (jsonArray.size() <= 0) {
                        OgcException ogcException =
                            new OgcException(204, "No Content", "Zero count");
                        LOGGER.info(ogcException.getJson().toString());
                        promise.fail(ogcException.getJson().toString());
                        return;
                      }
                      collectionDetailsCall(jsonArray)
                          .onSuccess(
                              resultHandler -> {
                                JsonObject resultJson =
                                    new JsonObject().put("results", resultHandler);
                                promise.complete(resultJson);
                              });
                    } else {
                      LOGGER.debug("Could not read from DB : " + handlers.cause());
                      promise.fail(handlers.cause().getMessage());
                    }
                  });

            } else {
              LOGGER.debug(providerHandler.cause().getMessage());
              promise.fail(providerHandler.cause().getMessage());
            }
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
      StringBuilder queryDetails =
          new StringBuilder(COLLECTION_DETAILS_QUERY.replace("$1", resourceId));
      LOGGER.info("queryDetails = {}", queryDetails);
      Future<JsonObject> queryResult = executeQueryDatabaseOperation2(queryDetails.toString());

      resourceCount.put(
          jsonArray.getJsonObject(loopi).getString("resourceid"),
          Integer.valueOf(jsonArray.getJsonObject(loopi).getString("count")));
      list.add(queryResult.recover(f -> Future.succeededFuture(null)));
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
                        .put("publisher", result.getString("title"))
                        .put("publisher_id", result.getString("owner_id"))
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
    databaseService
        .executeQuery(query)
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

  private Future<JsonObject> executeQueryDatabaseOperation2(String query) {
    Promise<JsonObject> promise = Promise.promise();
    databaseService
        .executeQuery(query)
        .onSuccess(
            handler -> {
              jsonArray = handler.getJsonArray("result");
              if (jsonArray.size() <= 0) {
                LOGGER.debug("fail due to zero result");
                promise.fail("Failed");
              } else {
                promise.complete(handler.getJsonArray("result").getJsonObject(0));
              }
            })
        .onFailure(
            failure -> {
              promise.fail(failure.getMessage());
            });

    return promise.future();
  }
}
