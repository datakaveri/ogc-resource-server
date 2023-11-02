package ogc.rs.metering;

import static ogc.rs.apiserver.util.Constants.*;
import static ogc.rs.metering.util.MeteringConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class MeteringServiceTest {
    private static final Logger LOGGER = LogManager.getLogger(MeteringServiceTest.class);
    static DatabaseService databaseService;
    static JsonObject config = new JsonObject().put("id","ogc.rs.metering.MeteringVerticle")
            .put("verticleInstances",1).put("catServerHost","api.cat-test.iudx.io")
            .put("catServerPort",443);
    static JsonObject requestJson =  new JsonObject().put(USER_ID,"123-1243-56546-13424");
    private static MeteringServiceImpl meteringService;
    private static Vertx vertxObj;
    private static DataBrokerService databroker;

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
        jsonObject.put(LIMITPARAM,"10");
        jsonObject.put(OFFSETPARAM,"0");

        return jsonObject;
    }

    private JsonObject readProviderRequest() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(RESOURCE_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(START_TIME, "2022-05-29T05:30:00+05:30");
        jsonObject.put(END_TIME, "2022-06-04T02:00:00+05:30");
        jsonObject.put(TIME_RELATION, DURING);
        jsonObject.put(API, "/ngsi-ld/v1/subscription");
        jsonObject.put(PROVIDER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(CONSUMER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6");
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
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        JsonArray jsonArray = mock(JsonArray.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getString(anyString())).thenReturn("owner_id");
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray)));

        when(databroker.publishMessage(anyString(),anyString(),any())).thenReturn(Future.succeededFuture());

        meteringService.insertMeteringValuesInRmq(request).onSuccess(successHandler->{
           vertxTestContext.completeNow();
        }).onFailure(failure->{
            vertxTestContext.failNow(failure.getMessage());
        });
        verify(databroker, times(1)).publishMessage(anyString(),anyString(),any());
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

        JsonObject json = mock(JsonObject.class);
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        JsonArray jsonArray = mock(JsonArray.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getString(anyString())).thenReturn("owner_id");

        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray)));
        when(databroker.publishMessage(anyString(),anyString(),any())).thenReturn(Future.failedFuture("failed"));

        meteringService.insertMeteringValuesInRmq(
                request).onComplete(
                handler -> {
                    if (handler.failed()) {
                        vertxTestContext.completeNow();
                    } else {
                        vertxTestContext.failNow("Failed");
                    }
                });
    }

    @Test
    @DisplayName("Testing read query with given Time Interval -consumer")
    void readFromValidTimeInterval(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        databaseService = mock(DatabaseService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        JsonArray jsonArray1 = new JsonArray().add(responseJson);

        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);

        JsonObject request = readConsumerRequest();

        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(10);
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray1)));

    meteringService
        .executeReadQuery(request)
        .onSuccess(
            successHandler -> {
              assertEquals(responseJson.toString(),successHandler.getJsonArray("result").getString(0));
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
    }

    @Test
    @DisplayName("Testing read query with given Time Interval")
    void readFromValidTimeInterval2(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        databaseService = mock(DatabaseService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        JsonArray jsonArray1 = new JsonArray().add(responseJson);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);

        JsonObject request = readConsumerRequest();

        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(0);
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture());

    meteringService
        .executeReadQuery(request)
        .onSuccess(
            successHandler -> {
              assertNull(successHandler);
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
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        databaseService = mock(DatabaseService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        JsonArray jsonArray1 = new JsonArray().add(responseJson);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);

        JsonObject request = readProviderRequest();

        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(10);
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray1)));

        meteringService
                .executeReadQuery(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(responseJson.toString(),successHandler.getJsonArray("result").getString(0));
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
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        databaseService = mock(DatabaseService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        JsonArray jsonArray1 = new JsonArray().add(responseJson);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);

        JsonObject request = readProviderRequest();
        request.remove(API);
        request.remove(CONSUMER_ID);
        request.remove(RESOURCE_ID);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(10);
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray1)));

        meteringService
                .executeReadQuery(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(responseJson.toString(),successHandler.getJsonArray("result").getString(0));
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
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        databaseService = mock(DatabaseService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        JsonArray jsonArray1 = new JsonArray().add(responseJson);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);

        JsonObject request = readConsumerRequest();
        request.remove(API);
        request.remove(RESOURCE_ID);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(0);
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture());

        meteringService
                .executeReadQuery(request)
                .onSuccess(
                        successHandler -> {
                            assertNull(successHandler);
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
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        databaseService = mock(DatabaseService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        JsonArray jsonArray1 = new JsonArray().add(responseJson);

        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);

        JsonObject request = readConsumerRequest();
        request.remove(API);
        request.remove(RESOURCE_ID);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(10);
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray1)));

        meteringService
                .executeReadQuery(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(responseJson.toString(),successHandler.getJsonArray("result").getString(0));
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
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(START_TIME, "2021-11-01T05:30:00+05:30");
        request.put(END_TIME, "2021-11-01T02:00:00+05:30");
        meteringService.executeReadQuery(
                request).onComplete(handler->{
                    if (handler.failed()){
                        assertEquals(handler.cause().getMessage(),"Error : Difference between dates cannot be less than 1 Minute.");
                        testContext.completeNow();
                    }
                    else {
                        testContext.failNow("failed");
                    }
        });
    }

    @Test
    @DisplayName("Testing read query with invalid time interval")
    void readFromInvalidTimeInterval2(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(START_TIME, "2021-11-01T05:30:00+05:30");
        request.put(END_TIME, "2021-1-1T02:00:00+05:30");
        meteringService.executeReadQuery(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Error : invalid date-time");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }
    @Test
    @DisplayName("Testing read query with invalid time")
    void readFromInvalidTime(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.remove(START_TIME);
        meteringService.executeReadQuery(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Error : Time interval not found.");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }

    @Test
    @DisplayName("Testing read query with invalid time relation")
    void readFromInvalidTimeRelationInterval(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.remove(TIME_RELATION);
        meteringService.executeReadQuery(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Error : Time relation not found.");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }

    @Test
    @DisplayName("Testing read query with blank user id")
    void readFromBlankUserId(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(USER_ID,"");
        meteringService.executeReadQuery(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Error : User Id not found.");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }

    @Test
    @DisplayName("Testing read query with given Time Interval - provider")
    void countFromValidTimeInterval(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put("totalHits", "10");
        databaseService = mock(DatabaseService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        JsonArray jsonArray1 = new JsonArray().add(responseJson);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);

        JsonObject request = readProviderRequest();
        request.put("options","count");

        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(10);
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray)));

        meteringService
                .executeReadQuery(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(responseJson.getString("totalHits"),successHandler.getString("totalHits"));
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
        databaseService = mock(DatabaseService.class);
        JsonObject expected = new JsonObject().put(SUCCESS, "Success");

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "admin");
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(expected));
        meteringService
            .monthlyOverview(request)
            .onSuccess(
                successHandler -> {
                    assertEquals(successHandler,expected);
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
        databaseService = mock(DatabaseService.class);
        JsonObject expected = new JsonObject().put(SUCCESS, "Success");

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "admin");
        request.put(STARTT,"2022-05-29T05:30:00+05:30");
        request.put(ENDT,"2022-06-29T05:30:00+05:30");
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(expected));
        meteringService
                .monthlyOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(successHandler,expected);
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
        databaseService = mock(DatabaseService.class);
        JsonObject expected = new JsonObject().put(SUCCESS, "Success");
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "provider");
        JsonArray jsonArray1 = new JsonArray().add(expected);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getString(anyString())).thenReturn("provider_id");
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray1)));
        meteringService
                .monthlyOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(expected.toString(),successHandler.getJsonArray("result").getString(0));
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
        databaseService = mock(DatabaseService.class);
        JsonObject expected = new JsonObject().put(SUCCESS, "Success");
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "provider");
        request.put(STARTT,"2022-05-29T05:30:00+05:30");
        request.put(ENDT,"2022-06-29T05:30:00+05:30");
        JsonArray jsonArray1 = new JsonArray().add(expected);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getString(anyString())).thenReturn("provider_id");
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray1)));
        meteringService
                .monthlyOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(expected.toString(),successHandler.getJsonArray("result").getString(0));
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
        databaseService = mock(DatabaseService.class);
        JsonObject expected = new JsonObject().put(SUCCESS, "Success");
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "delegate");
        JsonArray jsonArray1 = new JsonArray().add(expected);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getString(anyString())).thenReturn("provider_id");
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray1)));
        meteringService
                .monthlyOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(expected.toString(),successHandler.getJsonArray("result").getString(0));
                            vertxTestContext.completeNow();
                        })
                .onFailure(
                        failure -> {
                            vertxTestContext.failNow(failure.getMessage());
                        });
    }
    @Test
    @DisplayName("Deleagate for overview api")
    public void testOverallMethodDelegate2(VertxTestContext vertxTestContext) {
        databaseService = mock(DatabaseService.class);
        JsonObject expected = new JsonObject().put(SUCCESS, "Success");
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "delegate");
        request.put(STARTT,"2022-05-29T05:30:00+05:30");
        request.put(ENDT,"2022-06-29T05:30:00+05:30");
        JsonArray jsonArray1 = new JsonArray().add(expected);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getString(anyString())).thenReturn("provider_id");
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray))).thenReturn(Future.succeededFuture(new JsonObject().put("result",jsonArray1)));
        meteringService
                .monthlyOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(expected.toString(),successHandler.getJsonArray("result").getString(0));
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
        databaseService = mock(DatabaseService.class);
        JsonObject expected = new JsonObject().put(SUCCESS, "Success");

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "consumer");
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(expected));
        meteringService
                .monthlyOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(successHandler,expected);
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
        databaseService = mock(DatabaseService.class);
        JsonObject expected = new JsonObject().put(SUCCESS, "Success");

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "consumer");
        request.put(STARTT,"2022-05-29T05:30:00+05:30");
        request.put(ENDT,"2022-06-29T05:30:00+05:30");
        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(expected));
        meteringService
                .monthlyOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertEquals(successHandler,expected);
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
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(STARTT, "2021-11-01T05:30:00+05:30");
        request.put(ENDT, "2021-11-01T02:00:00+05:30");
        request.put(ROLE,"admin");
        meteringService.monthlyOverview(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Difference between dates cannot be less than 1 Minute.");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }

    @Test
    @DisplayName("Testing monthly api query with invalid time interval")
    void readFromInvalidTimeIntervalMonthly2(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(STARTT, "2021-11-01T05:30:00+05:30");
        request.put(ENDT, "");
        request.put(ROLE,"admin");
        meteringService.monthlyOverview(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"invalid date-time");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }

    @Test
    @DisplayName("Testing summary api query with invalid time interval")
    void readFromInvalidTimeIntervalSummary(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(STARTT, "2021-11-01T05:30:00+05:30");
        request.put(ENDT, "2021-11-01T02:00:00+05:30");
        request.put(ROLE,"admin");
        meteringService.summaryOverview(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Difference between dates cannot be less than 1 Minute.");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }
    @Test
    @DisplayName("Testing summary api query with invalid time interval")
    void readFromInvalidTimeIntervalSummary2(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(STARTT, "2021-11-01T05:30:00+05:30");
        request.put(ENDT, "");
        request.put(ROLE,"admin");
        meteringService.summaryOverview(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"invalid date-time");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }
    @Test
    @DisplayName("Testing summary api query with invalid time interval")
    void readFromInvalidTimeIntervalSummary3(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(STARTT, "");
        request.put(ENDT, "2021-11-01T05:30:00+05:30");
        request.put(ROLE,"admin");
        meteringService.summaryOverview(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"invalid date-time");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }

    @Test
    @DisplayName("consumer for summary api")
    public void testOverallMethodConsumerSummary(VertxTestContext vertxTestContext) {
        databaseService = mock(DatabaseService.class);

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "consumer");
        JsonArray jsonArray1 = new JsonArray().add(new JsonObject().put("resourceid","abc").put("count",10));
        JsonObject jsonObject =  new JsonObject().put("result",jsonArray1);
        JsonArray jsonArray2 =  new JsonArray().add(new JsonObject().put("id","abc").put("description","desc")
        .put("title","title").put("owner_id","ownerid"));
        JsonObject jsonObject2 = new JsonObject().put("result",jsonArray2);

        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(jsonObject)).thenReturn(Future.succeededFuture(jsonObject2));
        meteringService
                .summaryOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertTrue(successHandler.containsKey("results"));
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
        databaseService = mock(DatabaseService.class);

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "consumer");
        request.put(STARTT,"2021-11-01T05:30:00+05:30");
        request.put(ENDT,"2021-12-01T05:30:00+05:30");
        JsonArray jsonArray1 = new JsonArray().add(new JsonObject().put("resourceid","abc").put("count",10));
        JsonObject jsonObject =  new JsonObject().put("result",jsonArray1);
        JsonArray jsonArray2 =  new JsonArray().add(new JsonObject().put("id","abc").put("description","desc")
                .put("title","title").put("owner_id","ownerid"));
        JsonObject jsonObject2 = new JsonObject().put("result",jsonArray2);

        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(jsonObject)).thenReturn(Future.succeededFuture(jsonObject2));
        meteringService
                .summaryOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertTrue(successHandler.containsKey("results"));
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
        databaseService = mock(DatabaseService.class);

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put(STARTT,"2021-11-01T05:30:00+05:30");
        request.put(ENDT,"2021-12-01T05:30:00+05:30");
        JsonArray jsonArray1 = new JsonArray().add(new JsonObject().put("resourceid","abc").put("count",10));
        JsonObject jsonObject =  new JsonObject().put("result",jsonArray1);
        JsonArray jsonArray2 =  new JsonArray().add(new JsonObject().put("id","abc").put("description","desc")
                .put("title","title").put("owner_id","ownerid"));
        JsonObject jsonObject2 = new JsonObject().put("result",jsonArray2);

        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(jsonObject2)).thenReturn(Future.succeededFuture(jsonObject)).thenReturn(Future.succeededFuture(jsonObject2));
        meteringService
                .summaryOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertTrue(successHandler.containsKey("results"));
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
        databaseService = mock(DatabaseService.class);

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        JsonArray jsonArray1 = new JsonArray().add(new JsonObject().put("resourceid","abc").put("count",10));
        JsonObject jsonObject =  new JsonObject().put("result",jsonArray1);
        JsonArray jsonArray2 =  new JsonArray().add(new JsonObject().put("id","abc").put("description","desc")
                .put("title","title").put("owner_id","ownerid"));
        JsonObject jsonObject2 = new JsonObject().put("result",jsonArray2);

        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(jsonObject2)).thenReturn(Future.succeededFuture(jsonObject)).thenReturn(Future.succeededFuture(jsonObject2));
        meteringService
                .summaryOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertTrue(successHandler.containsKey("results"));
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
        databaseService = mock(DatabaseService.class);

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "admin");
        JsonArray jsonArray1 = new JsonArray().add(new JsonObject().put("resourceid","abc").put("count",10));
        JsonObject jsonObject =  new JsonObject().put("result",jsonArray1);
        JsonArray jsonArray2 =  new JsonArray().add(new JsonObject().put("id","abc").put("description","desc")
                .put("title","title").put("owner_id","ownerid"));
        JsonObject jsonObject2 = new JsonObject().put("result",jsonArray2);

        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(jsonObject)).thenReturn(Future.succeededFuture(jsonObject2));
        meteringService
                .summaryOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertTrue(successHandler.containsKey("results"));
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
        databaseService = mock(DatabaseService.class);

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "admin");
        request.put(STARTT,"2021-11-01T05:30:00+05:30");
        request.put(ENDT,"2021-12-01T05:30:00+05:30");
        JsonArray jsonArray1 = new JsonArray().add(new JsonObject().put("resourceid","abc").put("count",10));
        JsonObject jsonObject =  new JsonObject().put("result",jsonArray1);
        JsonArray jsonArray2 =  new JsonArray().add(new JsonObject().put("id","abc").put("description","desc")
                .put("title","title").put("owner_id","ownerid"));
        JsonObject jsonObject2 = new JsonObject().put("result",jsonArray2);

        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(jsonObject)).thenReturn(Future.succeededFuture(jsonObject2));
        meteringService
                .summaryOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertTrue(successHandler.containsKey("results"));
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
        databaseService = mock(DatabaseService.class);

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "delegate");
        JsonArray jsonArray1 = new JsonArray().add(new JsonObject().put("resourceid","abc").put("count",10));
        JsonObject jsonObject =  new JsonObject().put("result",jsonArray1);
        JsonArray jsonArray2 =  new JsonArray().add(new JsonObject().put("id","abc").put("description","desc")
                .put("title","title").put("owner_id","ownerid"));
        JsonObject jsonObject2 = new JsonObject().put("result",jsonArray2);

        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(jsonObject2)).thenReturn(Future.succeededFuture(jsonObject)).thenReturn(Future.succeededFuture(jsonObject2));
        meteringService
                .summaryOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertTrue(successHandler.containsKey("results"));
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
        databaseService = mock(DatabaseService.class);

        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        JsonObject request = readProviderRequest();
        request.put("role", "delegate");

        request.put(STARTT,"2021-11-01T05:30:00+05:30");
        request.put(ENDT,"2021-12-01T05:30:00+05:30");
        JsonArray jsonArray1 = new JsonArray().add(new JsonObject().put("resourceid","abc").put("count",10));
        JsonObject jsonObject =  new JsonObject().put("result",jsonArray1);
        JsonArray jsonArray2 =  new JsonArray().add(new JsonObject().put("id","abc").put("description","desc")
                .put("title","title").put("owner_id","ownerid"));
        JsonObject jsonObject2 = new JsonObject().put("result",jsonArray2);

        when(databaseService.executeQuery(any())).thenReturn(Future.succeededFuture(jsonObject2)).thenReturn(Future.succeededFuture(jsonObject)).thenReturn(Future.succeededFuture(jsonObject2));
        meteringService
                .summaryOverview(request)
                .onSuccess(
                        successHandler -> {
                            assertTrue(successHandler.containsKey("results"));
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
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(ENDT, "2021-11-01T05:30:00+05:30");
        request.put(ROLE,"admin");
        meteringService.summaryOverview(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Bad Request");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }
    @Test
    @DisplayName("Testing monthly api query with invalid time interval")
    void readFromInvalidTimeIntervalMonthly4(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(ENDT, "2021-11-01T05:30:00+05:30");
        request.put(ROLE,"admin");
        meteringService.monthlyOverview(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Bad Request");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }

    @Test
    @DisplayName("Testing summary api query with invalid time interval")
    void readFromInvalidTimeIntervalSummary5(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(STARTT, "2021-11-01T05:30:00+05:30");
        request.put(ROLE,"admin");
        meteringService.summaryOverview(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Bad Request");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }
    @Test
    @DisplayName("Testing monthly api query with invalid time interval")
    void readFromInvalidTimeIntervalMonthly5(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        databaseService = mock(DatabaseService.class);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);
        request.put(STARTT, "2021-11-01T05:30:00+05:30");
        request.put(ROLE,"admin");
        meteringService.monthlyOverview(
                request).onComplete(handler->{
            if (handler.failed()){
                assertEquals(handler.cause().getMessage(),"Bad Request");
                testContext.completeNow();
            }
            else {
                testContext.failNow("failed");
            }
        });
    }

}
