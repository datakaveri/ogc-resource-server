package ogc.rs.processes.s3PreSignedURLGeneration;

public class Constants {
    public static final String MESSAGE = "message";
    public static final String STARTING_PRE_SIGNED_URL_PROCESS_MESSAGE =
            "Starting the process to generate a Pre-Signed URL.";
    public static final String RESOURCE_OWNERSHIP_ERROR = "Resource does not belong to the user.";
    public static final String CAT_REQUEST_RESPONSE =
            "CAT API response received successfully. Resource ownership validated, and object key generated.";
    public static final String S3_PRE_SIGNED_URL_GENERATOR_MESSAGE =
            "Generating the S3 Pre-Signed URL.";
    public static final String S3_PRE_SIGNED_URL_GENERATOR_FAILURE_MESSAGE =
            "Failed to generate pre-signed URL";
    public static final String S3_PRE_SIGNED_URL_PROCESS_SUCCESS_MESSAGE =
            "Pre-Signed URL generation process completed successfully.";
    public static final String S3_PRE_SIGNED_URL_PROCESS_FAILURE_MESSAGE =
            "Pre-Signed URL generation process failed.";
    public static final String HANDLE_FAILURE_MESSAGE =
            "Failed to update job table status to FAILED after the process encountered an error.";
}
