package ogc.rs.databroker;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import ogc.rs.apiserver.util.Response;
import ogc.rs.apiserver.util.ResponseUrn;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBrokerServiceImpl implements DataBrokerService{
    private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
    private final RabbitMQClient client;

    public DataBrokerServiceImpl(RabbitMQClient webClient) {
        this.client = webClient;
    }

    @Override
    public Future<Void> publishMessage(String toExchange, String routingKey, JsonObject body) {
        Promise<Void> promise = Promise.promise();

        Future<Void> rabbitMqClientStartFuture;

        Buffer buffer = Buffer.buffer(body.toString());
        if (!client.isConnected()) {
            rabbitMqClientStartFuture = client.start();
        } else {
            rabbitMqClientStartFuture = Future.succeededFuture();
        }

    rabbitMqClientStartFuture
        .compose(rabbitStartupFuture -> client.basicPublish(toExchange, routingKey, buffer))
        .onSuccess(
            successHandler -> {
              LOGGER.debug("Message Published in RMQ");
              promise.complete();
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler);
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.QUEUE_ERROR_URN.getUrn())
                      .withStatus(HttpStatus.SC_BAD_REQUEST)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .build();
              promise.fail(response.toJson().toString());
            });

        return promise.future();
    }
}
