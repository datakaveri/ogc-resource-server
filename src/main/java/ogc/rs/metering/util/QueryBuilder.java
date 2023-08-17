package ogc.rs.metering.util;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static ogc.rs.metering.util.MeteringConstant.*;

public class QueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
  public JsonObject buildMessageForRmq(JsonObject request) {

    if (request.getString(ORIGIN) == null) {
      String primaryKey = UUID.randomUUID().toString().replace("-", "");
      request.put(PRIMARY_KEY, primaryKey);
      String userId = request.getString(USER_ID);
      request.put(USER_ID, userId);
      request.put(ORIGIN, ORIGIN_SERVER);
    }
    LOGGER.trace("Info: Request " + request);
    return request;
  }

}
