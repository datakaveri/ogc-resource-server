package ogc.rs.processes.tilesMetaDataOnboarding;

public class Constants {
    // Query to check if the collection exists in the collection_details table
    public static final String CHECK_COLLECTION_EXISTENCE_QUERY =
            "SELECT EXISTS (SELECT 1 FROM collection_details WHERE collection_id = $1)";
    // Query to get the collection type from the collection_type table
    public static String GET_COLLECTION_TYPE_QUERY =
            "SELECT type FROM collection_type WHERE collection_id = $1";
    // Query to check if the tile matrix set exists in tms_metadata table and retrieve all columns
    public static final String CHECK_TILE_MATRIX_SET_EXISTENCE_QUERY =
            "SELECT *, EXISTS (SELECT 1 FROM tms_metadata WHERE title = $1) AS exists FROM tms_metadata WHERE title = $1";
    public static final String START_TILES_ONBOARDING_PROCESS = "Starting Tiles Onboarding Process";
    public static final String S3_FILE_EXISTENCE_MESSAGE = "File exists in s3 bucket";
    public static final String S3_FILE_EXISTENCE_FAIL_MESSAGE = "File does not exist in S3 or is empty.";
    public static final String RESOURCE_OWNERSHIP_CHECK_MESSAGE =
            "Resource belongs to the user.";
    public static final String FEATURE_COLLECTION_MESSAGE =
            "Tile metadata onboarding is not applicable for feature collections.";
    public static final String COLLECTION_TYPE_CHECK_MESSAGE =
            "Collection type check complete: the collection type is suitable for tile metadata onboarding.";
    public static final String COLLECTION_EXISTENCE_CHECK_MESSAGE =
            "Collection existence check completed successfully. Collection is either a pure tile collection or a feature + tile collection.";
    public static final String COLLECTION_EXISTS_MESSAGE =
            "Collection already exists as a map or vector";
    public static final String UNKNOWN_COLLECTION_TYPE =
            "Unknown collection type";
    public static final String COLLECTION_TYPE_NOT_FOUND_MESSAGE =
            "No type found for the collection";
    public static final String COLLECTION_EXISTENCE_CHECK_FAILURE_MESSAGE =
            "Failed to check collection existence";
    public static final String TILE_MATRIX_SET_FOUND_MESSAGE =
            "Tile matrix set exists in tms_metadata table.";
    public static final String TILE_MATRIX_SET_NOT_FOUND_MESSAGE =
            "Tile matrix set does not exist in tms_metadata table.";
    public static final String TILE_MATRIX_SET_CHECK_FAILURE_MESSAGE =
            "Failed to check tile matrix set";
    public static final String HANDLE_FAILURE_MESSAGE =
            "Failed to update job table status to FAILED after handler failure";
    public static final String TILES_ONBOARDING_SUCCESS_MESSAGE =
            "Tiles Onboarding process has been completed successfully";
    public static final String TILES_ONBOARDING_FAILURE_MESSAGE =
            "Tiles Onboarding process failed";
}
