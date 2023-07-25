package ogc.rs.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DatabaseServiceImpl implements DatabaseService{
    private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);

    private final PgPool client;

    public DatabaseServiceImpl(final PgPool pgclient) {
        this.client = pgclient;
    }

    @Override
    public Future<JsonObject> getCollection(String collectionId) {
        LOGGER.info("getCollection");
        Promise<JsonObject> result = Promise.promise();
        Collector<Row, ? , List<JsonObject>> collector = Collectors.mapping(Row::toJson, Collectors.toList());
        client.withConnection(conn ->
           conn.preparedQuery("Select * from collections_details where collection_name = $1::text")
               .collecting(collector)
               .execute(Tuple.of(collectionId)).map(SqlResult::value))
            .onSuccess(success -> {
                System.out.println("Success!!! - " + success.toString());
                if (success.isEmpty())
                    result.fail(new OgcException("NotFound", "Collection not found"));
                else
                    result.complete(success.get(0));
            })
            .onFailure(fail -> {
                LOGGER.error("Failed at getCollection- {}",fail.getMessage());
                result.fail("Error!");
            });
        return result.future();
    }
}
