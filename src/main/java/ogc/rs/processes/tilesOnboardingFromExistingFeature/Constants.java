package ogc.rs.processes.tilesOnboardingFromExistingFeature;

public class Constants {
    //Message Constants
    public static final String START_TILES_ONBOARDING_PROCESS = "Starting tiles onboarding from existing features process";
    public static final String RESOURCE_OWNERSHIP_CHECK_MESSAGE = "Resource belongs to the user.";
    public static final String RESOURCE_NOT_ONBOARDED_MESSAGE = "Resource is not onboarded as a feature at first place. In order to proceed with the tiles onboarding process, onboard the collection as a feature first!";
    public static final String TILES_ALREADY_ONBOARDED_MESSAGE = "Collection has additional types; it must be only 'FEATURE' to proceed with the process. Tiles onboarding might already be done!";
    public static final String FEATURE_EXISTS_MESSAGE = "Collection is onboarded as a feature. Proceeding with tiles onboarding...";
    public static final String TILE_MATRIX_SET_EXISTS_MESSAGE = "Tile matrix set exists in tms_metadata table";
    public static final String TILES_ONBOARDING_SUCCESS_MESSAGE = "Successfully onboarded tiles from the existing feature collection";
    public static final String PROCESS_SUCCESS_MESSAGE = "Tiles Onboarding Process completed successfully!";
    public static final String PROCESS_FAILURE_MESSAGE = "Tiles Onboarding process failed!";
    public static final String HANDLE_FAILURE_MESSAGE = "Failed to update job table status to FAILED after handler failure";
    public static final String OGR_2_OGR_FAILED = "Failed to onboard tiles using OGR2OGR.";

    //SQL Constants
    public static final String COLLECTION_TYPE_SELECT_QUERY = "SELECT type from collection_type where collection_id = $1";

}
