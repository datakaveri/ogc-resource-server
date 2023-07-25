package ogc.rs.database;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class DatabaseQueryTest {

    private static Vertx vertxObj;
    static JsonObject config;

    @BeforeAll
    public static void  setUp(Vertx vertx, VertxTestContext testContext) {
        vertxObj = vertx;
        final String TEST_CONFIG_PATH = "./secrets/configs/test.json";
        try {
            config = new JsonObject(Files.readString(Paths.get(TEST_CONFIG_PATH)));
        }catch (FileNotFoundException e) {
            testContext.failNow(new Throwable(e.getMessage()));
        } catch (IOException e) {
            testContext.failNow(new Throwable("Couldn't read the configuration file!\nDetail:", e.getCause()));
        }
        if (config.isEmpty()){
            testContext.failNow("Configuration file is empty!");
        }

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(config.getJsonObject("database").getInteger("port"))
                .setHost(config.getJsonObject("database").getString("host"))
                .setDatabase(config.getJsonObject("database").getString("db_name"))
                .setUser(config.getJsonObject("database").getString("db_user"))
                .setPassword(config.getJsonObject("database").getString("db_password"));

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(10);
        PgPool pool = PgPool.pool(vertxObj, connectOptions, poolOptions);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Get collections")
    void getCollections(VertxTestContext testContext){
        // get all collections from database
        // check if all collections are present
        // the number of collections should be 1
        // the name of the collection should be district_hq
        // results should be an array >=1 (should not be empty)
        // properties of the JsonObject can be checked later

    }

    @Test
    @DisplayName("Get one collection")
    void getCollection(VertxTestContext testContext){
        // get <collectionId> from database
        // check the name of the collection, should be district_hq
        // can check various properties
    }

}
