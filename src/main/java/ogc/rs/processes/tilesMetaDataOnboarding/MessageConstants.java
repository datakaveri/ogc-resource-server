package ogc.rs.processes.tilesMetaDataOnboarding;

public class MessageConstants {
    public static final String START_TILES_METADATA_ONBOARDING_PROCESS = "Starting Tiles Meta Data Onboarding Process";
    public static final String S3_FILE_EXISTENCE_MESSAGE = "File exists in s3 bucket";
    public static final String S3_FILE_EXISTENCE_FAIL_MESSAGE = "File does not exist in S3";
    public static final String S3_EMPTY_FILE_MESSAGE = "File in S3 is empty!";
    public static final String RESOURCE_OWNERSHIP_CHECK_MESSAGE =
            "Resource belongs to the user.";
    public static final String INVALID_ENCODING_FORMAT_MESSAGE =
            "Invalid Encoding Type";
    public static final String ENCODING_FORMAT_CHECK_MESSAGE =
            "Encoding format check complete: The encoding format is suitable for tile metadata onboarding.";
    public static final String COLLECTION_EVALUATION_MESSAGE =
            "Collection evaluation completed successfully. Collection is either a pure tile collection or a feature + tile collection.";
    public static final String COLLECTION_EXISTS_MESSAGE =
            "Collection already exists as a map or vector";
    public static final String UNKNOWN_COLLECTION_TYPE =
            "Unknown collection type";
    public static final String COLLECTION_TYPE_NOT_FOUND_MESSAGE =
            "No type found for the collection";
    public static final String COLLECTION_EXISTENCE_CHECK_FAILURE_MESSAGE =
            "Failed to check collection existence";
    public static final String TILE_MATRIX_SET_FOUND_MESSAGE =
            "Tile matrix set exists in tms_metadata table";
    public static final String TILE_MATRIX_SET_NOT_FOUND_MESSAGE =
            "Tile matrix set does not exist in tms_metadata table.";
    public static final String TILE_MATRIX_SET_CHECK_FAILURE_MESSAGE =
            "Failed to check tile matrix set";
    public static final String HANDLE_FAILURE_MESSAGE =
            "Failed to update job table status to FAILED after handler failure";
    public static final String TILES_METADATA_ONBOARDING_SUCCESS_MESSAGE =
            "Tiles Meta Data Onboarding process has been completed successfully";
    public static final String TILES_METADATA_ONBOARDING_FAILURE_MESSAGE =
            "Tiles Meta Data Onboarding process failed";
}
