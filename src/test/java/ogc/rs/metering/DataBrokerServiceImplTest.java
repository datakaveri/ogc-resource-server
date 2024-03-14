package ogc.rs.metering;

import static ogc.rs.common.Constants.ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class DataBrokerServiceImplTest {
  @Mock RabbitMQClient rabbitMQClient;
  DataBrokerServiceImpl databroker;
  JsonObject request;

  @BeforeEach
  public void setup(VertxTestContext vertxTestContext) {
    databroker = new DataBrokerServiceImpl(rabbitMQClient);
    request = new JsonObject();
    request.put("Dummy key", "Dummy value");
    request.put(ID, "Dummy ID");
    request.put("status", "Dummy status");
    request.put("routingKey", "routingKeyValue");
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test publishMessage method : Success")
  public void test_publishMessage_success(VertxTestContext vertxTestContext) {
    when(rabbitMQClient.isConnected()).thenReturn(true);
    doAnswer(Answer -> Future.succeededFuture())
        .when(rabbitMQClient)
        .basicPublish(anyString(), anyString(), any(Buffer.class));
    databroker
        .publishMessage("Dummy string to Exchange", "Dummy routing Key", request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }
}
