package ogc.rs.database.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.common.Constants.DEFAULT_CRS_SRID;

public class FeatureQueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(FeatureQueryBuilder.class);

  private String tableName;
  private String[] tableNames;
  private int limit;
  private String bbox;
  private String datetime;
  private String filter;
  private String additionalParams;
  private String sqlString;
  private int offset;
  private String defaultCrsSrid;
  private String bboxCrsSrid;
  private String geoColumn;
  private String datetimeKey;
  private String itemIds;
  private String geometry;

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
    defaultCrsSrid = String.valueOf(DEFAULT_CRS_SRID);
    bboxCrsSrid = "";
    geoColumn = "cast(st_asgeojson(st_transform(geom," + defaultCrsSrid + ")) as json)";
  }

  // STAC
  public FeatureQueryBuilder(String[] tableNames) {
    this.tableNames = tableNames;
    bbox = "";
    datetime = "";
    additionalParams = "";
    sqlString = "";
    datetimeKey = "datetime";
    defaultCrsSrid = String.valueOf(DEFAULT_CRS_SRID);
    bboxCrsSrid = "";
    geoColumn = "cast(st_asgeojson(geom) as json)";
    itemIds = "";
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }
  public void setOffset(int offset) {
    this.offset = offset-1;
  }

  // bboxCrsSrid is srid equivalent of crs-url
  public void setBboxCrsSrid(String bboxCrsSrid) {
    this.bboxCrsSrid = bboxCrsSrid;
  }

  public void setBbox(String coordinates, String storageCrs) {
    if (!bboxCrsSrid.isEmpty() && !bboxCrsSrid.equalsIgnoreCase(defaultCrsSrid))
      coordinates = coordinates.concat(",").concat(bboxCrsSrid);
    else
      coordinates = coordinates.concat(",").concat(defaultCrsSrid);

    if (bboxCrsSrid.equalsIgnoreCase(storageCrs))
      this.bbox = "st_intersects(geom, st_makeenvelope(" + coordinates + "))";
    else
      this.bbox = "st_intersects(geom, st_transform(st_makeenvelope(" + coordinates + "),"+ storageCrs +"))";;

    this.additionalParams = "where";

  }
  public void setCrs (String crs) {
    // st_asgeojson(geometry, maxdecimaldigits, options); options = 0 means no extra options
    geoColumn = "cast(st_asgeojson(st_transform(geom," + crs + "), 9,0) as json)";
  }
  public void setDatetime(String datetime) {
    if (datetimeKey.isEmpty()) {
      return;
    }
    this.additionalParams = "where";
    
    String datetimeFormat = "'yyyy-mm-dd\"T\"HH24:MI:SS\"Z\"'";
    
    // to_timestamp('datetimeKey', 'datetimeFormat') 'operator' 'datetime' (from request);
    String concatString =
        " to_timestamp(" .concat(datetimeKey).concat(",").concat(datetimeFormat).concat(") ");
    
    if (!datetime.contains("/")) {
      this.datetime = concatString.concat("= '").concat(datetime).concat("'");
      return;
    }
    String[] dateTimeArr = datetime.split("/");
      if (dateTimeArr[0].equals("..")) { // -- before
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
    this.filter = "\"" + key + "\"='" + value + "'";
    this.additionalParams = "where";
  }

  public void setDatetimeKey(String datetimeKey) {
    this.datetimeKey = datetimeKey;
  }

  public void setItemIds(String itemIds) {
    this.itemIds = " and id = in (" + itemIds + ")";
  }

  public void setGeometryIntersects(String geometry) {
    // this is a geojson geometry
    this.geometry = "st_intersects(geom, st_geomfromgeojson(" + geometry +"))";
  }

  public String buildSqlString() {
    //TODO: refactor to build the sql query
    this.sqlString = String.format("select id, 'Feature' as type, %4$s as geometry, (row_to_json(\"%1$s\")::jsonb - " +
            " 'id' - 'geom') as properties from \"%1$s\" where id > %3$d ORDER BY id limit %2$d"
        , this.tableName, this.limit, this.offset, this.geoColumn);

    if (!bbox.isEmpty()) {
      this.sqlString = String.format("select id, 'Feature' as type, %6$s as geometry, (row_to_json(\"%1$s\")::jsonb - " +
              " 'id' - 'geom') as properties from \"%1$s\" %3$s %4$s and id > %5$d ORDER BY id limit %2$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.offset, this.geoColumn);
    }

    if(!datetime.isEmpty() ){
      this.sqlString = String.format("select id, 'Feature' as type, %6$s as geometry, (row_to_json(\"%1$s\")::jsonb - " +
              "'id' - 'geom') as properties from \"%1$s\" %3$s %4$s and id > %5$d ORDER BY id limit %2$d"
          ,this.tableName,this.limit, this.additionalParams, this.datetime, this.offset, this.geoColumn);
    }

    if (!filter.isEmpty()) {
      this.sqlString = String.format("select id, 'Feature' as type, %6$s as geometry, (row_to_json(\"%1$s\")::jsonb - " +
              " 'id' - 'geom') as properties from \"%1$s\" %3$s %4$s and id > %5$d ORDER BY id limit %2$d"
          ,this.tableName,this.limit, this.additionalParams, this.filter, this.offset, this.geoColumn);
    }

    if (!bbox.isEmpty() && !filter.isEmpty()) {
      this.sqlString = String.format("select id, 'Feature' as type, %7$s as geometry, (row_to_json(\"%1$s\")::jsonb - " +
              "'id' - 'geom') as properties from \"%1$s\" %3$s %4$s and %5$s and id > %6$d ORDER BY id limit %2$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.filter, this.offset, this.geoColumn);
    }

    if (!bbox.isEmpty() && !datetime.isEmpty()) {
      this.sqlString = String.format("select id, 'Feature' as type, %7$s as geometry, (row_to_json(\"%1$s\")::jsonb - " +
              " 'id' - 'geom') as properties from \"%1$s\" %3$s %4$s and %5$s and %6$d ORDER BY id limit %2$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.datetime, this.offset, this.geoColumn);
    }

    if (!datetime.isEmpty() && !filter.isEmpty()) {
      this.sqlString = String.format("select id, 'Feature' as type, %7$s as geometry, (row_to_json(\"%1$s\")::jsonb - " +
              " 'id' - 'geom') as properties from \"%1$s\" %3$s %4$s and %5$s and id > %6$d ORDER BY id limit %2$d"
          ,this.tableName,this.limit, this.additionalParams, this.datetime, this.filter, this.offset, this.geoColumn);
    }

    if (!bbox.isEmpty() && !filter.isEmpty() && !datetime.isEmpty()) {
      this.sqlString = String.format("select id, 'Feature' as type, %8$s as geometry, (row_to_json(\"%1$s\")::jsonb - " +
              " 'id' - 'geom') as properties from \"%1$s\" %3$s %4$s and %5$s and %7$s and id > %6$d ORDER BY id limit %2$d"
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
  public String buildItemSearchSqlString() {

    if (!geometry.isEmpty())
     this.bbox ="";

    int i = 0;
    StringBuilder selectStatementUnion = new StringBuilder();
    String selectStatement = " select id, 'Feature' as type, %1$s as geometry, properties" +
       " '_tablename_' as tablename, p_id from \"_tablename_\" ";

    while (i < tableNames.length) {
     String sql = selectStatement.replace("_tablename_", tableNames[i]);
     if (i == tableNames.length-1) {
       selectStatementUnion.append(sql);
     }
     else {
       selectStatementUnion.append(sql).append(" union ");
     }
     i++;
    }

    LOGGER.debug("ItemSearch SQL String (concat)" + selectStatementUnion);

    this.sqlString = String.format(String.valueOf(selectStatementUnion.append(" where p_id > %2$d %4$s ORDER BY p_id, " +
           " tablename limit %3$d"))
     , this.geoColumn, this.offset, this.limit, this.itemIds);

    if (!bbox.isEmpty()) {
     this.sqlString = String.format(String.valueOf(selectStatementUnion.append(" where %4$s and p_id > %2$d ORDER BY " +
             "p_id, tablename limit %3$d"))
         , this.geoColumn,this.offset, this.limit, this.bbox);
    }

    if(!datetime.isEmpty() ){
     this.sqlString = String.format(String.valueOf(selectStatementUnion.append(" where %4$s and p_id > %2$d %5$s" +
             " ORDER BY p_id, tablename limit %3$d"))
         , this.geoColumn, this.offset, this.limit, this.datetime, this.itemIds);
    }

    if(!geometry.isEmpty()) {
     this.sqlString = String.format(String.valueOf(selectStatementUnion.append(" where %4$s and p_id > %2$d %5$s" +
             " ORDER BY p_id, tablename limit %3$d"))
         , this.geoColumn, this.offset, this.limit, this.geometry, this.itemIds);
    }

    if (!bbox.isEmpty() && !datetime.isEmpty()) {
     this.sqlString = String.format(String.valueOf(selectStatementUnion.append(" where %4$s and %5$s and p_id > %2$d" +
             " %6$s ORDER BY p_id, tablename limit %3$d"))
         , this.geoColumn, this.offset, this.limit, this.bbox, this.datetime, this.itemIds);
    }

    if (!geometry.isEmpty() && !datetime.isEmpty()) {
     this.sqlString = String.format(String.valueOf(selectStatementUnion.append(" where %4$s and %5$s and p_id > %2$d" +
             " %6$s ORDER BY p_id, tablename limit %3$d"))
         , this.geoColumn, this.offset, this.limit, this.geometry, this.datetime, this.itemIds);
    }

    LOGGER.debug("<builder>Sql query- {}", sqlString);
    return sqlString;
  }


}
