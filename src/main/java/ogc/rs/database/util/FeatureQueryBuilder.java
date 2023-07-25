package ogc.rs.database.util;

import io.vertx.sqlclient.Tuple;

public class FeatureQueryBuilder {
  private String tableName;
  private int limit;
  private String bbox;
  private String datetime;
  private String filter;
  private String additionalParams;
  private String sqlString;
  private int offset;

  public FeatureQueryBuilder(String tableName) {
    this.tableName = tableName;
    limit = 10;
    offset = 0;
    bbox = "";
    datetime = "";
    filter = "";
    additionalParams = "";
    sqlString = "";
  }

  public void setLimit(int limit) {
    if(limit > 1000 || limit < 1) {
      return;
    }
    this.limit = limit;
  }
  public void setOffset(int offset) {
    if (offset > 1000)
      return;
    this.offset = offset-1;
  }

  public void setBbox(String coordinates) {
    coordinates = coordinates.concat(",4326");
    // TODO: validation for lat, lon values (0<=lat<=90, 0<=lon<=180);
    this.bbox = "st_intersects(geom, st_makeenvelope(" + coordinates + "))";
    this.additionalParams = "where";
  }

  public void setDatetime(String datetime) {
//    this.datetime = datetime;
  }

  public void setFilter(String key, String value) {
    this.filter = "properties->>'" + key + "'='" + value + "'";
    this.additionalParams = "where";
  }

  public String buildSqlString() {
    // do your sql string building stuff here
    
    this.sqlString = String.format("select id, itemType as type, cast(st_asgeojson(geom) as json) as geometry, properties" +
            " from %1$s limit %2$d offset %3$d"
        , this.tableName, this.limit, this.offset);

    if (!bbox.isEmpty()) {
      this.sqlString = String.format("select id, itemType as type, cast(st_asgeojson(geom) as json) as geometry, properties" +
              " from %1$s %3$s %4$s limit %2$d offset %5$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.offset);
    }
    if (!filter.isEmpty()) {
      this.sqlString = String.format("select id, itemType as type, cast(st_asgeojson(geom) as json) as geometry, properties" +
              " from %1$s %3$s %4$s limit %2$d offset %5$d"
          ,this.tableName,this.limit, this.additionalParams, this.filter, this.offset);
    }
    if (!bbox.isEmpty() && !filter.isEmpty()) {
      this.sqlString = String.format("select id, itemType as type, cast(st_asgeojson(geom) as json) as geometry, properties" +
              " from %1$s %3$s %4$s and %5$s limit %2$d offset %6$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.filter, this.offset);
    }

    System.out.println("<builder>Sql query- " + sqlString);
    return sqlString;
  }


}
