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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
           conn.preparedQuery("Select id, title, description from collections_details where id = $1::uuid")
               .collecting(collector)
               .execute(Tuple.of(UUID.fromString(collectionId))).map(SqlResult::value))
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
            // TODO: pull the baseURL from the config
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
        Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
        client.withConnection(conn ->
            //TODO: here we can use limit (default or provided by the user)
                conn.preparedQuery("Select id, title, description from collections_details")
                    .collecting(collector)
                    .execute()
                    .map(SqlResult::value))
            .onSuccess(success -> {
                if (success.isEmpty()) {
                    LOGGER.error("Collections table is empty!");
                    result.fail("Error!");
                }
                else {
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
        return  result.future();
    }
}
