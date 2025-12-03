package ogc.rs.processes.userDatasetUsageCheck;

public class Constants {
    public static final String USER_DATASET_USAGE_TITLE = "UserDatasetUsageCheck"; // Process title for authorization
    public static final String STARTING_DATASET_USAGE_CHECK_PROCESS_MESSAGE =
            "Starting User Dataset Usage Check Process.";
    public static final String TIMESTAMP_QUERY =
            "SELECT MAX(timestamp) AS last_used "
                    + "FROM metering "
                    + "WHERE user_id = $1 AND collection_id = $2";
}
