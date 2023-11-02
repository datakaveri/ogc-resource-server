package ogc.rs.metering.util;

public class MeteringConstant {
    public static final String ORIGIN = "origin";
    public static final String ORIGIN_SERVER = "ogc-server";
    public static final String PRIMARY_KEY = "primaryKey";
    public static final String USER_ID = "userid";
    public static final String EXCHANGE_NAME = "ogc-auditing";
    public static final String ROUTING_KEY = "#";
    public static final String TIME_RELATION = "timeRelation";
    public static final String DURING = "during";
    public static final String BETWEEN = "between";
    public static final String ERROR = "Error";
    public static final String INVALID_DATE_DIFFERENCE =
            "Difference between dates cannot be less than 1 Minute.";
    public static final String TIME_RELATION_NOT_FOUND = "Time relation not found.";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String TIME_NOT_FOUND = "Time interval not found.";
    public static final String USERID_NOT_FOUND = "User Id not found.";
    public static final String INVALID_DATE_TIME = "invalid date-time";
    public static final String OGC_DATABASE_TABLE_NAME = "auditing_ogc";
    public static final String TABLE_NAME = "tableName";
    public static final String RESOURCE_ID = "resourceId";
    public static final String CONSUMER_ID = "consumerID";
    public static final String PROVIDERID_TIME_INTERVAL_COUNT_QUERY =
            "SELECT count(*) FROM $0 where time between '$1' and '$2' and providerid='$3'";
    public static final String CONSUMERID_TIME_INTERVAL_COUNT_QUERY =
            "SELECT count(*) FROM $0 where time between '$1' and '$2' and userid='$3'";
    public static final String TOTALHITS = "totalHits";
    public static final String LIMITPARAM = "limit";
    public static final String OFFSETPARAM = "offset";
    public static final String API_QUERY = " and api='$4' ";
    public static final String RESOURCEID_QUERY = " and resourceid = '$5' ";
    public static final String USER_ID_QUERY = " and userid='$6' ";
    public static final String PROVIDERID_TIME_INTERVAL_READ_QUERY =
            "SELECT * FROM $0 where time between '$1' and '$2' and providerid='$3'";
    public static final String CONSUMERID_TIME_INTERVAL_READ_QUERY =
            "SELECT * FROM $0 where time between '$1' and '$2' and userid='$3'";
    public static final String ORDER_BY = " ORDER BY time";
    public static final String OFFSET_QUERY = " offset $8";
    public static final String LIMIT_QUERY = " limit $7";
    public static final String SUCCESS = "Success";
    public static final String ENDPOINT = "endPoint";
    public static final String ROLE = "role";
    public static final String IID = "iid";
    public static final String STARTT = "starttime";
    public static final String ENDT = "endtime";
    public static final String OVERVIEW_QUERY =
            "SELECT month,year,COALESCE(counts, 0) as counts\n"
                    + "FROM  (\n"
                    + "   SELECT day::date ,to_char(date_trunc('month', day),'FMmonth') as month"
                    + ",extract('year' from day) as year\n"
                    + "   FROM   generate_series(timestamp '$0'\n"
                    + "                        , timestamp '$1'\n"
                    + "                        , interval  '1 month') day\n"
                    + "   ) d\n"
                    + "LEFT  JOIN (\n"
                    + "   SELECT date_trunc('month', time)::date AS day\n"
                    + "        , count(api) as counts \n"
                    + "   FROM   auditing_ogc\n"
                    + "   WHERE  time between '$2'\n"
                    + "   AND '$3'\n";
    public static final String GROUPBY =
            "\n" + "   GROUP  BY 1\n" + "   ) t USING (day)\n" + "ORDER  BY day";
    public static final String COLLECTION_DETAILS_QUERY = "select * from collections_details where id = '$1'";
    public static final String SUMMARY_QUERY_FOR_METERING =
            "select resourceid,count(*) from auditing_ogc ";
    public static final String USERID_SUMMARY = " and userid = '$9' ";
    public static final String USERID_SUMMARY_WITHOUT_TIME = " userid = '$9' ";
    public static final String PROVIDERID_SUMMARY = " and providerid = '$8' ";
    public static final String PROVIDERID_SUMMARY_WITHOUT_TIME = " providerid = '$8' ";
    public static final String GROUPBY_RESOURCEID = " group by resourceid";
    public static String CAT_SEARCH_PATH = "/search";
}
