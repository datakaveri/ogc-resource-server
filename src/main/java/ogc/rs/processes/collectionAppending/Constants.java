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
    public static final String COLLECTION_NOT_FOUND_MESSAGE = "Collection is not found";
    public static final String COLLECTION_EXISTENCE_FAIL_CHECK =
            "Failed to check collection existence in db.";
    public static final String SCHEMA_VALIDATION_SUCCESS_MESSAGE =
            "Schema check is completed successfully.";
    public static final String SCHEMA_VALIDATION_FAILURE_MESSAGE =
            "Schema check is failed ";
    public static final String OGR_INFO_FAILED_MESSAGE =
            "ogrinfo execution failed ";
    public static final String OGR2_OGR_FAILED_MESSAGE =
            "ogr2ogr execution failed ";
    public static final String APPEND_PROCESS_MESSAGE =
            "Data appended successfully into temp table";
    public static final String MERGE_TEMP_TABLE_MESSAGE =
            "Merged temp table into collection table successfully.";
    public static final String MERGE_TEMP_TABLE_FAILURE_MESSAGE =
            "Failed to merge temp table into main table.";
    public static final String DELETE_TEMP_TABLE_SUCCESS_MESSAGE =
            "Temporary table deleted successfully. ";
    public static final String DELETE_TEMP_TABLE_FAILURE_MESSAGE =
            "Failed to delete temporary table";
    public static final String BBOX_UPDATE_MESSAGE =
            "Updated bbox for the collection.";
    public static final String HANDLE_FAILURE_MESSAGE =
            "Failed to update job table status to FAILED after handler failure";
    public static final String APPEND_SUCCESS_MESSAGE=
            "Collection Appending process has been completed successfully.";
    public static final String APPEND_FAILURE_MESSAGE=
            "Collection Appending process failed.";
}
