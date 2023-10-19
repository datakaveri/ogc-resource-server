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
import static ogc.rs.common.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DatabaseServiceImpl implements DatabaseService{
    private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);

    private final PgPool client;

    public DatabaseServiceImpl(final PgPool pgClient) {
        this.client = pgClient;
    }
    public Set<String> predefinedKeys = Set.of("limit", "bbox", "datetime", "offset", "bbox-crs", "crs");

    @Override
    public Future<List<JsonObject>> getCollection(String collectionId) {
        LOGGER.info("getCollection");
        Promise<List<JsonObject>> result = Promise.promise();
        
        /* TODO : Remove once spec validation is being done */
        if (!collectionId.matches(UUID_REGEX)) {
          result.fail(new OgcException(404, "Not found", "Collection not found"));
          return result.future();
        }
        
        Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
        client.withConnection(conn ->
           conn.preparedQuery("select collections_details.id, title, description, datetime_key," +
                   " array_agg(crs_to_srid.crs) as crs, collections_details.crs as \"storageCrs\"," +
                   " bbox, temporal" +
                   " from collections_details join collection_supported_crs" +
                   " on collections_details.id = collection_supported_crs.collection_id join crs_to_srid" +
                   " on crs_to_srid.id = collection_supported_crs.crs_id group by collections_details.id" +
                   " having collections_details.id = $1::uuid")
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
                conn.preparedQuery("select collections_details.id, title, array_agg(crs_to_srid.crs) as crs, " +
                        "collections_details.crs as \"storageCrs\", description, datetime_key, bbox, temporal" +
                        " from collections_details join collection_supported_crs" +
                        " on collections_details.id = collection_supported_crs.collection_id" +
                        " join crs_to_srid on crs_to_srid.id = collection_supported_crs.crs_id" +
                        " group by collections_details.id")
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
    public Future<JsonObject> getFeatures(String collectionId, Map<String, String> queryParams,
                                          Map<String, Integer> crs) {
      LOGGER.info("getFeatures");
      Promise<JsonObject> result = Promise.promise();
      
      /* TODO : Remove once spec validation is being done */
      if (!collectionId.matches(UUID_REGEX)) {
        result.fail(new OgcException(404, "Not found", "Collection not found"));
        return result.future();
      }
      
      Collector<Row, ? , Map<String, Integer>> collectorT = Collectors.toMap(row -> row.getColumnName(0),
          row -> row.getInteger("count"));
      Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
      String datetimeValue;
      FeatureQueryBuilder featureQuery = new FeatureQueryBuilder(collectionId);
      if (queryParams.containsKey("limit"))
          featureQuery.setLimit(Integer.parseInt(queryParams.get("limit")));
      if (queryParams.containsKey("bbox-crs"))
        featureQuery.setBboxCrs(String.valueOf(crs.get(queryParams.get("bbox-crs"))));
      if (queryParams.containsKey("bbox")) {
        // find storageCrs from collections_details
        String coordinates = queryParams.get("bbox");
        Future<String> sridOfStorageCrs = getSridOfStorageCrs(collectionId);
        sridOfStorageCrs
            .onSuccess(srid -> featureQuery.setBbox(coordinates, srid))
            .onFailure(fail -> result.fail(fail.getMessage()));
        if(sridOfStorageCrs.failed())
          return result.future();
      }
      //TODO: convert individual DB calls to a transaction
      datetimeValue = queryParams.getOrDefault("datetime", null);
      if (queryParams.containsKey("offset"))
        featureQuery.setOffset(Integer.parseInt(queryParams.get("offset")));
      if (queryParams.containsKey("crs"))
        featureQuery.setCrs(String.valueOf(crs.get(queryParams.get("crs"))));
      Set<String> keys =  queryParams.keySet();
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
                  if(conn1.rowCount() == 0) {
                    result.fail(new OgcException(404, "Not found", "Collection not found"));
                    return;
                  }
//                  if (conn1.value().get(0).getInteger("count") == 0) {
//                    result.fail(new OgcException(404, "Not found", "Collection not found"));
//                    return;
//                  }
                  LOGGER.debug("Count collection- {}", conn1.value().get(0).getInteger("count"));
                  if (conn1.value().get(0).getString("datetime_key") != null && datetimeValue != null ){
                    LOGGER.debug("datetimeKey: {}, datetimeValue: {}"
                        ,conn1.value().get(0).getString("datetime_key"), datetimeValue);
                    featureQuery.setDatetimeKey(conn1.value().get(0).getString("datetime_key"));
                    featureQuery.setDatetime(datetimeValue);
                  }
                  LOGGER.debug("datetime_key: {}",conn1.value().get(0).getString("datetime_key"));
                  LOGGER.debug("Sql Query- {}", featureQuery.buildSqlString());
                  LOGGER.debug("Count Query- {}", featureQuery.buildSqlString("count"));
                  JsonObject resultJson = new JsonObject();
                  conn.preparedQuery(featureQuery.buildSqlString("count"))
                      .collecting(collectorT).execute()
                      .onSuccess(count -> {
                        LOGGER.debug("Feature Count- {}",count.value().get("count"));
                        int totalCount = count.value().get("count");
                        resultJson.put("numberMatched", totalCount);
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
                                JsonArray featureJsonArr = new JsonArray(success);
                                int numReturn = featureJsonArr.size();
                                result.complete(resultJson
                                    .put("type","FeatureCollection")
                                    .put("features", featureJsonArr)
                                    .put("numberReturned", numReturn ));
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

  private Future<String> getSridOfStorageCrs(String collectionId) {
    LOGGER.info("getSridOfStorageCrs");
    Promise<String> result = Promise.promise();
    Collector<Row, ? , Map<String, Integer>> collector = Collectors.toMap(row -> row.getColumnName(0),
        row -> row.getInteger("srid"));
    client.withConnection(conn ->
        conn.preparedQuery("select srid from collections_details join crs_to_srid on collections_details.crs = " +
                " crs_to_srid.crs and collections_details.id = $1::uuid")
            .collecting(collector)
            .execute(Tuple.of(UUID.fromString(collectionId)))
            .onSuccess(success -> {
              LOGGER.debug("Srid of Storage Crs- {}",success.value().get("srid"));
              int srid = success.value().get("srid");
              result.complete(String.valueOf(srid));
            })
            .onFailure(fail -> {
              LOGGER.error("Something went wrong, {}", fail.getMessage());
              result.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
            }));
    return result.future();
  }

  public Future<Void> matchFilterWithProperties(String collectionId, Map<String, String> queryParams) {
      Promise<Void> result = Promise.promise();
      if (queryParams.isEmpty()) {
         result.complete();
         return result.future();
      }
      Set<String> keys =  queryParams.keySet();
      keys.removeAll(predefinedKeys);
      if (keys.isEmpty()) {
        result.complete();
        return result.future();
      }
      client.withConnection(conn ->
          conn.preparedQuery("select jsonb_object_keys(properties) as filter_keys from \"" + collectionId + "\" group" +
                  " by " +
                  "filter_keys")
              .execute()
              .onSuccess(success -> {
                Set<String> propertiesKeys = new HashSet<>();
                for(Row row: success){
                  propertiesKeys.add(row.getString("filter_keys"));
                }
                LOGGER.debug("properties keys: {}", propertiesKeys);
                if (propertiesKeys.contains(keys))
                  result.complete();
                else
                  result.fail(new OgcException(400, "Bad Request", "Query parameter is invalid"));
              })
              .onFailure(failed -> {
                LOGGER.debug("DB query Failed!! {}", failed.getMessage());
                result.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
              }));
      return result.future();
  }

  @Override
  public Future<Map<String, Integer>> isCrsValid(String collectionId, Map<String, String> queryParams) {
    Promise<Map<String, Integer>> result = Promise.promise();
    
    /* TODO : Remove once spec validation is being done */
    if (!collectionId.matches(UUID_REGEX)) {
      result.fail(new OgcException(404, "Not found", "Collection not found"));
      return result.future();
    }
    if (queryParams.isEmpty()) {
       result.complete(Map.of(DEFAULT_SERVER_CRS,4326));
       return result.future();
    }
    if (!queryParams.containsKey("crs") && !queryParams.containsKey("bbox-crs")) {
      result.complete(Map.of(DEFAULT_SERVER_CRS, 4326));
      return result.future();
    }
    //check for both crs and bbox-crs
    String requestCrs = queryParams.getOrDefault("crs", DEFAULT_SERVER_CRS);
    String bboxCrs = queryParams.getOrDefault("bbox-crs", DEFAULT_SERVER_CRS);
    Collector<Row, ?, Map<String, Integer>> crsCollector = Collectors.toMap(row -> row.getString("crs"),
        row -> row.getInteger("srid"));
    client.withConnection(conn ->
      conn.preparedQuery("Select crs, srid from collection_supported_crs as colcrs join crs_to_srid as crsrid on " +
              "colcrs.crs_id = crsrid.id and colcrs.collection_id = $1::uuid")
          .collecting(crsCollector)
          .execute(Tuple.of(UUID.fromString(collectionId)))
          .onSuccess(success -> {
            LOGGER.debug("CRS:SRID-\n{}",success.toString() );
            if (!success.value().containsKey(requestCrs)) {
              result.fail(new OgcException(400, "Bad Request", "Collection does not support this crs"));
            }
            if (!success.value().containsKey(bboxCrs)) {
              result.fail(new OgcException(400, "Bad Request", "Collection does not support this bbox-crs"));
            }
              result.complete(success.value());
          })
          .onFailure(failed -> {
            LOGGER.error("Error: {}", failed.getMessage());
            result.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
          })
    );
    return result.future();
  }

  @Override
  public Future<JsonObject> getFeature(String collectionId, String featureId, Map<String, String> queryParams,
                                       Map<String, Integer> crs) {
    LOGGER.info("getFeature");
    Promise<JsonObject> result = Promise.promise();

    /* TODO : Remove once spec validation is being done */
    if (!collectionId.matches(UUID_REGEX)) {
      result.fail(new OgcException(404, "Not found", "Collection not found"));
      return result.future();
    }
    
    Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
    Collector<Row, ? , Map<String, Integer>> collectorT = Collectors.toMap(row -> row.getColumnName(0)
      , row -> row.getInteger("count"));
    String srid = String.valueOf(crs.get(queryParams.getOrDefault("crs", DEFAULT_SERVER_CRS)));
    String geoColumn = "cast(st_asgeojson(st_transform(geom," + srid + "),9,0) as json)";
    client.withConnection(conn ->
        conn.preparedQuery("select count(*) from collections_details where id = $1::uuid")
            .collecting(collectorT)
            .execute(Tuple.of(UUID.fromString(collectionId)))
            .onSuccess(conn1 -> {
                if (conn1.value().get("count") == 0) {
                    result.fail(new OgcException(404, "Not found", "Collection not found"));
                    return;
                }
                String sqlQuery = "Select id, itemType as type," + geoColumn + " as geometry, " +
                    "properties from \"" + collectionId + "\" where id=$2::UUID" ;
                conn.preparedQuery(sqlQuery)
                    .collecting(collector).execute(Tuple.of(geoColumn, UUID.fromString(featureId)))
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


  @Override
  public Future<List<JsonObject>> getStacCollections() {
    Promise<List<JsonObject>> result = Promise.promise();
    Collector<Row, ?, List<JsonObject>> collector =
        Collectors.mapping(Row::toJson, Collectors.toList());
    client
        .withConnection(
            conn ->
                conn.preparedQuery(
                        "Select id, title, description, bbox, temporal,license from collections_details")
                    .collecting(collector)
                    .execute()
                    .map(SqlResult::value))
        .onSuccess(
            success -> {
              if (success.isEmpty()) {
                LOGGER.error("Collections table is empty!");
                result.fail("Error!");
              }
              else {
                LOGGER.debug("Collections Result: {}", success.toString());
                result.complete(success);
              }
            })
        .onFailure(
            fail -> {
              LOGGER.error("Failed to getCollections! - {}", fail.getMessage());
              result.fail("Error!");
            });
    return result.future();
  }

  @Override
  public Future<List<JsonObject>> getTileMatrixSets() {
    LOGGER.info("getTileMatrixSets");
    Promise<List<JsonObject>> result = Promise.promise();
    Collector<Row, ?, List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
    client.withConnection(conn ->
            conn.preparedQuery("Select id, title, uri from tilematrixsets_relation")
                .collecting(collector)
                .execute()
                .map(SqlResult::value))
        .onSuccess(success -> {
          if (success.isEmpty()) {
            LOGGER.error("TileMatrixSet_relation table is empty!");
            result.fail(new OgcException(404, "Not found", "TileMatrixSets (tiling scheme) not found"));
          } else {
            LOGGER.debug("TileMatrixSets Result: {}", success.toString());
            result.complete(success);
          }
        })
        .onFailure(fail -> {
          LOGGER.error("Failed to getTileMatrixSets(tiling scheme)! - {}", fail.getMessage());
          result.fail("Error!");
        });
    return result.future();
  }

  @Override
  public Future<List<JsonObject>> getStacCollection(String collectionId) {
    LOGGER.info("getFeature");
    Promise<List<JsonObject>> result = Promise.promise();

    /* TODO : Remove once spec validation is being done */
    if (!collectionId.matches(UUID_REGEX)) {
      result.fail(new OgcException(404, "Not found", "Collection not found"));
      return result.future();
    }

    Collector<Row, ?, List<JsonObject>> collector =
        Collectors.mapping(Row::toJson, Collectors.toList());
    client
        .withConnection(
            conn ->
                conn.preparedQuery(
                        "SELECT id, title, description, bbox, temporal, license FROM collections_details where id = $1::uuid")
                    .collecting(collector)
                    .execute(Tuple.of(UUID.fromString(collectionId)))
                    .map(SqlResult::value))
        .onSuccess(
            success -> {
              LOGGER.debug("DB result - {}", success);
              if (success.isEmpty())
                result.fail(new OgcException(404, "Not found", "Collection not found"));
              else {
                LOGGER.debug("Built OGC Collection Response - {}", success);

                result.complete(success);
              }
            })
        .onFailure(
            fail -> {
              LOGGER.error("Failed at getCollection- {}", fail.getMessage());
              result.fail("Error!");
            });
    return result.future();
  }
  public Future<List<JsonObject>> getTileMatrixSetMetaData(String tileMatrixSet) {
    LOGGER.info("getTileMatrixSetMetaData");
    Promise<List<JsonObject>> result = Promise.promise();
    Collector<Row, ?, List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
    client.withConnection(conn ->
            conn.preparedQuery("Select tmsr.id as id, tmsr.title as title, tmsr.uri as uri, pointoforigin, tilewidth," +
                    " tileheight, crs, tmsm.tilematrix_id, tmsm.title as tilematrixmeta_title, tmsm.description, " +
                    "scaledenominator, cellsize, corneroforigin, matrixwidth, matrixheight" +
                    " from tilematrixsets_relation as tmsr join tilematrixset_metadata tmsm on" +
                    " tmsr.id = tmsm.tilematrixset_id where tmsr.id = $1::text")
                .collecting(collector)
                .execute(Tuple.of(tileMatrixSet))
                .map(SqlResult::value))
        .onSuccess(success -> {
          if (success.isEmpty()) {
            LOGGER.error("TileMatrixSet_relation or TileMatrixset_metadata table is empty!");
            result.fail(new OgcException(404, "Not found", "TileMatrixSets (tiling scheme) not found"));
          } else {
            LOGGER.debug("TileMatrices Result: {}", success.toString());
            result.complete(success);
          }
        })
        .onFailure(fail -> {
          LOGGER.error("Failed to getTileMatrixSetsRelation(tiling scheme)! - {}", fail.getMessage());
          result.fail("Error!");
        });
    return result.future();
  }

  @Override
  public Future<List<JsonObject>> getTileMatrixSetRelation(String collectionId) {
    LOGGER.info("getTileMatrixSetRelation");
    Promise<List<JsonObject>> result = Promise.promise();
    Collector<Row, ?, List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
    client.withConnection(conn ->
            conn.preparedQuery("select cd.id as collection_id, cd.title as collection_title, cd.description, tmsr.crs" +
                    ", tmsr.id as tilematrixset, tmsr.title as tilematrixset_title, uri, datatype" +
                    " from collections_details as cd join tilematrixsets_relation as tmsr on cd.id = tmsr.collection_id" +
                    " where cd.id = $1::uuid")
                .collecting(collector)
                .execute(Tuple.of(collectionId))
                .map(SqlResult::value))
        .onSuccess(success -> {
          if (success.isEmpty()) {
            LOGGER.error("TileMatrixSet_relation or collections_details table is empty!");
            result.fail(new OgcException(404, "Not found", "TileSetList not found"));
          } else {
            LOGGER.debug("TileSets Result: {}", success.toString());
            result.complete(success);
          }
        })
        .onFailure(fail -> {
          LOGGER.error("Failed to getTileSetList! - {}", fail.getMessage());
          result.fail("Error!");
        });
    return result.future();

  }

  @Override
  public Future<List<JsonObject>> getTileMatrixSetRelationOverload(String collectionId, String tileMatrixSetId) {
    LOGGER.info("getTileMatrixSetRelation<collId,tileMatrixSetId>");
    Promise<List<JsonObject>> result = Promise.promise();
    Collector<Row, ?, List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
    client.withConnection(conn ->
            conn.preparedQuery("select cd.id as collection_id, cd.title as collection_title, cd.description, tmsr.crs" +
                    " as crs, tmsr.id as tilematrixset, tmsr.title as tilematrixset_title, uri, datatype" +
                    " from collections_details as cd join tilematrixsets_relation as tmsr on cd.id = tmsr.collection_id" +
                    " where cd.id = $1::uuid and tmsr.id = $2::text")
                .collecting(collector)
                .execute(Tuple.of(collectionId, tileMatrixSetId))
                .map(SqlResult::value))
        .onSuccess(success -> {
          if (success.isEmpty()) {
            LOGGER.error("TileMatrixSet_relation or collections_details table is empty!");
            result.fail(new OgcException(404, "Not found", "TileSetList not found"));
          } else {
            LOGGER.debug("TileSets Result: {}", success.toString());
            result.complete(success);
          }
        })
        .onFailure(fail -> {
          LOGGER.error("Failed to getTileSetList! - {}", fail.getMessage());
          result.fail("Error!");
        });
    return result.future();

  }
}
