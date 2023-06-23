package ogc.rs.metering;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import ogc.rs.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService{
    private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);

    public MeteringServiceImpl(Vertx vertx, DatabaseService databaseService) {
    }

    @Override
    public Future<JsonObject> executeReadQuery(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> insertMeteringValuesInRmq(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> monthlyOverview(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> summaryOverview(JsonObject request) {
        return null;
    }
}
