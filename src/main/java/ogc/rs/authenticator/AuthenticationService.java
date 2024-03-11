package ogc.rs.authenticator;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Authentication Service.
 * <h1>Authentication Service</h1>
 * <p>monthlyOverview
 * The Authentication Service in the OGC Resource Server defines the operations to be performed
 * with the IUDX Authentication, Authorization and Accounting server.
 * </p>
 *
 * @version 1.0
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface AuthenticationService {
    /**
     * The createProxy helps the code generation blocks to generate proxy code.
     *
     * @param vertx which is the vertx instance
     * @param address which is the proxy address
     * @return AuthenticationServiceVertxEBProxy which is a service proxy
     */

    @GenIgnore
    static AuthenticationService createProxy(Vertx vertx, String address) {
        return new AuthenticationServiceVertxEBProxy(vertx, address);
    }

    /**
     * The tokenIntrospect method implements the authentication and authorization module using IUDX
     * APIs. It caches the result of the TIP from the auth server for a duration specified by the
     * Constants TIP_CACHE_TIMEOUT_AMOUNT and TIP_CACHE_TIMEOUT_UNIT.
     *
     * @param request which is a JsonObject containing ids: [String]
     * @param authenticationInfo which is a JsonObject containing token: String and apiEndpoint:
     *        String
     * @param handler which is a request handler
     * @return AuthenticationService which is a service
     */

Future<JsonObject> tokenIntrospect(JsonObject request, JsonObject authenticationInfo);

  /**
   * The assetApiCheck method implements the authentication and authrization module using IUDX APIS
   * for the asset API endpoint.
   *
   * @param requestJson which is jsonObject containing ids: [String]
   * @param authInfo which is a JsonObject containing token: String and apiEndpoint: String
   * @param handler which is a request handler
   * @return AuthenticationService which is a service
   */
  Future<JsonObject> assetApiCheck(JsonObject requestJson, JsonObject authInfo);
}
