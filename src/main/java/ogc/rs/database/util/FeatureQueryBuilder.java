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

  public FeatureQueryBuilder(String tableName) {
    this.tableName = tableName;
    limit = 10;
    bbox = "";
    datetime = "";
    filter = "";
    additionalParams = "";
    sqlString = "";
  }

  public FeatureQueryBuilder setLimit(int limit) {
    if(limit > 1000 || limit < 1) {
      return this;
    }
    this.limit = limit;
    return this;
  }

  public void setBbox(String coordinates) {
    coordinates = coordinates.concat(",4326");
    // TODO: validation for lat, lon values (0<=lat<=90, 0<=lon<=180); also order(lat,lon or lon,lat)
    this.bbox = "st_intersects(geogc, st_makeenvelope(" + coordinates + "))";
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
    if (this.bbox.isEmpty() && this.datetime.isEmpty() && this.filter.isEmpty()
        && this.additionalParams.isEmpty()) {
      this.sqlString = String.format("select itemType as type, st_asgeojson(geogc) as geometry, properties" +
        " from %1$s limit %2$d"
        , this.tableName, this.limit);
    }
    if (!bbox.isEmpty()) {
      this.sqlString = String.format("select itemType as type, st_asgeojson(geogc) as geometry, properties" +
              " from %1$s %3$s %4$s limit %2$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox);
    }
    if (!filter.isEmpty()) {
      this.sqlString = String.format("select itemType as type, st_asgeojson(geogc) as geometry, properties" +
              " from %1$s %3$s %4$s limit %2$d"
          ,this.tableName,this.limit, this.additionalParams, this.filter);
    }
    if (!bbox.isEmpty() && !filter.isEmpty()) {
      this.sqlString = String.format("select itemType as type, st_asgeojson(geogc) as geometry, properties" +
              " from %1$s %3$s %4$s and %5$s limit %2$d"
          ,this.tableName,this.limit, this.additionalParams, this.bbox, this.filter);
    }

    System.out.println("<builder>Sql query- " + sqlString);
    return sqlString;
  }

}
