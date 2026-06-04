package ogc.rs.processes.mapCollectionOnboarding;

public final class SqlConstants {

  private SqlConstants() {}

  public static final String CHECK_COLLECTION_EXISTENCE_QUERY =
      "SELECT EXISTS (SELECT 1 FROM collections_details WHERE id = $1::UUID) AS exists";

  public static final String INSERT_COLLECTION_DETAILS_QUERY =
      "INSERT INTO collections_details (id, title, description, crs, bbox, temporal) "
          + "VALUES ($1::UUID, $2, $3, $4, $5, $6)";

  public static final String INSERT_RI_DETAILS_QUERY =
      "INSERT INTO ri_details (id, access, role_id) VALUES ($1, $2, $3)";

  public static final String INSERT_COLLECTION_TYPE_QUERY =
      "INSERT INTO collection_type (collection_id, type) VALUES ($1::UUID, $2)";

  public static final String INSERT_COLLECTIONS_ENCLOSURE_QUERY =
      "INSERT INTO collections_enclosure (collections_id, title, href, type, size, s3_bucket_id) "
          + "VALUES ($1::UUID, $2, $3, $4, $5, $6)";

  public static final String COLLECTION_SUPPORTED_CRS_INSERT_QUERY =
      "WITH data AS (SELECT $1::uuid, id AS crs_id FROM crs_to_srid WHERE srid = 4326 OR srid = $2) "
          + "INSERT INTO collection_supported_crs (collection_id, crs_id) SELECT * FROM data";

  public static final String INSERT_COLLECTION_MAP_METADATA_QUERY =
      "INSERT INTO collection_map_metadata "
          + "(collection_id, href, s3_bucket_id, content_bbox, wms_url, raster_width, raster_height) "
          + "VALUES ($1::UUID, $2, $3, $4::double precision[], $5, $6, $7)";

  public static final String CRS_TO_SRID_SELECT_QUERY =
      "SELECT crs, srid FROM crs_to_srid WHERE srid = $1";

  public static final String CRS_TO_SRID_INSERT_QUERY =
      "INSERT INTO crs_to_srid (crs, srid) VALUES ($1, $2) ON CONFLICT (crs) DO NOTHING";
}
