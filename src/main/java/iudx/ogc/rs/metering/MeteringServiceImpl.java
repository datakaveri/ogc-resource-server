package iudx.ogc.rs.metering;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService{
    private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);

    @Override
    public MeteringService executeReadQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        LOGGER.info("executeReadQuery!");
        return this;
    }

    @Override
    public MeteringService insertMeteringValuesInRmq(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        LOGGER.info("insertMeteringValuesInRmq");
        return this;
    }

    @Override
    public MeteringService monthlyOverview(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        LOGGER.info("monthlyOverview!");
        return this;
    }

    @Override
    public MeteringService summaryOverview(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        LOGGER.info("summaryOverview!");
        return this;
    }
}
