package ogc.rs.database.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ogc.rs.common.Constants.DEFAULT_CRS_SRID;

public class RecordQueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(RecordQueryBuilder.class);

  private String tableName;
  private StringBuilder qValueSearchConditions;
  private String bbox;
  private String queryParameters;
  private String idsParameter;
  private int limit;
  private int offset;
  private String datetimeParameter;

  public RecordQueryBuilder(String tableName) {

    this.tableName = tableName;
    queryParameters = "";
    limit = 10;
  }

  public void setLimit(String limit) {
    this.limit = Integer.parseInt(limit);
  }

  public void setOffset(String offset) {
    this.offset = Integer.parseInt(offset) - 1;
  }

  public void setIdsParamValues(String ids) {

    // Remove square brackets and whitespace
    String cleaned = ids.replaceAll("[\\[\\]\\s]", ""); // "[2, 3, 4]" → "2,3,4"
    String[] idArray = cleaned.split(",");

    // Join into SQL-safe format
    String idClause = "id IN (" + String.join(", ", idArray) + ")";

    LOGGER.debug("Generated ID WHERE clause: " + idClause);

    // Append to existing queryParameters
    if (idsParameter == null || idsParameter.isEmpty()) {
      idsParameter = "(" + idClause + ")";
    } else {
      idsParameter = idsParameter.replaceAll("\\)$", "") + " AND " + idClause + ")";
    }
  }

  public void setQueryParamValues(Map<String, String> recordQueryMap) {
    queryParameters =
        "("
            + recordQueryMap.entrySet().stream()
                .map(
                    entry -> {
                      String key = entry.getKey();
                      String value = entry.getValue().replace("'", "''"); // Escape single quotes

                      if (key.equals("id")) {
                        return key + " = " + value;
                      } else if (key.equals("created")) {
                        return key + " = TIMESTAMPTZ '" + value + "'";
                      } else if (key.equals("keywords")) {
                        String cleaned = value.replaceAll("[\\[\\]\"]", "");
                        String[] keywordsArray = cleaned.split(",");

                        String keywordArrayString =
                            Arrays.stream(keywordsArray)
                                .map(k -> "'" + k.trim().replace("'", "''") + "'")
                                .collect(Collectors.joining(", "));

                        return key
                            + " && ARRAY["
                            + keywordArrayString
                            + "]::text[]"; // use @> for match all and use && for match any
                      } else {
                        return key + " = '" + value + "'";
                      }
                    })
                .collect(Collectors.joining(" AND "))
            + ") ";
  }

  public void setQValues(String qValues) {

    qValues = qValues.replaceAll("[\\[\\]\"]", "");

    List<String> qList = Arrays.stream(qValues.split("(?<!\\\\),"))
            .map(s -> s.trim().replace("\\,", ","))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    if(qList.isEmpty())
      return;
    StringBuilder singleWord = new StringBuilder();
    StringBuilder multiWord = new StringBuilder();

    for (String word : qList) {
      if (word.contains(" ")) {
        multiWord.append(word).append(" ");
      } else {
        singleWord.append(word).append(" ");
      }
    }

    qValueSearchConditions = new StringBuilder();
    qValueSearchConditions.append("(");

    if (singleWord.length() > 0) {
      qValueSearchConditions
          .append("search_vector @@ plainto_tsquery('english', '")
          .append(singleWord.toString().trim())
          .append("')");
    }

    if (multiWord.length() > 0) {
      if (qValueSearchConditions.length() > 0) {
        qValueSearchConditions.append(" OR ");
      }
      qValueSearchConditions
          .append("search_vector @@ phraseto_tsquery('english', '")
          .append(multiWord.toString().trim())
          .append("')");
    }
    qValueSearchConditions.append(") ");

    LOGGER.debug("Search condition: " + qValueSearchConditions.toString());
  }

  public void setBbox(String coordinates) {

    coordinates = coordinates.concat(",").concat(DEFAULT_CRS_SRID.toString());

    LOGGER.debug("bbox coordinates " + coordinates);
    this.bbox = "st_intersects(geometry, st_makeenvelope(" + coordinates + ")) ";
    LOGGER.debug("bbox vale string bbox: " + bbox);
  }

  public void setDatetimeParam(String datetime) {
    String condition;

    // Handle interval (contains "/")
    if (datetime.contains("/")) {
      String[] parts = datetime.split("/", 2);
      String start = parts[0].trim();
      String end = parts[1].trim();

      if (start.equals("..") || start.isEmpty()) {
        // Only end is specified → before or equal to end
        condition = "created <= TIMESTAMPTZ '" + end + "'";
      } else if (end.equals("..") || end.isEmpty()) {
        // Only start is specified → after or equal to start
        condition = "created >= TIMESTAMPTZ '" + start + "'";
      } else {
        // Full interval
        condition = "created BETWEEN TIMESTAMPTZ '" + start + "' AND TIMESTAMPTZ '" + end + "'";
      }
    } else {
      // Single datetime value
      condition = "created = TIMESTAMPTZ '" + datetime + "'";
    }

    // Append to existing queryParameters
    if (datetimeParameter == null || datetimeParameter.isEmpty()) {
      datetimeParameter = "(" + condition + ")";
    } else {
      datetimeParameter = datetimeParameter.replaceAll("\\)$", "") + " AND " + condition + ")";
    }

    LOGGER.debug("Datetime condition: " + datetimeParameter);
  }


  public String buildItemCountSqlString() {

    String catalogTableName = "public.\"" + tableName + "\"";
    StringBuilder query =
        new StringBuilder(String.format("SELECT COUNT(id) FROM %s", catalogTableName));

    List<String> conditions = new ArrayList<>();

    if (qValueSearchConditions != null && qValueSearchConditions.length() > 0)
      conditions.add(qValueSearchConditions.toString());

    if (idsParameter != null && idsParameter.length() > 0) conditions.add(idsParameter.toString());

    if (datetimeParameter!=null && datetimeParameter.length()>0) conditions.add(datetimeParameter);


    if (bbox != null && !bbox.isEmpty()) conditions.add(bbox);

    if (queryParameters != null && !queryParameters.isEmpty()) conditions.add(queryParameters);

    if (!conditions.isEmpty()) query.append(" WHERE ").append(String.join(" AND ", conditions));

    return query.toString();
  }

  public String buildItemSearchSqlString() {
    String catalogTableName = "public.\"" + tableName + "\"";
    StringBuilder query =
        new StringBuilder(
            String.format(
                "SELECT id, ST_AsGeoJSON(geometry)::json AS geometry, created, title, description, keywords, bbox, temporal, collection_id, provider_name, provider_contacts FROM %s",
                catalogTableName));

    List<String> conditions = new ArrayList<>();

    if (offset > 0) conditions.add("id > " + offset);

    if (qValueSearchConditions != null && qValueSearchConditions.length() > 0)
      conditions.add(qValueSearchConditions.toString());

    if (idsParameter != null && idsParameter.length() > 0) conditions.add(idsParameter.toString());

    if (datetimeParameter!=null && datetimeParameter.length()>0) conditions.add(datetimeParameter);

    if (bbox != null && !bbox.isEmpty()) conditions.add(bbox);

    if (queryParameters != null && !queryParameters.isEmpty()) conditions.add(queryParameters);

    if (!conditions.isEmpty()) query.append(" WHERE ").append(String.join(" AND ", conditions));

    query.append(" ORDER BY id ASC");
    query.append(" LIMIT ").append(limit);

    return query.toString();
  }
}
