package ogc.rs.databroker.client;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitClient {
  private static final Logger LOGGER = LogManager.getLogger(RabbitClient.class);
  public static String publishEx;
  private final RabbitMQClient iudxInternalRabbitMqClient;
  private final Vertx vertx;

  public RabbitClient(
      Vertx vertx,
      RabbitMQClient iudxInternalRabbitMqClient,
      RabbitMQClient iudxRabbitMqClient) {
    this.vertx = vertx;
    this.iudxInternalRabbitMqClient = iudxInternalRabbitMqClient;

    iudxInternalRabbitMqClient
        .start()
        .onSuccess(
            iudxInternalRabbitClientStart -> {
              LOGGER.info("RMQ client started for Internal Vhost");
            })
        .onFailure(
            iudxInternalRabbitClientStart -> {
              LOGGER.fatal("RMQ client startup failed");
            });
    iudxRabbitMqClient
        .start()
        .onSuccess(
            iudxRabbitClientStart -> {
              LOGGER.info("RMQ client started for Prod Vhost");
            })
        .onFailure(
            iudxRabbitClientStart -> {
              LOGGER.fatal("RMQ client startup failed");
            });
  }

  public Future<Void> publishMessageInternal(
      JsonObject body, String exchangeName, String routingKey) {
    Buffer buffer = Buffer.buffer(body.toString());
    Promise<Void> promise = Promise.promise();
    Future<Void> rabbitMqClientIudxInternalStartFuture;
    if (!iudxInternalRabbitMqClient.isConnected()) {
      rabbitMqClientIudxInternalStartFuture = iudxInternalRabbitMqClient.start();
    } else {
      rabbitMqClientIudxInternalStartFuture = Future.succeededFuture();
    }
    rabbitMqClientIudxInternalStartFuture
        .compose(
            started -> {
              return iudxInternalRabbitMqClient.basicPublish(exchangeName, routingKey, buffer);
            })
        .onSuccess(
            publishSuccess -> {
              promise.complete();
            })
        .onFailure(
            publishFailure -> {
              LOGGER.error("publishMessage failure " + publishFailure);
                promise.fail(new OgcException(500,"Internal Server Error","Internal Server Error"));

            });
    return promise.future();
  }

}
