package ogc.rs.processes.s3MultiPartUploadForStacOnboarding;

public class Constants {
    // SQL query to check if the resource is already onboarded as stac
    public static final String COLLECTION_TYPE_QUERY = "SELECT COUNT(*) FROM collection_type WHERE collection_id = $1 AND type = 'STAC'";
    // SQL query to check for a row with the specific collection_id and item_id
    public static final String ITEM_EXISTENCE_CHECK_QUERY = "SELECT COUNT(*) FROM stac_collections_part WHERE collection_id = $1 AND id = $2";

    //Message Constants
    public static final String MESSAGE = "message";
    public static final String INITIATE_MULTIPART_UPLOAD_PROCESS_START_MESSAGE =
            "Starting the multipart upload initiation process for STAC onboarding.";
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
            "Item ID not provided. Skipping item existence check.";
    public static final String OBJECT_ALREADY_EXISTS_MESSAGE =
            "Object already exists in S3, no need to initiate S3 multipart upload.";
    public static final String OBJECT_DOES_NOT_EXIST_MESSAGE =
    "Object not found in S3. Proceeding with multipart upload initiation.";
    public static final String INITIATE_MULTIPART_UPLOAD_MESSAGE =
            "S3 multipart upload initiated. Upload ID generated successfully.";
    public static final String INITIATE_MULTIPART_UPLOAD_FAILURE_MESSAGE =
            "Failed to initiate S3 multipart upload.";
    public static final String INITIATE_MULTIPART_UPLOAD_PROCESS_COMPLETE_MESSAGE =
            "Successfully initiated S3 Multipart upload and presigned URLs generated for all parts.";
    public static final String INITIATE_MULTIPART_UPLOAD_PROCESS_FAILURE_MESSAGE =
            "Failed to initiate S3 Multipart upload and generate presigned urls for all parts.";

    public static final String COMPLETE_MULTIPART_UPLOAD_PROCESS_START_MESSAGE =
            "Starting the multipart upload completion process for STAC onboarding.";
    public static final String INVALID_PART_FORMAT_MESSAGE =
            "The request contains an invalid part format.";
    public static final String COMPLETE_MULTIPART_UPLOAD_PROCESS_SUCCESS_MESSAGE =
            "Successfully completed S3 Multipart upload completion process.";
    public static final String COMPLETE_MULTIPART_UPLOAD_FAIL_MESSAGE =
            "Failed to complete S3 multipart upload completion process.";

}
