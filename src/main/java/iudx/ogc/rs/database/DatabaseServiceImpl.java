package iudx.ogc.rs.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseServiceImpl implements DatabaseService{
    private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);

    private final PgPool client;

    public DatabaseServiceImpl(final PgPool pgclient) {
        this.client = pgclient;
    }

    @Override
    public DatabaseService executeQuery(String query, Handler<AsyncResult<JsonObject>> handler) {
        LOGGER.info("executeQuery");
        return this;
    }
}
