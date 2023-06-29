package ogc.rs.database;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class DatabaseQueryTest {
    @BeforeAll
    public static void  setUp(VertxTestContext vertxTestContext) {

        // Now we have an address and port for Postgresql, no matter where it is running
        int port = 8080;
        String host = "localhost";
        String db = "random-database";
        String user = "random-user";
        String password = "guess-what";

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(port)
                .setHost(host)
                .setDatabase(db)
                .setUser(user)
                .setPassword(password);

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(10);

        Vertx vertxObj = Vertx.vertx();

        PgPool pool = PgPool.pool(vertxObj, connectOptions, poolOptions);

        vertxTestContext.completeNow();
    }
}
