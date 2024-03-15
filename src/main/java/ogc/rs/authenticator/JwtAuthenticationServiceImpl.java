package ogc.rs.authenticator;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import net.sf.saxon.expr.Token;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.authenticator.model.JWTData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static ogc.rs.authenticator.Constants.CAT_SEARCH_PATH;

public class JwtAuthenticationServiceImpl implements AuthenticationService {
    private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);
    static WebClient catClient;
    final JWTAuth jwtAuth;
    final String audience;
    private PgConnectOptions connectOptions;
    private PoolOptions poolOptions;
    private PgPool pooledClient;
    private String databaseIp;
    private int databasePort;
    private String databaseName;
    private String databaseUserName;
    private String databasePassword;
    private int poolSize;

    public JwtAuthenticationServiceImpl(Vertx vertx, JWTAuth jwtAuth, JsonObject config) {
        this.jwtAuth = jwtAuth;
        this.audience = config.getString("audience");
        this.databaseIp = config.getString("databaseHost");
        this.databasePort = config.getInteger("databasePort");
        this.databaseName = config.getString("databaseName");
        this.databaseUserName = config.getString("databaseUser");
        this.databasePassword = config.getString("databasePassword");
        this.poolSize = config.getInteger("poolSize");
        this.connectOptions =
            new PgConnectOptions()
                .setPort(databasePort)
                .setHost(databaseIp)
                .setDatabase(databaseName)
                .setUser(databaseUserName)
                .setPassword(databasePassword)
                .setReconnectAttempts(2)
                .setReconnectInterval(1000L);

        this.poolOptions = new PoolOptions().setMaxSize(poolSize);
        this.pooledClient = PgPool.pool(vertx, connectOptions, poolOptions);
    }


    @Override
    public Future<JsonObject> tokenIntrospect(JsonObject request, JsonObject authenticationInfo) {
        Promise<JsonObject> result = Promise.promise();
        String id, token;
        try {
            id = authenticationInfo.getString("id");
            token = authenticationInfo.getString("token");
        } catch (NullPointerException e) {
            LOGGER.error("NullPointer Exception while getting JSONObj values");
            result.fail("NullPointer error!");
            return result.future();
        }
        Future<JWTData> jwtDecodeFut = decodeJwt(token);
        ResultContainer resultIntermediate = new ResultContainer();
        // what the hell is this assert for? Does it help?
        assert jwtDecodeFut != null;
        jwtDecodeFut
            .compose(decode -> {
                resultIntermediate.jwtData = decode;
                LOGGER.debug("Intermediate JWTData: {}\n", resultIntermediate.jwtData.toJson().toString());
                return isValidAudience(resultIntermediate.jwtData);
            })
            .compose(audience -> {
                LOGGER.debug("Valid Audience: {}\n" , audience);
                // check for revoked client here before returning true
                return Future.succeededFuture(true);
            })
            .compose(validClient -> {
                // check for something else that I don't understand
                LOGGER.debug("Revoked client is always {}", validClient);
                return isOpenResource(id);
            })
            .compose(openResource -> {
                LOGGER.debug("Is it an open resource: {}", openResource);
                resultIntermediate.isOpen = openResource;
                if(openResource) {
                    return Future.succeededFuture(true);
                } else {
                    return isValidId(resultIntermediate.jwtData, id);
                }
            })
            .compose(validId -> {
                if (validId && resultIntermediate.jwtData.getRole().equalsIgnoreCase("consumer")){
                    return validateAccess(resultIntermediate.jwtData, resultIntermediate.isOpen,
                authenticationInfo);
                }
                return Future.succeededFuture(new JsonObject());
            })
            .onSuccess(success -> {
                success.put("isAuthorised", true);
                result.complete(success);
                LOGGER.debug("Congratulations! It worked. {}", (success).toString());
                //
            })
            .onFailure(failed -> {
                LOGGER.error("Something went wrong while authentication or authorisation:(\n Show message {}",
                    failed.getMessage());
                // I don't know what to put here, check and confirm
                result.fail(failed);
            });

        return result.future();
    }

  @Override
  public Future<JsonObject> assetApiCheck(JsonObject requestJson, JsonObject authenticationInfo) {
    Promise<JsonObject> result = Promise.promise();
    String assetId, token;
    try {
      assetId = authenticationInfo.getString("id");
      token = authenticationInfo.getString("token");
    } catch (NullPointerException e) {
      LOGGER.error("NullPointer Exception while getting JSONObj values");
      result.fail("NullPointer error!");
      return result.future();
    }
    Future<JWTData> jwtDecodeFut = decodeJwt(token);
    ResultContainer resultIntermediate = new ResultContainer();
    assert jwtDecodeFut != null;
    jwtDecodeFut
        .compose(
            decode -> {
              resultIntermediate.jwtData = decode;
              LOGGER.debug(
                  "Intermediate JWTData: {}\n", resultIntermediate.jwtData.toJson().toString());
              return isValidAudience(resultIntermediate.jwtData);
            })
        .compose(
            audience -> {
              LOGGER.debug("Valid Audience: {}\n", audience);
              // check for revoked client here before returning true
              return Future.succeededFuture(true);
            })
        .compose(
            validClient -> {
              // check for something else that I don't understand
              LOGGER.debug("Revoked client is always {}", validClient);
              return validateAssetAccess(assetId, resultIntermediate.jwtData);
            })
        .onSuccess(
            success -> {
              success.put("isAuthorised", true);
              result.complete(success);
              LOGGER.debug("Congratulations! It worked. {}", (success).toString());
            })
        .onFailure(
            failed -> {
              LOGGER.error(
                  "Something went wrong while authentication or authorisation:(\n Show message {}",
                  failed.getMessage());
              result.fail(failed);
            });

    return result.future();
  }

    private Future<Boolean> isValidId(JWTData jwtData, String id) {
        // check if the id in the token matches the id in the request/path param.
        Promise<Boolean> promise = Promise.promise();
        String idFromJwt = jwtData.getIid().split(":")[1];
        if(id.equalsIgnoreCase(idFromJwt))
            promise.complete(true);
        else {
            LOGGER.error("Resource Ids don't match! id- {}, jwtId- {}", id, idFromJwt);
            promise.fail(new OgcException(401, "Not Authorised", "User is not authorised. Please contact IUDX AAA " +
                "Server."));
        }
        return promise.future();
    }

    private Future<JsonObject> validateAccess(JWTData jwtData, boolean isOpen, JsonObject authenticationInfo) {
        // this is where the magic happens
        Promise<JsonObject> promise = Promise.promise();
        String idFromJwt = jwtData.getIid().split(":")[1];

        if(isOpen && jwtData.getAud().equalsIgnoreCase(idFromJwt)) {
            LOGGER.debug("Resource is Open and hence granted access!");
            JsonObject result = new JsonObject();
            result.put("iid",  idFromJwt);
            result.put("userId", jwtData.getSub());
            result.put("role", jwtData.getRole());
            result.put("expiry", LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(Long.parseLong(jwtData.getExp().toString())),
                    ZoneId.systemDefault())
                .toString());
            promise.complete(result);
            return promise.future();
        }
        if (!jwtData.getRole().equalsIgnoreCase("consumer")){
            JsonObject result = new JsonObject();
            result.put("iid",  idFromJwt);
            result.put("userId", jwtData.getSub());
            result.put("role", jwtData.getRole());
            result.put("expiry", LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(Long.parseLong(jwtData.getExp().toString())),
                    ZoneId.systemDefault())
                .toString());
            promise.complete(result);
            return promise.future();
        }
        JsonArray access = jwtData.getCons() != null ? jwtData.getCons().getJsonArray("access") : null;
        if (access == null)
            promise.fail(new OgcException(401, "Not Authorised", "User is not authorised. Please contact IUDX AAA " +
                "Server."));
        else {
            if (access.contains("api")
                && jwtData.getRole().equalsIgnoreCase("consumer")) {
                JsonObject result = new JsonObject();
                result.put("iid",  idFromJwt);
                result.put("userId", jwtData.getSub());
                result.put("role", jwtData.getRole());
                result.put("expiry", LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(Long.parseLong(jwtData.getExp().toString())),
                        ZoneId.systemDefault())
                    .toString());
                promise.complete(result);
            }
        }
        return promise.future();
    }

    private Future<Boolean> isOpenResource(String id) {
        Promise<Boolean> promise = Promise.promise();
        String sqlString = "select access from ri_details where id = $1::uuid";
        Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());

        pooledClient.withConnection(conn ->
            conn.preparedQuery(sqlString)
                .collecting(collector)
                .execute(Tuple.of(UUID.fromString(id)))
                .map(SqlResult::value))
            .onSuccess(success -> {
                if (success.isEmpty()){
                    promise.fail(new OgcException(404, "Not found", "Collection not found"));
                }
                else {
                    String access = success.get(0).getString("access");
                    if (access.equalsIgnoreCase("secure")) {
                        promise.complete(false);
                    }
                    else if (access.equalsIgnoreCase("open")){
                        promise.complete(true);
                    }
                }
            })
            .onFailure(fail -> {
                LOGGER.error("Something went wrong at isOpenResource: {}", fail.getMessage() );
                promise.fail(new OgcException(500, "Internal Server Error","Internal Server Error"));
            });
        return promise.future();
    }

    private Future<Boolean> isValidAudience(JWTData jwtData) {
        // check if the audience is valid
        Promise<Boolean> promise = Promise.promise();
        if (audience != null && audience.equalsIgnoreCase(jwtData.getAud()))
            promise.complete(true);
        else {
            LOGGER.error("Audience value does not match aud- {} and audJwt- {}", audience, jwtData.getAud());
            promise.fail(new OgcException(401, "Not Authorised", "User is not authorised. Please contact IUDX AAA " +
                "Server."));
        }
        return promise.future();
    }

    private Future<JWTData> decodeJwt(String token) {
        // decoding JWT token
        Promise<JWTData> promise = Promise.promise();
        TokenCredentials credentials = new TokenCredentials(token);

        jwtAuth
            .authenticate(credentials)
            .onSuccess(user -> {
                JWTData jwtData = new JWTData(user.principal());
                jwtData.setExp(user.get("exp"));
                jwtData.setIat(user.get("iat"));
                promise.complete(jwtData);
            })
            .onFailure(failed -> {
                LOGGER.error("Cannot decode/validate JWT Token: {}", failed.getMessage());
                promise.fail(new OgcException(401, "Not Authorised", "User is not authorised. Please contact IUDX AAA " +
                    "Server." ));
            });
        return promise.future();
    }

    private static class ResultContainer {
        JWTData jwtData;
        boolean isOpen;
    }

  /**
   * The validateAssetAccess method validates if the assetID is present or not. It then checks for
   * the collectionId associate with it and verifies it with the token.
   *
   * @param assetId which is a String
   * @param jwtData The JWTData object containing relevant information extracted from the JWT.
   * @return A Future<JsonObject> representing the result of the validation. If successful, the
   *     JsonObject contains authorization details. If unsuccessful, the Future is completed with an
   *     OgcException.
   */
  private Future<JsonObject> validateAssetAccess(String assetId, JWTData jwtData) {
    Promise<JsonObject> promise = Promise.promise();
    String idFromJwt = jwtData.getIid().split(":")[1];
    Collector<Row, ?, List<JsonObject>> collector =
        Collectors.mapping(Row::toJson, Collectors.toList());

    pooledClient
        .withConnection(
            conn ->
                conn.preparedQuery(
                        "select id, stac_collections_id from stac_collections_assets where id = $1::uuid")
                    .collecting(collector)
                    .execute(Tuple.of(UUID.fromString(assetId)))
                    .map(SqlResult::value))
        .onSuccess(
            success -> {
              if (success.isEmpty()) {
                promise.fail(new OgcException(404, "Not Found", "Asset not found"));
                return;
              }
              LOGGER.debug("Asset Found {} ", success.get(0));
              isOpenResource(success.get(0).getString("stac_collections_id"))
                  .onSuccess(
                      resourceResult -> {
                        boolean openResource =
                            jwtData.getIid().split(":")[0].equals("rs")
                                && jwtData.getIid().split(":")[1].equals("ogc.iudx.io");
                        if (resourceResult && openResource) {
                          LOGGER.debug("Resource is Open, Access Granted.");
                          JsonObject results = new JsonObject();
                          results.put("iid", idFromJwt);
                          results.put("userId", jwtData.getSub());
                          results.put("role", jwtData.getRole());
                          results.put(
                              "expiry",
                              LocalDateTime.ofInstant(
                                      Instant.ofEpochSecond(
                                          Long.parseLong(jwtData.getExp().toString())),
                                      ZoneId.systemDefault())
                                  .toString());
                          promise.complete(results);
                          return;
                        } else if (!resourceResult) {
                          LOGGER.debug("Not an open resource. Its  a secure resource");
                          if (!success.get(0).getString("stac_collections_id").equals(idFromJwt)) {
                            LOGGER.error(
                                "Collection associated with asset is not same as in token");
                            promise.fail(
                                new OgcException(401, "Not Authorised", "Invalid Collection Id"));
                            return;
                          }
                          LOGGER.debug("Collection Id in token Validated ");
                          if (jwtData.getRole().equalsIgnoreCase("provider")) {
                            JsonObject results = new JsonObject();
                            results.put("iid", idFromJwt);
                            results.put("userId", jwtData.getSub());
                            results.put("role", jwtData.getRole());
                            results.put(
                                "expiry",
                                LocalDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                            Long.parseLong(jwtData.getExp().toString())),
                                        ZoneId.systemDefault())
                                    .toString());
                            promise.complete(results);
                            return;
                          }
                          JsonArray access =
                              jwtData.getCons() != null
                                  ? jwtData.getCons().getJsonArray("access")
                                  : null;
                          if (access == null)
                            promise.fail(
                                new OgcException(
                                    401,
                                    "Not Authorised",
                                    "User is not authorised. Please contact IUDX AAA "
                                        + "Server."));
                          else {
                            if (access.contains("api")
                                && jwtData.getRole().equalsIgnoreCase("consumer")) {
                              JsonObject results = new JsonObject();
                              results.put("iid", idFromJwt);
                              results.put("userId", jwtData.getSub());
                              results.put("role", jwtData.getRole());
                              results.put(
                                  "expiry",
                                  LocalDateTime.ofInstant(
                                          Instant.ofEpochSecond(
                                              Long.parseLong(jwtData.getExp().toString())),
                                          ZoneId.systemDefault())
                                      .toString());
                              promise.complete(results);
                              return;
                            }
                          }
                        }
                      })
                  .onFailure(
                      fail -> {
                        LOGGER.error("Collection not present in table" + fail.getMessage());
                        promise.fail(fail);
                      });
              ;
            })
        .onFailure(
            fail -> {
              LOGGER.error("Asset not found: " + fail.getMessage());
              promise.fail(fail);
            });

    return promise.future();
  }
}
