package ogc.rs.apiserver.authentication.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import ogc.rs.apiserver.authorization.AuthorizationHandler;
import ogc.rs.catalogue.CatalogueService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AclClient {
  private static final Logger LOGGER = LogManager.getLogger(AclClient.class);

  private final int port;
    private final String controlPanelHost;
    private final String controlPanelSearchPath;
    private final WebClient webClient;
    /**
     * Constructs the access checker with endpoint details.
     *
     * @param vertx                  Vertx instance
     * @param port                   control panel port
     * @param controlPanelHost       control panel host
     * @param controlPanelSearchPath control panel path
     */
  public AclClient(Vertx vertx, int port, String controlPanelHost, String controlPanelSearchPath) {
    this.port = port;
    this.controlPanelHost = controlPanelHost;
    this.controlPanelSearchPath = controlPanelSearchPath;
    WebClientOptions webClientOptions = new WebClientOptions();
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
    LOGGER.info("Checking access for itemId: {}", itemId);
    //TODO: Remove the hardcoded item id
    return
        webClient
            .post(port, controlPanelHost, controlPanelSearchPath)
            .putHeader("Authorization", token)
            .sendJsonObject(new JsonObject().put("itemId", "a7815cb3-fcf2-4616-961a-07913560db81"))
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
