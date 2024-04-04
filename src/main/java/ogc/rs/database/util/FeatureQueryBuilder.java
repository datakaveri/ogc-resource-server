package ogc.rs.database.util;

import ogc.rs.database.DatabaseServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLOutput;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FeatureQueryBuilder {
  private String tableName;
  private int limit;
  private String bbox;
  private String datetime;
  private String filter;
  private String additionalParams;
  private String sqlString;
  private int offset;
  private String defaultCrsSrid;
  private String bboxCrs;
  private String geoColumn;
  private String datetimeKey;
  private static final Logger LOGGER = LogManager.getLogger(FeatureQueryBuilder.class);


  public FeatureQueryBuilder(String tableName) {
    this.tableName = tableName;
    limit = 10;
    offset = 0;
    bbox = "";
    datetime = "";
    filter = "";
    additionalParams = "";
    sqlString = "";
    datetimeKey = "";
    defaultCrsSrid = "4326";
    bboxCrs = "4326";
    geoColumn = "cast(st_asgeojson(st_transform(geom,4326)) as json)";
  }

  public void setLimit(int limit) {
    if(limit > 10000 || limit < 1) {
      return;
    }
    this.limit = limit;
  }
  public void setOffset(int offset) {
    if (offset > 2000000)
      return;
    this.offset = offset-1;
  }

  // bboxCrs is srid equivalent of crs-url
  public void setBboxCrs(String bboxCrs) {
    this.bboxCrs = bboxCrs;
  }

  public void setBbox(String coordinates, String storageCrs) {
    if (!bboxCrs.isEmpty() && !bboxCrs.equalsIgnoreCase(defaultCrsSrid)){
      //TODO: do bbox transformation to the specified bbox-crs parameter
      // find the storage crs and then transform accordingly
      coordinates = coordinates.concat("," + bboxCrs);
    }
    else
      coordinates = coordinates.concat(",4326");
    //TODO: validation for lat, lon values (0<=lat<=90, 0<=lon<=180);
    if (bboxCrs.equalsIgnoreCase(storageCrs))
      this.bbox = "st_intersects(geom, st_makeenvelope(" + coordinates + "))";
    else
      this.bbox = "st_intersects(geom, st_transform(st_makeenvelope(" + coordinates + "),"+ storageCrs +"))";;

    this.additionalParams = "where";

  }
  public void setCrs (String crs) {
    // st_geojson(geometry, maxdecimaldigits, options); options = 0 means no extra options
    geoColumn = "cast(st_asgeojson(st_transform(geom," + crs + "), 9,0) as json)";
  }
  public void setDatetime(String datetime) {
    if (datetimeKey.isEmpty()) {
      return;
    }
    this.additionalParams = "where";
    String datetimeFormat = "'yyyy-mm-dd\"T\"HH24:MI:SS\"Z\"'";
//    datetime query where clause -
//    to_timestamp(properties ->> 'datetimeKey', 'datetimeFormat') 'operator' 'datetime' (from request);
    String concatString =
        " to_timestamp(properties ->> '" .concat(datetimeKey).concat("',").concat(datetimeFormat).concat(") ");
    if (!datetime.contains("/")) {
      this.datetime = concatString.concat("= '").concat(datetime).concat("'");
      return;
    }
    String[] dateTimeArr = datetime.split("/");
      if (dateTimeArr[0].equals("..")) { // -- before\
      this.datetime = concatString.concat("<'").concat(dateTimeArr[1]).concat("'");
  }
    else if (dateTimeArr[1].equals("..")) { // -- after
      this.datetime = concatString.concat(">'").concat(dateTimeArr[0]).concat("'");
    }
    else {
      this.datetime = concatString.concat(" between '").concat(dateTimeArr[0]).concat("' and '")
          .concat(dateTimeArr[1]).concat("'");
    }
  }

  public void setFilter(String key, String value) {
    this.filter = "properties->>'" + key + "'='" + value + "'";
    this.additionalParams = "where";
  }

  public void setDatetimeKey(String datetimeKey) {
    this.datetimeKey = datetimeKey;
  }

  public String buildSqlString() {
    //TODO: refactor to build the sql query
    this.sqlString = String.format("select id, itemType as type, %4$s as geometry, properties" +
            " from \"%1$s\" limit %2$d offset %3$d"
        , this.tableName, this.limit, this.offset, this.geoColumn);

    if (!bbox.isEmpty()) {
      this.sqlString = String.format("select id, itemType as type, %6$s as geometry, properties" +
              " from \"%1$s\" %3$s %4$s limit %2$d offset %5$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.offset, this.geoColumn);
    }

    if(!datetime.isEmpty() ){
      this.sqlString = String.format("select id, itemType as type, %6$s as geometry, properties " +
              " from \"%1$s\" %3$s %4$s limit %2$d offset %5$d"
          ,this.tableName,this.limit, this.additionalParams, this.datetime, this.offset, this.geoColumn);
    }

    if (!filter.isEmpty()) {
      this.sqlString = String.format("select id, itemType as type, %6$s as geometry, properties" +
              " from \"%1$s\" %3$s %4$s limit %2$d offset %5$d"
          ,this.tableName,this.limit, this.additionalParams, this.filter, this.offset, this.geoColumn);
    }

    if (!bbox.isEmpty() && !filter.isEmpty()) {
      this.sqlString = String.format("select id, itemType as type, %7$s as geometry, properties" +
              " from \"%1$s\" %3$s %4$s and %5$s limit %2$d offset %6$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.filter, this.offset, this.geoColumn);
    }

    if (!bbox.isEmpty() && !datetime.isEmpty()) {
      this.sqlString = String.format("select id, itemType as type, %7$s as geometry, properties" +
              " from \"%1$s\" %3$s %4$s and %5$s limit %2$d offset %6$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.datetime, this.offset, this.geoColumn);
    }

    if (!datetime.isEmpty() && !filter.isEmpty()) {
      this.sqlString = String.format("select id, itemType as type, %7$s as geometry, properties" +
              " from \"%1$s\" %3$s %4$s and %5$s limit %2$d offset %6$d"
          ,this.tableName,this.limit, this.additionalParams, this.datetime, this.filter, this.offset, this.geoColumn);
    }

    if (!bbox.isEmpty() && !filter.isEmpty() && !datetime.isEmpty()) {
      this.sqlString = String.format("select id, itemType as type, %8$s as geometry, properties" +
              " from \"%1$s\" %3$s %4$s and %5$s and %7$s limit %2$d offset %6$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.filter, this.offset, this.datetime,
          this.geoColumn);
    }
    LOGGER.debug("<builder>Sql query- {}", sqlString);
    return sqlString;
  }

  public String buildSqlString(String isCountQuery) {
    this.sqlString = String.format("select count(id) from \"%1$s\" "
        , this.tableName);

    if (!bbox.isEmpty()) {
    this.sqlString = String.format("select count(id) from \"%1$s\" %2$s %3$s"
        ,this.tableName, this.additionalParams, this.bbox);
    }
    if(!datetime.isEmpty() ){
      this.sqlString = String.format("select count(id) from \"%1$s\" %2$s %3$s"
          ,this.tableName, this.additionalParams, this.datetime);
    }

    if (!filter.isEmpty()) {
      this.sqlString = String.format("select count(id) from \"%1$s\" %2$s %3$s"
          ,this.tableName, this.additionalParams, this.filter);
    }

    if (!bbox.isEmpty() && !filter.isEmpty()) {
      this.sqlString = String.format("select count(id) from \"%1$s\" %2$s %3$s and %4$s"
          ,this.tableName, this.additionalParams, this.bbox, this.filter);
    }

    if (!bbox.isEmpty() && !datetime.isEmpty()) {
      this.sqlString = String.format("select count(id) from \"%1$s\" %2$s %3$s and %4$s"
          ,this.tableName, this.additionalParams, this.bbox, this.datetime);
    }

    if (!datetime.isEmpty() && !filter.isEmpty()) {
      this.sqlString = String.format("select count(id) from \"%1$s\" %2$s %3$s and %4$s"
          ,this.tableName, this.additionalParams, this.datetime, this.filter);
    }

    if (!bbox.isEmpty() && !filter.isEmpty() && !datetime.isEmpty()) {
      this.sqlString = String.format("select count(id) from \"%1$s\" %2$s %3$s and %4$s and %5$s"
          ,this.tableName, this.additionalParams, this.bbox, this.filter, this.datetime);
    }
    LOGGER.debug("<builder>Count query- {}", sqlString);
    return sqlString;
  }


  public int getLimit() {
    return this.limit;
  }
}
