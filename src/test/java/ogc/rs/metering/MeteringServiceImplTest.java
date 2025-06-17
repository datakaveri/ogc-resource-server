package ogc.rs.metering;

import static ogc.rs.common.Constants.*;
import static ogc.rs.common.Constants.ROLE;
import static ogc.rs.common.Constants.USER_ID;
import static ogc.rs.metering.util.MeteringConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import io.vertx.pgclient.PgPool;
import ogc.rs.catalogue.CatalogueService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class MeteringServiceImplTest {
  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImplTest.class);
  static PgPool databaseService;
  static PgPool ogcMeteringService;
  static JsonObject config =
      new JsonObject()
          .put("id", "ogc.rs.metering.MeteringVerticle")
          .put("verticleInstances", 1)
          .put("catServerHost", "api.cat-test.iudx.io")
          .put("catServerPort", 443)
          .put(DATABASE_TABLE_NAME, "auditing_ogc");
  static JsonObject requestJson = new JsonObject().put(USER_ID, "123-1243-56546-13424");
  private static MeteringServiceImpl meteringService;
  private static Vertx vertxObj;
  private static DataBrokerService databroker;
  @Mock HttpResponse<JsonObject> httpResponse;
  @Mock AsyncResult<HttpResponse<JsonObject>> httpResponseAsyncResult;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    vertxObj = vertx;
    vertxTestContext.completeNow();
  }

  private JsonObject readConsumerRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    jsonObject.put(RESOURCE_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    jsonObject.put(START_TIME, "2022-05-29T05:30:00+05:30");
    jsonObject.put(END_TIME, "2022-06-04T02:00:00+05:30");
    jsonObject.put(TIME_RELATION, DURING);
    jsonObject.put(API, "/ngsi-ld/v1/subscription");
    jsonObject.put(ENDPOINT, "/ngsi-ld/v1/consumer/audit");
    jsonObject.put(LIMITPARAM, 10);
    jsonObject.put(OFFSETPARAM, 0);

    return jsonObject;
  }

  private JsonObject readProviderRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    jsonObject.put(RESOURCE_ID, "a6615d6b-97e7-4964-a226-5daf0214fa5f");
    jsonObject.put(START_TIME, "2022-05-29T05:30:00+05:30");
    jsonObject.put(END_TIME, "2022-06-04T02:00:00+05:30");
    jsonObject.put(TIME_RELATION, DURING);
    jsonObject.put(API, "/ngsi-ld/v1/subscription");
    jsonObject.put(PROVIDER_ID, "a6615d6b-97e7-4964-a226-5daf0214fa5f");
    jsonObject.put(CONSUMER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    jsonObject.put(IID, "a6615d6b-97e7-4964-a226-5daf0214fa5f");
    jsonObject.put(ENDPOINT, "/ngsi-ld/v1/provider/audit");
    jsonObject.put(ROLE, "provider");
    return jsonObject;
  }

  @Test
  @DisplayName("Testing Write Query Successful")
  void writeDataSuccessful(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = zst.toInstant().toEpochMilli();
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
    request.put(EPOCH_TIME, time);
    request.put(ISO_TIME, isoTime);
    request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    request.put(API, "/ngsi-ld/v1/subscription");
    request.put(RESPONSE_SIZE, 12);
    JsonObject json = mock(JsonObject.class);
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    JsonArray jsonArray = mock(JsonArray.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    when(databroker.publishMessage(anyString(), anyString(), any()))
        .thenReturn(Future.succeededFuture());

    meteringService
        .insertMeteringValuesInRmq(request)
        .onSuccess(
            successHandler -> {
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
    verify(databroker, times(1)).publishMessage(anyString(), anyString(), any());
  }

  @Test
  @DisplayName("Testing Write Query Failure")
  void writeDataFailure(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = zst.toInstant().toEpochMilli();
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
    request.put(EPOCH_TIME, time);
    request.put(ISO_TIME, isoTime);
    request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    request.put(API, "/ngsi-ld/v1/subscription");
    request.put(RESPONSE_SIZE, 12);

    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    when(databroker.publishMessage(anyString(), anyString(), any()))
        .thenReturn(Future.failedFuture("failed"));

    meteringService
        .insertMeteringValuesInRmq(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed");
              }
            });
  }

  @Test
  @DisplayName("Testing read query with given Time Interval")
  void readFromValidTimeInterval2(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject json = mock(JsonObject.class);
    JsonArray jsonArray = mock(JsonArray.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    JsonObject request = readConsumerRequest();
    List<JsonObject> jsonObjectList = new ArrayList<>();

    JsonObject jsonObject1 = new JsonObject().put("count",0);

    jsonObjectList.add(jsonObject1);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .executeReadQuery(request)
        .onSuccess(
            successHandler -> {
              assertNotNull(successHandler);
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Testing read query with given Time Interval - provider")
  void readFromValidTimeInterval3(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    JsonObject request = readProviderRequest().put("limit",100).put("offset",10);
    List<JsonObject> jsonObjectList = new ArrayList<>();

    JsonObject jsonObject1 = new JsonObject().put("count",10);

    jsonObjectList.add(jsonObject1);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .executeReadQuery(request)
        .onSuccess(
            successHandler -> {
              assertEquals(
                  jsonObject1.toString(), successHandler.getJsonArray("result").getString(0));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Testing read query with given Time Interval - provider")
  void readFromValidTimeInterval4(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);

    JsonObject request = readProviderRequest().put("limit",100).put("offset",10);
    request.remove(API);
    request.remove(CONSUMER_ID);
    request.remove(RESOURCE_ID);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    List<JsonObject> jsonObjectList = new ArrayList<>();

    JsonObject jsonObject1 = new JsonObject().put("count",10);

    jsonObjectList.add(jsonObject1);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .executeReadQuery(request)
        .onSuccess(
            successHandler -> {
              assertEquals(
                  jsonObject1.toString(), successHandler.getJsonArray("result").getString(0));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Testing read query with given Time Interval")
  void readFromValidTimeInterval5(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);

    JsonObject request = readConsumerRequest();
    request.remove(API);
    request.remove(RESOURCE_ID);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    List<JsonObject> jsonObjectList = new ArrayList<>();

    JsonObject jsonObject1 = new JsonObject().put("count",0);

    jsonObjectList.add(jsonObject1);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .executeReadQuery(request)
        .onSuccess(
            successHandler -> {
              assertNotNull(successHandler);
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Testing read query with given Time Interval -consumer")
  void readFromValidTimeInterval6(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject request = readConsumerRequest().put("limit",100).put("offset",10);
    request.remove(API);
    request.remove(RESOURCE_ID);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    List<JsonObject> jsonObjectList = new ArrayList<>();

    JsonObject jsonObject1 = new JsonObject().put("count",10);

    jsonObjectList.add(jsonObject1);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .executeReadQuery(request)
        .onSuccess(
            successHandler -> {
              assertEquals(
                  jsonObject1.toString(), successHandler.getJsonArray("result").getString(0));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Testing read query with invalid time interval")
  void readFromInvalidTimeInterval(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(START_TIME, "2021-11-01T05:30:00+05:30");
    request.put(END_TIME, "2021-11-01T02:00:00+05:30");
    meteringService
        .executeReadQuery(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(
                    handler.cause().getMessage(),
                    "Error : Difference between dates cannot be less than 1 Minute.");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing read query with invalid time interval")
  void readFromInvalidTimeInterval2(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(START_TIME, "2021-11-01T05:30:00+05:30");
    request.put(END_TIME, "2021-1-1T02:00:00+05:30");
    meteringService
        .executeReadQuery(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Error : invalid date-time");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing read query with blank user id")
  void readFromBlankUserId(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(USER_ID, "");
    meteringService
        .executeReadQuery(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Error : User Id not found.");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing read query with given Time Interval - provider")
  void countFromValidTimeInterval(VertxTestContext vertxTestContext) {
    JsonObject responseJson = new JsonObject().put("totalHits", "10");
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);

    JsonObject request = readProviderRequest();
    request.put("options", "count");

    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    List<JsonObject> jsonObjectList = new ArrayList<>();

    JsonObject jsonObject1 = new JsonObject().put("count",10);

    jsonObjectList.add(jsonObject1);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .executeReadQuery(request)
        .onSuccess(
            successHandler -> {
              assertEquals(
                  responseJson.getString("totalHits"), "10");
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Admin for overview api")
  public void testOverallMethodAdmin(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject expected = new JsonObject().put(SUCCESS, "Success");

    databroker = mock(DataBrokerService.class);
    JsonObject request = readProviderRequest();
    request.put("role", "admin");
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    List<JsonObject> jsonObjectList = new ArrayList<>();
    JsonObject jsonObject1 = new JsonObject().put(SUCCESS,"Success");

    jsonObjectList.add(jsonObject1);
    JsonArray expectedJsonArray = new JsonArray().add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));
    meteringService
        .monthlyOverview(request)
        .onSuccess(
            successHandler -> {
              assertEquals(expectedJsonArray, successHandler.getJsonArray("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Admin for overview api")
  public void testOverallMethodAdmin2(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject expected = new JsonObject().put(SUCCESS, "Success");

    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "admin");
    request.put(STARTT, "2022-05-29T05:30:00+05:30");
    request.put(ENDT, "2022-06-29T05:30:00+05:30");
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    List<JsonObject> jsonObjectList = new ArrayList<>();
    JsonObject jsonObject1 = new JsonObject().put(SUCCESS,"Success");
    jsonObjectList.add(jsonObject1);
    JsonArray expectedJsonArray = new JsonArray().add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));
    meteringService
        .monthlyOverview(request)
        .onSuccess(
            successHandler -> {
              assertEquals(expectedJsonArray, successHandler.getJsonArray("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Provider for overview api")
  public void testOverallMethodProvider(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    databroker = mock(DataBrokerService.class);
    JsonObject request = readProviderRequest();
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "2021-12-01T05:30:00+05:30");
    request.put("provider", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonArray jsonArray1 =
        new JsonArray().add(new JsonObject().put("resourceid", "abc").put("count", 10));
    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .monthlyOverview(request)
        .onSuccess(
            successHandler -> {
              System.out.println(successHandler);
              assertEquals(expected.toString(), successHandler.getJsonArray("result").getString(0));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Provider for overview api")
  public void testOverallMethodProvider2(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "provider");
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "2021-12-01T05:30:00+05:30");
    request.put("provider", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonArray jsonArray1 =
        new JsonArray().add(new JsonObject().put("resourceid", "abc").put("count", 10));
    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .monthlyOverview(request)
        .onSuccess(
            successHandler -> {
              assertEquals(expected.toString(), successHandler.getJsonArray("result").getString(0));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Delegate for overview api")
  public void testOverallMethodDelegate(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject expected = new JsonObject().put(SUCCESS, "Success");
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request =
        readProviderRequest().put("provider", "8b95ab80-2aaf-4636-a65e-7f2563d0d371");
    JsonArray jsonArray1 = new JsonArray().add(expected);
    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .monthlyOverview(request)
        .onSuccess(
            successHandler -> {
              assertEquals(expected.toString(), successHandler.getJsonArray("result").getString(0));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Delegate for overview api")
  public void testOverallMethodDelegate2(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject expected = new JsonObject().put(SUCCESS, "Success");
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);

    JsonObject request =
        readProviderRequest().put("provider", "8b95ab80-2aaf-4636-a65e-7f2563d0d371");
    request.put("role", "delegate");
    request.put(STARTT, "2022-05-29T05:30:00+05:30");
    request.put(ENDT, "2022-06-29T05:30:00+05:30");
    JsonArray jsonArray1 = new JsonArray().add(expected);
    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .monthlyOverview(request)
        .onSuccess(
            successHandler -> {
              assertEquals(expected.toString(), successHandler.getJsonArray("result").getString(0));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("consumer for overview api")
  public void testOverallMethodConsumer(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject expected = new JsonObject().put(SUCCESS, "Success");

    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "consumer");
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .monthlyOverview(request)
        .onSuccess(
            successHandler -> {
              assertEquals(successHandler.getJsonArray("result").getJsonObject(0), expected);
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("consumer for overview api")
  public void testOverallMethodConsumere2(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    JsonObject expected = new JsonObject().put(SUCCESS, "Success");

    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "consumer");
    request.put(STARTT, "2022-05-29T05:30:00+05:30");
    request.put(ENDT, "2022-06-29T05:30:00+05:30");

    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .monthlyOverview(request)
        .onSuccess(
            successHandler -> {
              assertEquals(successHandler.getJsonArray("result").getJsonObject(0), expected);
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Testing monthly api query with invalid time interval")
  void readFromInvalidTimeIntervalMonthly(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "2021-11-01T02:00:00+05:30");
    request.put(ROLE, "admin");
    meteringService
        .monthlyOverview(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(
                    handler.cause().getMessage(),
                    "Bad request");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing monthly api query with invalid time interval")
  void readFromInvalidTimeIntervalMonthly2(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "");
    request.put(ROLE, "admin");
    meteringService
        .monthlyOverview(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Bad request");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing summary api query with invalid time interval")
  void readFromInvalidTimeIntervalSummary(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "2021-11-01T02:00:00+05:30");
    request.put(ROLE, "admin");
    meteringService
        .summaryOverview(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(
                    handler.cause().getMessage(),
                    "Bad request");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing summary api query with invalid time interval")
  void readFromInvalidTimeIntervalSummary2(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "");
    request.put(ROLE, "admin");
    meteringService
        .summaryOverview(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Bad request");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing summary api query with invalid time interval")
  void readFromInvalidTimeIntervalSummary3(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(STARTT, "");
    request.put(ENDT, "2021-11-01T05:30:00+05:30");
    request.put(ROLE, "admin");
    meteringService
        .summaryOverview(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Bad request");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("consumer for summary api")
  public void testOverallMethodConsumerSummary(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "consumer");
    JsonArray jsonArray1 =
        new JsonArray().add(new JsonObject().put("resourceid", "abc").put("count", 10));

    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());
    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .summaryOverview(request)
        .onSuccess(
            successHandler -> {
              assertTrue(successHandler.containsKey("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("consumer for summary api with time")
  public void testOverallMethodConsumerSummaryWithTime(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "consumer");
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "2021-12-01T05:30:00+05:30");
    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .summaryOverview(request)
        .onSuccess(
            successHandler -> {System.out.println(successHandler);
              assertTrue(successHandler.containsKey("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Provider for summary api with time")
  public void testOverallMethodProviderSummaryWithTime(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "2021-12-01T05:30:00+05:30");
    request.put("provider", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
     JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .summaryOverview(request)
        .onSuccess(
            successHandler -> {
              assertTrue(successHandler.containsKey("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Provider for summary api")
  public void testOverallMethodProviderSummary(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("provider", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonArray jsonArray1 =
        new JsonArray().add(new JsonObject().put("resourceid", "abc").put("count", 10));
    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .summaryOverview(request)
        .onSuccess(
            successHandler -> {
              assertTrue(successHandler.containsKey("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Admin for summary api")
  public void testOverallMethodAdminSummary(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "admin");
    JsonArray jsonArray1 =
        new JsonArray().add(new JsonObject().put("resourceid", "abc").put("count", 10));

    JsonObject jsonObject =
        new JsonObject()
            .put("type", "urn:dx:rs:success")
            .put("title", "Success")
            .put("result", jsonArray1);

    JsonObject outputFormat =
        new JsonObject().put("resourceid", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonArray outputArray = new JsonArray().add(outputFormat);

    MeteringServiceImpl spyMeteringService = Mockito.spy(meteringService);
    doAnswer(Answer -> Future.succeededFuture(outputArray))
        .when(spyMeteringService)
        .collectionDetailsCall(any());

    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    spyMeteringService
        .summaryOverview(request)
        .onSuccess(
            successHandler -> {
              assertTrue(successHandler.containsKey("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Admin for summary api with time")
  public void testOverallMethodAdminSummary2(VertxTestContext vertxTestContext) {

    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "admin");
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "2021-12-01T05:30:00+05:30");

    JsonArray jsonArray1 =
        new JsonArray().add(new JsonObject().put("resourceid", "abc").put("count", 10));

    JsonObject jsonObject =
        new JsonObject()
            .put("type", "urn:dx:rs:success")
            .put("title", "Success")
            .put("result", jsonArray1);

    JsonObject outputFormat =
        new JsonObject().put("resourceid", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonArray outputArray = new JsonArray().add(outputFormat);

    MeteringServiceImpl spyMeteringService = Mockito.spy(meteringService);
    doAnswer(Answer -> Future.succeededFuture(outputArray))
        .when(spyMeteringService)
        .collectionDetailsCall(any());

    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));
    spyMeteringService
        .summaryOverview(request)
        .onSuccess(
            successHandler -> {
              assertTrue(successHandler.containsKey("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Delegate for summary api")
  public void testOverallMethodDelegateSummary(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "delegate");
    request.put("provider", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonArray jsonArray1 =
        new JsonArray().add(new JsonObject().put("resourceid", "abc").put("count", 10));
    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .summaryOverview(request)
        .onSuccess(
            successHandler -> {
              assertTrue(successHandler.containsKey("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Delegate for summary api with time")
  public void testOverallMethodDelegateSummaryWithTime(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "delegate");
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ENDT, "2021-12-01T05:30:00+05:30");
    request.put("provider", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonArray jsonArray1 =
        new JsonArray().add(new JsonObject().put("resourceid", "abc").put("count", 10));
    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 1)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 10);
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .summaryOverview(request)
        .onSuccess(
            successHandler -> {
              assertTrue(successHandler.containsKey("result"));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }

  @Test
  @DisplayName("Testing summary api query with invalid time interval")
  void readFromInvalidTimeIntervalSummary4(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(ENDT, "2021-11-01T05:30:00+05:30");
    request.put(ROLE, "admin");
    meteringService
        .summaryOverview(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Bad request");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing monthly api query with invalid time interval")
  void readFromInvalidTimeIntervalMonthly4(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(ENDT, "2021-11-01T05:30:00+05:30");
    request.put(ROLE, "admin");
    meteringService
        .monthlyOverview(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Bad request");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing summary api query with invalid time interval")
  void readFromInvalidTimeIntervalSummary5(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ROLE, "admin");
    meteringService
        .summaryOverview(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Bad request");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("Testing monthly api query with invalid time interval")
  void readFromInvalidTimeIntervalMonthly5(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    request.put(STARTT, "2021-11-01T05:30:00+05:30");
    request.put(ROLE, "admin");
    meteringService
        .monthlyOverview(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Bad request");
                testContext.completeNow();
              } else {
                testContext.failNow("failed");
              }
            });
  }

  @Test
  @DisplayName("consumer for summary api -- > Catalogue fail")
  public void testOverallMethodConsumerSummary3(VertxTestContext vertxTestContext) {
    databaseService = mock(PgPool.class);
    ogcMeteringService = mock(PgPool.class);
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(vertxObj, databaseService, ogcMeteringService, config, databroker);
    JsonObject request = readProviderRequest();
    request.put("role", "consumer");

    JsonObject providerJson =
        new JsonObject()
            .put("totalHits", 0)
            .put("results", new JsonArray().add(request))
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    HttpRequest<Buffer> httpRequest = mock(HttpRequest.class);
    CatalogueService.catWebClient = mock(WebClient.class);
    lenient()
        .when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);

    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);

    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);

    when(httpResponse.bodyAsJsonObject()).thenReturn(providerJson);
    doAnswer(
            (Answer<Void>)
                invocation -> {
                  ((Handler<AsyncResult<HttpResponse<JsonObject>>>) invocation.getArgument(0))
                      .handle(httpResponseAsyncResult);
                  return null;
                })
        .when(httpRequest)
        .send(any());

    JsonObject expected = new JsonObject().put("resourceid", "abc").put("count", 0);
    List<JsonObject> jsonObjectList = new ArrayList<>();
    jsonObjectList.add(expected);
    when(databaseService.withConnection(any())).thenReturn(Future.succeededFuture(jsonObjectList));

    meteringService
        .summaryOverview(request)
        .onSuccess(
            successHandler -> {
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
  }
}
