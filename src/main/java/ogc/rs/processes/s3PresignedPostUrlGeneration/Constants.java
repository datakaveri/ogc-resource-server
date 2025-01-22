package ogc.rs.processes.s3PresignedPostUrlGeneration;

public class Constants {
    // SQL query to check if the resource is already onboarded as stac
    public static final String COLLECTION_TYPE_QUERY = "SELECT COUNT(*) FROM collection_type WHERE collection_id = $1 AND type = 'STAC'";
    // SQL query to check for a row with the specific collection_id and item_id
    public static final String ITEM_EXISTENCE_CHECK_QUERY = "SELECT COUNT(*) FROM stac_collections_part WHERE collection_id = $1 AND id = $2";

    //Message Constants
    public static final String MESSAGE = "message";
    public static final String PROCESS_START_MESSAGE =
            "Initiating the pre-signed POST URL generation process.";
    public static final String RESOURCE_OWNERSHIP_CHECK_MESSAGE =
            "Resource ownership verification successful.";
    public static final String STAC_RESOURCE_ONBOARDED_MESSAGE =
            "The resource is onboarded as a STAC collection.";
    public static final String RESOURCE_NOT_ONBOARDED_MESSAGE =
            "The resource is not onboarded as a STAC collection. Unable to proceed with the onboarding.";
    public static final String ITEM_EXISTS_MESSAGE =
            "The specified item exists and is associated with the resource.";
    public static final String ITEM_NOT_EXISTS_MESSAGE =
            "The specified item does not exist for the given resource.";
    public static final String SKIP_ITEM_EXISTENCE_CHECK_MESSAGE =
            "No item ID provided. Skipping item existence verification.";
    public static final String S3_PRE_SIGNED_POST_URL_GENERATOR_FAILURE_MESSAGE =
            "Failed to generate pre-signed URL";
    public static final String PROCESS_COMPLETE_MESSAGE =
            "Pre-Signed POST URL generation process completed successfully.";
    public static final String PROCESS_FAILURE_MESSAGE =
            "Pre-Signed POST URL generation process failed.";

}
