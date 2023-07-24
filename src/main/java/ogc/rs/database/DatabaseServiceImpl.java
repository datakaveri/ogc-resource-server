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

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DatabaseServiceImpl implements DatabaseService{
    private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);

    private final PgPool client;

    public DatabaseServiceImpl(final PgPool pgClient) {
        this.client = pgClient;
    }

    @Override
    public Future<JsonObject> getCollection(String collectionId) {
        LOGGER.info("getCollection");
        Promise<JsonObject> result = Promise.promise();
        Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
        client.withConnection(conn ->
           conn.preparedQuery("Select * from collections_details where id = $1::text")
               .collecting(collector)
               .execute(Tuple.of(collectionId)).map(SqlResult::value))
            .onSuccess(success -> {
                LOGGER.debug("DB result - {}", success);
                if (success.isEmpty())
                    result.fail(new OgcException("NotFound", "Collection not found"));
                else {
                    JsonObject result_ogc =  buildCollectionResult(success);
                    LOGGER.debug("Built OGC Collection Response - {}", result_ogc);
                    result.complete(result_ogc);
                }
            })
            .onFailure(fail -> {
                LOGGER.error("Failed at getCollection- {}",fail.getMessage());
                result.fail("Error!");
            });
        return result.future();
    }

    private JsonObject buildCollectionResult(List<JsonObject> success) {
        JsonObject collection = success.get(0);
        // collection.put("id",collection.getString("id"));
        collection.put("links", new JsonArray()
            .add(new JsonObject()
                .put("href","http://localhost/collections/" + collection.getString("id"))
                .put("rel","self")
                .put("title", collection.getString("title"))
                .put("description", collection.getString("description"))))
            .put("itemType", "feature")
            .put("crs", new JsonArray().add("http://www.opengis.net/def/crs/ESPG/0/4326"));
        collection.remove("title");
        collection.remove("description");
        return collection;
    }

    public Future<JsonArray> getCollections() {
        JsonArray collections = new JsonArray();
        Promise<JsonArray> result = Promise.promise();
        Collector<Row, ?, List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
        client.withConnection(conn ->
                //TODO: here we can use limit (default or provided by the user)
                conn.preparedQuery("Select * from collections_details")
                    .collecting(collector)
                    .execute()
                    .map(SqlResult::value))
            .onSuccess(success -> {
                if (success.isEmpty()) {
                    LOGGER.error("Collections table is empty!");
                    result.fail("Error!");
                } else {
                    success.forEach(collection -> {
                        try {
                            JsonObject json;
                            List<JsonObject> tempArray = new ArrayList<>();
                            tempArray.add(collection);
                            json = buildCollectionResult(tempArray);
                            collections.add(json);
                        } catch (Exception e) {
                            System.out.println("Ouch!- " + e.getMessage());
                            result.fail("Error!");
                        }
                    });
                    result.complete(collections);
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
        Collector<Row, ? , String> collectorT = Collectors.mapping(row -> row.getString("to_regclass"), Collectors.joining());
        Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
       /* check the json to figure out what sql query needs to be used
            * limit int, ex. 10,100, etc
            * bbox string,ex. "[10,20,30,40]", "[10,20,30,40,50,60]"
            * datetime string, ex. "2023-04-23T10:10:30-..", "..-2023-04-23T10:10:30", "2023-04-23T10:10:30-2023-04-23T10:10:40"
            *   !!! should not worry about datetime since district_hq does not have time property
            * filter string
            * SQL Query = buildSqlQuery(params);
        * buildSqlQuery can be a builder/fluent class, with functions limit(),bbox(),datetime(),filter(), and
            *   [build() --> which will build the query string]
        * if (limit is !null) --> .limit($params.getInteger(limit))
            * if (bbox is !null) --> .bbox($params.getString(bbox))
            * if (datetime is !null) --> .datetime($params.getString(datetime))
            * if (filter is !null)--> .filter($params.getString(filter))
            * essentially would look like sqlString = new buildSqlQuery()
            *                                           .limit()
            *                                           .bbox()
            *                                           .filter(based on properties field in the db) --> validation??
        *                                           .build();*/
        String sqlQuery;
        FeatureQueryBuilder featureQuery = new FeatureQueryBuilder(collectionId);
        if (queryParams.containsKey("limit"))
            featureQuery.setLimit(Integer.parseInt(queryParams.get("limit")));
        if (queryParams.containsKey("bbox"))
            featureQuery.setBbox(queryParams.get("bbox"));
        if (queryParams.containsKey("datetime"))
            featureQuery.setDatetime(queryParams.get("datetime"));
        Set<String> keys =  queryParams.keySet();
        Set<String> predefinedKeys = Set.of("limit", "bbox", "datetime");
        keys.removeAll(predefinedKeys);
        String[] key = keys.toArray(new String[keys.size()]);
        if (!keys.isEmpty())
            featureQuery.setFilter(key[0], queryParams.get(key[0]));
        sqlQuery = featureQuery.buildSqlString();
        System.out.println("<DBService> Sql query- " + sqlQuery);
        client.withConnection(conn ->
            conn.preparedQuery("select to_regclass($1::text)")
                .collecting(collectorT)
                .execute(Tuple.of(collectionId))
                .onSuccess(conn1 -> {
                    if (conn1.value().equals("null")) {
                        result.fail(new OgcException("NotFound", "Collection not found"));
                        return;
                    }
                    conn.preparedQuery(sqlQuery)
                    .collecting(collector).execute().map(SqlResult::value)
                        .onSuccess(success -> {
                            if (success.isEmpty())
                                result.fail(new OgcException("NotFound", "Features not found"));
                            else
                                result.complete(new JsonObject()
                                    .put("type","FeatureCollection")
                                    .put("features",new JsonArray(success)));
                        })
                        .onFailure(failed -> {
                            LOGGER.error("Failed at getFeatures- {}",failed.getMessage());
                            result.fail("Error!");
                        });
                })
                .onFailure(fail -> {
                    LOGGER.error("Failed at to_regclass- {}",fail.getMessage());
                    result.fail("Error!");
                }));
            /*.onSuccess(success -> {
                System.out.println("Success!!! - " + success.toString());
                if (success.isEmpty())
                    result.fail(new OgcException("NotFound", "Features not found"));
                else
                    result.complete(new JsonArray(success));
            })
            .onFailure(fail -> {
                LOGGER.error("Failed at getFeatures- {}",fail.getMessage());
                result.fail("Error!");
            });*/
        return result.future();
    }

    @Override
    public Future<JsonObject> getFeature(String collectionId, String featureId) {
        LOGGER.info("getFeature");
        Promise<JsonObject> result = Promise.promise();
        Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
        Collector<Row, ? , String> collectorT = Collectors.mapping(row -> row.getString("to_regclass"), Collectors.joining());
        client.withConnection(conn ->
            conn.preparedQuery("select to_regclass($1::text)")
                .collecting(collectorT)
                .execute(Tuple.of(collectionId))
                .onSuccess(conn1 -> {
                    if (conn1.value().equals("null")) {
                        result.fail(new OgcException("NotFound", "Collection not found"));
                        return;
                    }
                    String sqlQuery = "Select id, itemType as type, cast(st_asgeojson(geom) as json) as geometry, properties from "
                        + collectionId + " where id=$1::UUID" ;
                    conn.preparedQuery(sqlQuery)
                        .collecting(collector).execute(Tuple.of(UUID.fromString(featureId)))
                        .map(SqlResult::value)
                        .onSuccess(success -> {
                            if (success.isEmpty())
                                result.fail(new OgcException("NotFound", "Features not found"));
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
//        client.withConnection(conn ->
//                conn.preparedQuery("Select itemType as type, st_asgeojson(geoc) as geometry, properties from $1::text"
//                        + " where id = $2::uuid")
//                    .collecting(collector)
//                    .execute(Tuple.of(collectionId, UUID.fromString(featureId))).map(SqlResult::value))
//            .onSuccess(success -> {
//                System.out.println("Success!!! - " + success.toString());
//                if (success.isEmpty())
//                    result.fail(new OgcException("NotFound", "Feature not found"));
//                else
//                    result.complete(success.get(0));
//            })
//            .onFailure(fail -> {
//                LOGGER.error("Failed at getFeature- {}",fail.getMessage());
//                result.fail("Error!");
//            });

        return result.future();
    }
}
