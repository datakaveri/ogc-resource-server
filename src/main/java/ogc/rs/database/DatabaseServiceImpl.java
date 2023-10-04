package ogc.rs.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.database.util.FeatureQueryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DatabaseServiceImpl implements DatabaseService{
    private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);

    private final PgPool client;
    private final JsonObject datetimeConfig;

    public DatabaseServiceImpl(final PgPool pgClient) {
        this.client = pgClient;
    }

    @Override
    public Future<List<JsonObject>> getCollection(String collectionId) {
        LOGGER.info("getCollection");
        Promise<List<JsonObject>> result = Promise.promise();
        Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
        client.withConnection(conn ->
           conn.preparedQuery("Select id, title, description, datetime_key from collections_details where id = " +
                   "$1::UUID")
               .collecting(collector)
               .execute(Tuple.of(UUID.fromString( collectionId))).map(SqlResult::value))
            .onSuccess(success -> {
                LOGGER.debug("DB result - {}", success);
                if (success.isEmpty())
                result.fail(new OgcException(404, "Not found", "Collection not found"));
                else {
                    LOGGER.debug("Built OGC Collection Response - {}", success);
                    result.complete(success);
                }
            })
            .onFailure(fail -> {
                LOGGER.error("Failed at getCollection- {}",fail.getMessage());
                result.fail("Error!");
            });
        return result.future();
    }
    
    @Override
    public Future<List<JsonObject>> getCollections() {
        Promise<List<JsonObject>> result = Promise.promise();
        Collector<Row, ?, List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
        client.withConnection(conn ->
                conn.preparedQuery("Select id, title, description, datetime_key from collections_details")
                    .collecting(collector)
                    .execute()
                    .map(SqlResult::value))
            .onSuccess(success -> {
                if (success.isEmpty()) {
                    LOGGER.error("Collections table is empty!");
                    result.fail("Error!");
                } else {
                  LOGGER.debug("Collections Result: {}", success.toString());
                  result.complete(success);
                }
            })
            .onFailure(fail -> {
                LOGGER.error("Failed to getCollections! - {}", fail.getMessage());
                result.fail("Error!");
            });
        return result.future();
    }
    @Override
    public Future<JsonObject> getFeatures(String collectionId, Map<String, String> queryParams) {
      LOGGER.info("getFeatures");
      Promise<JsonObject> result = Promise.promise();
      Collector<Row, ? , Map<String, Integer>> collectorT = Collectors.toMap(row -> row.getColumnName(0),
          row -> row.getInteger("count"));
      Collector<Row, ? , Map<String, String>> collectorDatetime = Collectors.toMap(row -> row.getColumnName(0),
        row -> row.getString("datetime_key"));
      Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
      String datetimeValue;
        FeatureQueryBuilder featureQuery = new FeatureQueryBuilder(collectionId);
        if (queryParams.containsKey("limit"))
            featureQuery.setLimit(Integer.parseInt(queryParams.get("limit")));
        if (queryParams.containsKey("bbox"))
            featureQuery.setBbox(queryParams.get("bbox"));
      //TODO: convert individual DB calls to a transaction
      datetimeValue = queryParams.getOrDefault("datetime", null);
      if (queryParams.containsKey("offset"))
            featureQuery.setOffset(Integer.parseInt(queryParams.get("offset")));
        Set<String> keys =  queryParams.keySet();
        Set<String> predefinedKeys = Set.of("limit", "bbox", "datetime", "offset");
        keys.removeAll(predefinedKeys);
        String[] key = keys.toArray(new String[keys.size()]);
        if (!keys.isEmpty())
            featureQuery.setFilter(key[0], queryParams.get(key[0]));
      client.withConnection(conn ->
        conn.preparedQuery("select datetime_key, count(id) from collections_details " +
                    "where id = $1::uuid group by id, datetime_key")
                .collecting(collector)
                .execute(Tuple.of(UUID.fromString(collectionId)))
                .onSuccess(conn1 -> {
                  LOGGER.debug("Count collection- {}", conn1.value().get(0).getInteger("count"));
                  if (conn1.value().get(0).getInteger("count") == 0) {
                    result.fail(new OgcException(404, "Not found", "Collection not found"));
                    return;
                  }
                  if (conn1.value().get(0).getString("datetime_key") != null && datetimeValue != null ){
                    LOGGER.debug("datetimeKey: {}, datetimeValue: {}"
                        ,conn1.value().get(0).getString("datetime_key"), datetimeValue);
                    featureQuery.setDatetimeKey(conn1.value().get(0).getString("datetime_key"));
                    featureQuery.setDatetime(datetimeValue);
                  }
                  LOGGER.debug("datetime_key: {}",conn1.value().get(0).getString("datetime_key"));
                  System.out.println("<DBService> Sql query- " + featureQuery.buildSqlString());
                  LOGGER.debug("Count Query- {}", featureQuery.buildSqlString("count"));
                  JsonObject resultJson = new JsonObject();
                  conn.preparedQuery(featureQuery.buildSqlString("count"))
                      .collecting(collectorT).execute()
                      .onSuccess(count -> {
                        LOGGER.debug("Feature Count- {}",count.value().get("count"));
                        int totalCount = count.value().get("count");
                        resultJson.put("numberMatched", totalCount);
                        int numReturn = Math.min(featureQuery.getLimit(), totalCount);
                        resultJson.put("numberReturned", numReturn );
                      })
                      .onFailure(countFail -> {
                        LOGGER.error("Failed to get the count of number of features!");
                        result.fail("Error!");
                      })
                      .compose(sql -> {
                        conn.preparedQuery(featureQuery.buildSqlString())
                            .collecting(collector).execute().map(SqlResult::value)
                            .onSuccess(success -> {
                              if (success.isEmpty())
                                result.fail(new OgcException(404, "Not found", "Features not found"));
                              else {
                                result.complete(resultJson
                                    .put("type","FeatureCollection")
                                    .put("features",new JsonArray(success)));
                              }
                            })
                            .onFailure(failed -> {
                              LOGGER.error("Failed at getFeatures- {}",failed.getMessage());
                              result.fail("Error!");
                            });
                        return result.future();
                      });
                })
                .onFailure(fail -> {
                  LOGGER.error("Failed at find_collection- {}",fail.getMessage());
                  result.fail("Error!");
                }));
        return result.future();
    }

    @Override
    public Future<JsonObject> getFeature(String collectionId, String featureId) {
        LOGGER.info("getFeature");
        Promise<JsonObject> result = Promise.promise();
        Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
      Collector<Row, ? , Map<String, Integer>> collectorT = Collectors.toMap(row -> row.getColumnName(0)
          , row -> row.getInteger("count"));
      client.withConnection(conn ->
            conn.preparedQuery("select count(*) from collections_details where id = $1::uuid")
                .collecting(collectorT)
                .execute(Tuple.of(UUID.fromString(collectionId)))
                .onSuccess(conn1 -> {
                    if (conn1.value().get("count") == 0) {
                        result.fail(new OgcException(404, "Not found", "Collection not found"));
                        return;
                    }
                    String sqlQuery = "Select id, itemType as type, cast(st_asgeojson(geom) as json) as geometry, " +
                        "properties from \"" + collectionId + "\" where id=$1::UUID" ;
                    conn.preparedQuery(sqlQuery)
                        .collecting(collector).execute(Tuple.of(UUID.fromString(featureId)))
                        .map(SqlResult::value)
                        .onSuccess(success -> {
                            if (success.isEmpty())
                                result.fail(new OgcException(404, "Not found", "Feature not found"));
                            else
                                result.complete(success.get(0));
                        })
                        .onFailure(failed -> {
                            LOGGER.error("Failed at getFeature- {}",failed.getMessage());
                            result.fail("Error!");
                        });
                })
                .onFailure(fail -> {
                    LOGGER.error("Failed at to_regclass- {}",fail.getMessage());
                    result.fail("Error!");
                }));

        return result.future();
    }
}
