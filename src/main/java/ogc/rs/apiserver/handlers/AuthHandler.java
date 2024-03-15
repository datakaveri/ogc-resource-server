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

import static ogc.rs.apiserver.util.Constants.JOB_STATUS_REGEX;
import static ogc.rs.apiserver.util.Constants.PROCESS_EXECUTION_REGEX;
import static ogc.rs.apiserver.util.Constants.HEADER_TOKEN;
import static ogc.rs.common.Constants.AUTH_SERVICE_ADDRESS;
import static ogc.rs.common.Constants.UUID_REGEX;

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

      if(System.getProperty("disable.auth") != null)
      {
          context.put("isAuthorised", true);
          context.next();
          return;
      }

      HttpServerRequest request = context.request();
      String token;
      String id;
      String path = context.normalizedPath();
      boolean isProcessExecution = path.matches(PROCESS_EXECUTION_REGEX) || path.matches(JOB_STATUS_REGEX);
      token = request.headers().get(HEADER_TOKEN);
      id = context.normalizedPath().split("/")[2];
      if (context.request().path().substring(1, 7).equals("assets")) {
        id = context.pathParam("assetId");
      }

      /* TODO : Remove once spec validation is being done */
      if (!isProcessExecution && !id.matches(UUID_REGEX)) {
        context.put("isAuthorised", false);
        context.put(
            "response", new OgcException(404, "Not found", "Collection not found").getJson().toString());
        context.put("statusCode", 404);
        context.next();
        return;
      }
      // requestJson will be used by the metering service
      if (token == null) {
        LOGGER.error("Null values for either token or id!");
        context.put("isAuthorised",  false);
        context.fail(unAuthorizedException());
        return;
      }
      JsonObject requestJson = new JsonObject();
      JsonObject authInfo = new JsonObject().put(HEADER_TOKEN, token).put("id", id);

      if (isProcessExecution) {
        Future<JsonObject> resultFromAuth = authenticator.executionApiCheck(authInfo, requestJson);
        resultFromAuth
          .onSuccess(
            result -> {
              context.data().put("authInfo", result);
              context.data().put("isAuthorised", result.getBoolean("isAuthorised"));
              context.next();
            })
          .onFailure(
              context::fail);
      }
      else if (context.request().path().substring(1, 7).equals("assets")) {
        /* TODO : Remove once spec validation is being done */
        if (id==null || !id.matches(UUID_REGEX)) {
          context.put("isAuthorised",  false);
          OgcException ogcException =new OgcException(401, "Not Authorised", "User is not Authorised. Please contact IUDX AAA Server.");
          context.fail(ogcException);
          return;
        }
        Future<JsonObject> resultFromAuth = authenticator.assetApiCheck(requestJson, authInfo);
        resultFromAuth
                .onSuccess(
                        result -> {
                            context.data().put("authInfo", authInfo);
                            context.data().put("isAuthorised", result.getBoolean("isAuthorised"));
                            context.next();
                        })
                .onFailure(
                        failed -> {
                            context.put("isAuthorised", false);
                            LOGGER.debug("isAuthorised? {}", context.get("isAuthorised").toString());
                            if (failed instanceof OgcException) {
                              context.put("statusCode", ((OgcException) failed).getStatusCode());
                              context.put("response", ((OgcException) failed).getJson().toString());
                            } else {
                                context.put(
                                        "response",
                                        new OgcException(500, "Internal Server Error", "Internal Server Error")
                                                .getJson()
                                                .toString());
                                context.put("statusCode", 500);
                                LOGGER.debug("statusCode? {}", context.get("statusCode").toString());
                            }
                            context.next();
                        });
      } else {
        /* TODO : Remove once spec validation is being done */
        if (id==null || !id.matches(UUID_REGEX)) {
          context.put("isAuthorised", false);
          context.put(
            "response", new OgcException(404, "Not Found", "Asset Not Found").getJson().toString());
          context.put("statusCode", 404);
          context.next();
          return;
        }
      Future<JsonObject> resultFromAuth = authenticator.tokenIntrospect(requestJson, authInfo);
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
                context.fail(failed);
              } else {
                context.fail(internalServerError());
                LOGGER.debug("statusCode? {}", context.get("statusCode").toString());
              }
          });
    }
  }
  private OgcException unAuthorizedException(){
    return new OgcException(401, "Not Authorised", "User is not Authorised. Please contact IUDX AAA Server.");
  }
  private OgcException internalServerError(){
    return new OgcException(500, "Internal Server Error", "Internal Server Error");
  }
}
