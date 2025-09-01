package ogc.rs.apiserver.authorization;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class to check resource access by making a POST request to a control panel has_access endpoint.
 */
public class CheckResourceAccess {
  private static final Logger LOGGER = LogManager.getLogger(CheckResourceAccess.class);
  private final int port;
  private final String controlPanelHost;
  private final String controlPanelSearchPath;
  private final WebClient webClient;
  private WebClientOptions webClientOptions;

  /**
   * Constructs the access checker with endpoint details.
   *
   * @param vertx                  Vertx instance
   * @param port                   control panel port
   * @param controlPanelHost       control panel host
   * @param controlPanelSearchPath control panel path
   */
  public CheckResourceAccess(Vertx vertx, int port, String controlPanelHost, String controlPanelSearchPath) {
    this.port = port;
    this.controlPanelHost = controlPanelHost;
    this.controlPanelSearchPath = controlPanelSearchPath;
    webClientOptions = new WebClientOptions();
    webClientOptions.setTrustAll(false).setVerifyHost(true).setSsl(true);
    webClient = WebClient.create(vertx, webClientOptions);
  }

  /**
   * Checks if the given itemId is accessible using the provided token.
   *
   * @param itemId resource UUID to check
   * @param token  Bearer token for authorization
   * @return Future with true if resource access is granted, false otherwise
   */
  public Future<Boolean> checkAccess(String itemId, String token) {
    return
        webClient
            .post(port, controlPanelHost, controlPanelSearchPath)
            .putHeader("Authorization", "Bearer " + token)
            .sendJsonObject(new JsonObject().put("itemId", itemId))
            .compose(response -> {
              LOGGER.info("check access response : {}",
                  response.body().toJsonObject().getString("detail"));
              if (response.statusCode() == 200) {
                return Future.succeededFuture(true);
              } else {
                return Future.succeededFuture(false);
              }
            });
  }


}
