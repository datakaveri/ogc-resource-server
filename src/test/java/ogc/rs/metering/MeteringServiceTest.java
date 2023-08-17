package ogc.rs.metering;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import ogc.rs.common.VirtualHosts;
import ogc.rs.database.DatabaseService;
import ogc.rs.databroker.DataBrokerService;
import ogc.rs.databroker.DataBrokerServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static ogc.rs.apiserver.util.Constants.*;
import static ogc.rs.metering.util.MeteringConstant.USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class MeteringServiceTest {
    private static final Logger LOGGER = LogManager.getLogger(MeteringServiceTest.class);
    private static MeteringServiceImpl meteringService;
    private static Vertx vertxObj;
    static DatabaseService databaseService;
    private static DataBrokerService databroker;
    static JsonObject config = new JsonObject().put("id","ogc.rs.metering.MeteringVerticle")
            .put("verticleInstances",1).put("catServerHost","api.cat-test.iudx.io")
            .put("catServerPort",443);
    static JsonObject requestJson =  new JsonObject().put(USER_ID,"123-1243-56546-13424");
    @BeforeAll
    @DisplayName("Deploying Verticle")
    static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
        vertxObj = vertx;
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertx, databaseService,config,databroker);
        vertxTestContext.completeNow();
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
        request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        request.put(API, "/ngsi-ld/v1/subscription");
        request.put(RESPONSE_SIZE, 12);
        databroker = mock(DataBrokerService.class);
        meteringService = new MeteringServiceImpl(vertxObj, databaseService,config,databroker);

        when(databroker.publishMessage(anyString(),anyString(),any())).thenReturn(Future.succeededFuture());

        meteringService.insertMeteringValuesInRmq(request).onSuccess(successHandler->{
           vertxTestContext.completeNow();
        }).onFailure(failure->{
            vertxTestContext.failNow(failure.getMessage());
        });
        verify(databroker, times(1)).publishMessage(anyString(),anyString(),any());
    }

}
