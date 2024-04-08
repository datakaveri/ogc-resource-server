package ogc.rs.database;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import static ogc.rs.common.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(DatabaseService.class);
    private MessageConsumer<JsonObject> consumer;
    private ServiceBinder binder;
    private PgConnectOptions connectOptions,meteringConnectOptions;
    private PoolOptions poolOptions;
    private PgPool pool, meteringPool;
    private String databaseIp,meteringDatabaseHost;
    private int databasePort,meteringDatabasePort;
    private String databaseName,meteringDatabaseName;
    private String databaseUserName,meteringDatabaseUserName;
    private String databasePassword,meteringDatabasePassword;
    private int poolSize;

    private DatabaseService dbService;

    @Override
    public void start() throws Exception {

        databaseIp = config().getString("databaseHost");
        databasePort = config().getInteger("databasePort");
        databaseName = config().getString("databaseName");
        databaseUserName = config().getString("databaseUser");
        databasePassword = config().getString("databasePassword");
        poolSize = config().getInteger("poolSize");

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
        this.pool = PgPool.pool(vertx, connectOptions, poolOptions);


        meteringDatabaseHost = config().getString("meteringDatabaseHost");
        meteringDatabasePort = config().getInteger("meteringDatabasePort");
        meteringDatabaseName= config().getString("meteringDatabaseName");
        meteringDatabaseUserName = config().getString("meteringDatabaseUser");
        meteringDatabasePassword = config().getString("meteringDatabasePassword");


        this.meteringConnectOptions =
                new PgConnectOptions()
                        .setPort(meteringDatabasePort)
                        .setHost(meteringDatabaseHost)
                        .setDatabase(meteringDatabaseName)
                        .setUser(meteringDatabaseUserName)
                        .setPassword(meteringDatabasePassword)
                        .setReconnectAttempts(2)
                        .setReconnectInterval(1000L);

        this.poolOptions = new PoolOptions().setMaxSize(poolSize);
        this.meteringPool = PgPool.pool(vertx, meteringConnectOptions, poolOptions);

        dbService = new DatabaseServiceImpl(this.pool,this.config(),this.meteringPool);

        binder = new ServiceBinder(vertx);
        consumer = binder.setAddress(DATABASE_SERVICE_ADDRESS).register(DatabaseService.class, dbService);
        LOGGER.info("Database verticle started.");

   }

    @Override
    public void stop() {
        binder.unregister(consumer);
    }
}
