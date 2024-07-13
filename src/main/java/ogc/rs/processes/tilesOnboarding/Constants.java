package ogc.rs.processes.tilesOnboarding;

public class Constants {
    public static final String START_TILES_ONBOARDING_PROCESS = "Starting Tiles Onboarding Process";
    public static final String S3_FILE_EXISTENCE_MESSAGE = "File exists in s3 bucket";
    public static final String S3_FILE_EXISTENCE_FAIL_MESSAGE = "File does not exist in S3 or is empty.";
    public static final String RESOURCE_OWNERSHIP_CHECK_MESSAGE =
            "Resource belongs to the user.";
    public static final String FEATURE_COLLECTION_MESSAGE =
            "Tile metadata onboarding is not applicable for feature collections.";
    public static final String COLLECTION_TYPE_CHECK_MESSAGE =
            "Collection type check complete: the collection type is suitable for tile metadata onboarding.";
    public static final String HANDLE_FAILURE_MESSAGE =
            "Failed to update job table status to FAILED after handler failure";
    public static final String TILES_ONBOARDING_SUCCESS_MESSAGE =
            "Tiles Onboarding process has been completed successfully";
    public static final String TILES_ONBOARDING_FAILURE_MESSAGE =
            "Tiles Onboarding process failed";
}
