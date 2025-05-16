package ogc.rs.processes.tilesMetaDataOnboarding;

public class SqlConstants {
    // Query to check if the collection exists in the collection_details table
    public static final String CHECK_COLLECTION_EXISTENCE_QUERY =
            "SELECT EXISTS (SELECT 1 FROM collections_details WHERE id = $1::UUID)";
    // Query to get the collection type from the collection_type table
    public static String GET_COLLECTION_TYPE_QUERY =
            "SELECT collection_id, array_agg(type) from collection_type group by collection_id having collection_id = $1::uuid";
    // Query to check if the tile matrix set exists in tms_metadata table and retrieve all columns
    public static final String CHECK_TILE_MATRIX_SET_EXISTENCE_QUERY =
            "SELECT *, EXISTS (SELECT 1 FROM tms_metadata WHERE title = $1) AS exists FROM tms_metadata WHERE title = $1";
    // Query to insert required values into collections_details table
    public static final String INSERT_COLLECTION_DETAILS_QUERY =
            "INSERT INTO collections_details (id, title, description, crs, bbox, temporal) VALUES ($1::UUID, $2, $3, $4, $5, $6)";
    // Query to insert required values into ri_details table
    public static final String INSERT_RI_DETAILS_QUERY =
            "INSERT INTO ri_details (id, access, role_id) VALUES ($1, $2, $3)";
    // Query to insert required values into collection_type table
    public static final String INSERT_COLLECTION_TYPE_QUERY =
            "INSERT INTO collection_type (collection_id, type) VALUES ($1::UUID, $2)";
    // Query to insert required values into tilematrixsets_relation table
    public static final String INSERT_TILE_MATRIX_SET_RELATION_QUERY =
            "INSERT INTO tilematrixsets_relation (collection_id, tms_id, pointoforigin, s3_bucket_id) VALUES ($1, $2, $3, $4)";
}
