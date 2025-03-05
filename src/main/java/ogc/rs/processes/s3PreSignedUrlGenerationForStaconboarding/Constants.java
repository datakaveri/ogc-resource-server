package ogc.rs.processes.s3PreSignedUrlGenerationForStaconboarding;

public class Constants {
    // SQL query to check if the resource is already onboarded as stac
    public static final String COLLECTION_TYPE_QUERY = "SELECT COUNT(*) FROM collection_type WHERE collection_id = $1 AND type = 'STAC'";
    // SQL query to check for a row with the specific collection_id and item_id
    public static final String ITEM_EXISTENCE_CHECK_QUERY = "SELECT COUNT(*) FROM stac_collections_part WHERE collection_id = $1 AND id = $2";

    //Message Constants
    public static final String MESSAGE = "message";
    public static final String S3_PRESIGNED_URL_PROCESS_START_MESSAGE =
            "Initiating the pre-signed URL generation process for stac onboarding.";
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
    public static final String OBJECT_DOES_NOT_EXIST_MESSAGE =
            "Object not found in S3. Proceeding with S3 Presigned Url Generation.";
    public static final String S3_PRESIGNED_URL_PROCESS_SUCCESS_MESSAGE =
            "Pre-Signed URL generation process for stac onboarding completed successfully.";
    public static final String S3_PRESIGNED_URL_PROCESS_FAILURE_MESSAGE =
            "Pre-Signed URL generation process for stac onboarding failed.";
    public static final String HANDLE_FAILURE_MESSAGE =
            "Failed to update job table status to FAILED after handler failure";

}