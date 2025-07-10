package ogc.rs.processes.featureAttributesExtraction;

/**
 * Constants used in the Feature Attributes Extraction Process
 */
public class Constants {

    // Process status messages
    public static final String STARTING_FEATURE_EXTRACTION_MESSAGE =
            "Starting feature attributes extraction process";

    public static final String COLLECTION_VALIDATION_SUCCESS_MESSAGE =
            "Collection validation completed successfully";

    public static final String FEATURE_EXTRACTION_SUCCESS_MESSAGE =
            "Feature attributes extracted successfully";

    public static final String PROCESS_COMPLETION_MESSAGE =
            "Feature attributes extraction process completed successfully";

    // Error messages
    public static final String COLLECTION_NOT_FOUND_MESSAGE =
            "Specified collection does not exist";

    public static final String ATTRIBUTES_NOT_FOUND_MESSAGE =
            "One or more attributes in the input do not exist in the collection.";

    public static final String FEATURE_ATTRIBUTES_EXTRACTION_FAILURE_MESSAGE =
            "Failed to extract feature attributes from database";
}