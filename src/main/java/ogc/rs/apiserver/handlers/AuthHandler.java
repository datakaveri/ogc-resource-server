package ogc.rs.apiserver.handlers;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.authenticator.AuthenticationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.apiserver.util.Constants.HEADER_TOKEN;
import static ogc.rs.common.Constants.AUTH_SERVICE_ADDRESS;

public class AuthHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);
    static AuthenticationService authenticator;

  public static AuthHandler create(Vertx vertx) {
        authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
        return new AuthHandler();
    }
    @Override
    public void handle(RoutingContext context) {
      // private static Api api;

      HttpServerRequest request = context.request();
      String token;
      String id;
      token = request.headers().get(HEADER_TOKEN);
      id = context.pathParam("collectionId");
      // requestJson will be used by the metering service
      if (token == null || id == null) {
        LOGGER.error("Null values for either token or id!");
        context.put("isAuthorised",  false);
        context.put("response",
            new OgcException(401, "Not Authorised", "User is not Authorised. Please contact IUDX AAA Server.")
                .getJson().toString());
        context.put("statusCode", 401);
        context.next();
        return;
      }
      JsonObject requestJson = new JsonObject();
      JsonObject authInfo = new JsonObject().put(HEADER_TOKEN, token).put("id", id);
      LOGGER.debug("<AuthHandler> {}", authInfo.toString());
      Future<JsonObject> resultFromAuth= authenticator.tokenIntrospect(requestJson, authInfo);
      resultFromAuth
          .onSuccess(result -> {
              // the authInfo will be used later for metering and auditing purposes
              authInfo.put("iid",result.getValue("iid"));
              authInfo.put("userId", result.getValue("userId"));
              authInfo.put("expiry", result.getValue("expiry"));
              authInfo.put("role", result.getValue("role"));
              context.data().put("authInfo", authInfo);
              context.data().put("isAuthorised", result.getBoolean("isAuthorised"));
              context.next();
      })
          .onFailure(failed -> {
              context.put("isAuthorised", false);
              LOGGER.debug("isAuthorised? {}", context.get("isAuthorised").toString());
              if (failed instanceof OgcException){
                  if(((OgcException) failed).getJson().getString("code").equals("401")) {
                      context.put("response", ((OgcException) failed).getJson().toString());
                      context.put("statusCode", 401);
                  }
                  if(((OgcException) failed).getJson().getString("code").equals("404")) {
                      context.put("response", ((OgcException) failed).getJson().toString());
                      context.put("statusCode", 404);
                  }
              } else {
                  context.put("response",
                      new OgcException(401, "Not Authorised",
                          "User is not authorised. Please contact IUDX AAA Server.").getJson().toString());
                  context.put("statusCode", 401);
              }
              context.next();
          });
    }
}
