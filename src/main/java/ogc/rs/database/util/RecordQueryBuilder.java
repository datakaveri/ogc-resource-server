package ogc.rs.database.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RecordQueryBuilder {
    private static final Logger LOGGER = LogManager.getLogger(RecordQueryBuilder.class);

    private String tableName;
    private StringBuilder qValueSearchConditions;
    public RecordQueryBuilder(String tableName) {
        this.tableName = tableName;
    }

    public void setQValues(String qValues){
        List<String> qValuesList = Arrays.stream(
                        qValues.substring(0, qValues.length())
                                .split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        StringBuilder singleWord = new StringBuilder();
        StringBuilder multiWord = new StringBuilder();

        for (String word : qValuesList) {
            if (word.contains(" ")) {
                multiWord.append(word).append(" ");
            } else {
                singleWord.append(word).append(" ");
            }
        }


        qValueSearchConditions = new StringBuilder();

        if (singleWord.length() > 0) {
            qValueSearchConditions.append("search_vector @@ plainto_tsquery('english', '")
                    .append(singleWord.toString().trim())
                    .append("')");
        }

        if (multiWord.length() > 0) {
            if (qValueSearchConditions.length() > 0) {
                qValueSearchConditions.append(" OR ");
            }
            qValueSearchConditions.append("search_vector @@ phraseto_tsquery('english', '")
                    .append(multiWord.toString().trim())
                    .append("')");
        }

        LOGGER.debug("Search condition: " + qValueSearchConditions.toString());
    }

    public String buildItemSearchSqlString()
    {

        String catalogTableName = "public.\"" + tableName + "\"";
        StringBuilder query = new StringBuilder(String.format(
                "SELECT id, ST_AsGeoJSON(geometry)::json AS geometry, created, title, description, keywords, bbox, temporal, collection_id, provider_name, provider_contacts FROM %s",
                catalogTableName
        ));

        if (qValueSearchConditions != null && qValueSearchConditions.length() > 0) {
            query.append(" WHERE ").append(qValueSearchConditions);
        }
        return query.toString();
    }
}
