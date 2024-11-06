package ogc.rs.apiserver.util;

import static ogc.rs.common.Constants.*;

import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Shareable;
import ogc.rs.apiserver.handlers.TilesMeteringHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.LinkOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class MeteringInfo implements Shareable {

  private static final Logger LOGGER = LogManager.getLogger(MeteringInfo.class);

  private final String userId;
  private final String api;
  private final long epochTime;
  private final String isoTime;
  private long totalResponseSize;
  private final String resourceGroup;
  private final String delegatorId;
  private final String providerId;
  private final JsonObject requestBody;
  private final String resourceId;

  public MeteringInfo(
      AuthInfo authInfo,
      String resourceGroup,
      String providerId,
      String api,
      long responseSize,
      JsonObject requestBody) {
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    this.userId = authInfo.getUserId().toString();
    this.api = api;
    this.epochTime = zst.toInstant().toEpochMilli();
    this.isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
    this.totalResponseSize = responseSize;
    this.resourceGroup = resourceGroup;
    this.delegatorId =
        authInfo.getDelegatorUserId() != null ? authInfo.getDelegatorUserId().toString() : userId;
    this.providerId = providerId;
    this.requestBody = requestBody != null ? requestBody : new JsonObject();
    this.resourceId = authInfo.getResourceId().toString();
  }

  public int getSize() {
    return (int) totalResponseSize;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(RESOURCE_GROUP, resourceGroup)
        .put(EPOCH_TIME, epochTime)
        .put(ISO_TIME, isoTime)
        .put(USER_ID, userId)
        .put(DELEGATOR_ID, delegatorId)
        .put(API, api)
        .put(RESPONSE_SIZE, totalResponseSize)
        .put(PROVIDER_ID, providerId)
        .put(REQUEST_JSON, requestBody)
        .put(ID, resourceId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MeteringInfo)) return false;
    MeteringInfo that = (MeteringInfo) o;
    return Objects.equals(userId, that.userId) && Objects.equals(api, that.api);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, api);
  }
}
