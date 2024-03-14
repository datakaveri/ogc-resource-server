package ogc.rs.metering;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBrokerServiceImpl implements DataBrokerService {
  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
  RabbitMQClient rabbitWebclient;

  public DataBrokerServiceImpl(RabbitMQClient client) {
    this.rabbitWebclient = client;
  }

  @Override
  public Future<Void> publishMessage(String toExchange, String routingKey, JsonObject body) {
    Promise<Void> promise = Promise.promise();

    Future<Void> rabbitMqClientStartFuture;

    Buffer buffer = Buffer.buffer(body.toString());
    if (!rabbitWebclient.isConnected()) {
      rabbitMqClientStartFuture = rabbitWebclient.start();
    } else {
      rabbitMqClientStartFuture = Future.succeededFuture();
    }

    rabbitMqClientStartFuture
        .compose(
            rabbitStartupFuture -> rabbitWebclient.basicPublish(toExchange, routingKey, buffer))
        .onSuccess(
            successHandler -> {
              LOGGER.debug("Message Published in RMQ");
              promise.complete();
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler);
              promise.fail(failureHandler.toString());
            });

    return promise.future();
  }
}
