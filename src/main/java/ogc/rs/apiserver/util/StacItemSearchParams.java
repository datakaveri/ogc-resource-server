package ogc.rs.apiserver.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.validation.RequestParameters;

@DataObject
@JsonGen
/**
 * Object to hold different query parameters used in STAC Item Search.
 */
public class StacItemSearchParams {

  private List<String> collections = new ArrayList<String>();
  private List<String> ids = new ArrayList<String>();
  private List<Float> bbox = new ArrayList<Float>();
  private JsonObject intersects;
  private Integer limit;
  private Integer offset;
  private String datetime;

  /**
   * Create {@link StacItemSearchParams} object from {@link RequestParameters} object. The
   * <code>query</code> JSON object in the JSON representation of the {@link RequestParameters}
   * object is used.
   * 
   * @param params
   * @return {@link StacItemSearchParams}
   * @throws OgcException in case of expected errors like both {@link StacItemSearchParams#bbox} and
   *         {@link StacItemSearchParams#intersects} supplied
   */
  public static StacItemSearchParams createFromGetRequest(RequestParameters params)
      throws OgcException {

    if (params.queryParameter("datetime") != null) {
      validateDatetime(params.queryParameter("datetime").getString());
    }

    if (params.queryParameter("intersects") != null && params.queryParameter("bbox") != null) {
      throw new OgcException(400, "Bad Request",
          "Cannot have both bbox and intersects in STAC Item Search");
    }

    return new StacItemSearchParams(params.toJson().getJsonObject("query"));
  }

  public void setCollections(List<String> collections) {
    this.collections = collections;
  }

  public void setIds(List<String> items) {
    this.ids = items;
  }

  public void setBbox(List<Float> bbox) {
    this.bbox = bbox;
  }

  public void setIntersects(JsonObject intersectsGeoJson) {
    this.intersects = intersectsGeoJson;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public void setDatetime(String datetimeInterval) {
    this.datetime = datetimeInterval;
  }

  public List<String> getCollections() {
    return new ArrayList<String>(collections);
  }

  public List<String> getIds() {
    return new ArrayList<String>(ids);
  }

  public List<Float> getBbox() {
    return new ArrayList<Float>(bbox);
  }

  public JsonObject getIntersects() {
    return intersects;
  }

  public Integer getLimit() {
    return limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public String getDatetime() {
    return datetime;
  }

  /**
   * Validate datetime parameter.
   * 
   * @param datetime
   */
  private static void validateDatetime(String datetime) {

    try {
      ZonedDateTime zone, zone2;
      DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
      if (!datetime.contains("/")) {
        zone = ZonedDateTime.parse(datetime, formatter);
      } else if (datetime.contains("/")) {
        String[] dateTimeArr = datetime.split("/");
        if (dateTimeArr[0].equals("..")) { // -- before
          zone = ZonedDateTime.parse(dateTimeArr[1], formatter);
        } else if (dateTimeArr[1].equals("..")) { // -- after
          zone = ZonedDateTime.parse(dateTimeArr[0], formatter);
        } else {
          zone = ZonedDateTime.parse(dateTimeArr[0], formatter);
          zone2 = ZonedDateTime.parse(dateTimeArr[1], formatter);
          if (zone2.isBefore(zone)) {
            throw new OgcException(400, "Bad Request",
                "After time cannot be lesser " + "than Before time");
          }
        }
      }
    } catch (DateTimeParseException e) {
      throw new OgcException(400, "Bad Request", "Time parameter not in ISO format");
    }
  }

  public StacItemSearchParams(JsonObject json) {
    StacItemSearchParamsConverter.fromJson(json, this);
  }


  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    StacItemSearchParamsConverter.toJson(this, json);
    return json;
  }
}
