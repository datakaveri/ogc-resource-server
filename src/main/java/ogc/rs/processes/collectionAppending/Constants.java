package ogc.rs.processes.collectionAppending;

public class Constants {
    public static final String COLLECTIONS_DETAILS_SELECT_QUERY = "SELECT * FROM collections_details WHERE id = $1";
    public static final String DB_SCHEMA_CHECK_QUERY =
            "SELECT column_name FROM information_schema.columns WHERE table_name = $1;";
    public static final String DELETE_TEMP_TABLE_QUERY = "DROP TABLE IF EXISTS \"%s\"";
    public static final String MERGE_TEMP_TABLE_QUERY = "INSERT INTO \"%s\" (%s) SELECT %s FROM \"%s\"";
    public static final String STARTING_APPEND_PROCESS_MESSAGE =
            "Starting collection append process.";
    public static final String COLLECTION_EXISTS_MESSAGE =
            "Collection exists in collection_details table.";
    public static final String SCHEMA_VALIDATION_SUCCESS_MESSAGE =
            "Schema check is completed successfully.";
    public static final String APPEND_PROCESS_MESSAGE=
            "Data appended successfully into temp table";
    public static final String MERGE_TEMP_TABLE_MESSAGE =
            "Merged temp table into collection table.";
    public static final String BBOX_UPDATE_MESSAGE =
            "Updated bbox for the collection. Collection Appending has been completed successfully.";
}
