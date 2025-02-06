package ogc.rs.processes.collectionOnboarding;

public class Constants {

  public static final String FEATURE = "FEATURE";
  public static final String COLLECTION_ROLE = "{data}";
  public static final String COLLECTION_TYPE = "application/geopackage+sqlite3";
  public static final String GRANT_QUERY =
      "GRANT SELECT, INSERT ON  \"collections_details_id\" TO databaseUser";
  public static final String STAC_COLLECTION_ASSETS_INSERT_QUERY =
      "INSERT INTO collections_enclosure (collections_id,title,href,type,size) VALUES ($1::UUID, $2, $3, $4, $5) returning id;";

  public static final String COLLECTIONS_DETAILS_INSERT_QUERY =
      "INSERT INTO collections_details (id, title, description, crs) VALUES ($1::UUID, $2, $3, $4)";
  public static final String COLLECTION_SUPPORTED_CRS_INSERT_QUERY =
      "with data as (select $1::uuid,"
          + " id as crs_id from crs_to_srid where srid = 4326 or srid = $2) insert into collection_supported_crs (collection_id, crs_id) select * from data";
  public static final String ROLES_INSERT_QUERY =
      "INSERT INTO roles (user_id,role) VALUES($1,$2) ON CONFLICT DO NOTHING;";
  public static final String RG_DETAILS_INSERT_QUERY =
      "INSERT INTO rg_details (id,role_id,access) VALUES($1,$2,$3) ON CONFLICT DO NOTHING;";
  public static final String RI_DETAILS_INSERT_QUERY =
      "INSERT INTO ri_details (id,role_id,access) VALUES($1,$2,$3);";
  public static final String COLLECTION_TYPE_INSERT_QUERY = "INSERT INTO collection_type (collection_id,type) VALUES($1::UUID,$2);";
  public static final String COLLECTIONS_DETAILS_SELECT_QUERY =
      "SELECT * FROM collections_details where id=$1::UUID;";
  public static final String COLLECTIONS_DETAILS_TABLE_EXIST_QUERY =
      "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = $1) AS table_existence;";
  public static final String CRS_TO_SRID_SELECT_QUERY =
      "SELECT crs,srid FROM CRS_TO_SRID WHERE SRID = $1;";
  public static final String UPDATE_COLLECTIONS_DETAILS =
          "UPDATE collections_details SET bbox = $1::DOUBLE PRECISION[] WHERE id = $2::UUID;";
  public static final String DEFAULT_SERVER_CRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
  public static final String MESSAGE = "message";
  public static final String CHECK_CAT_FOR_RESOURCE_REQUEST =
      "Checking the catalog for the resource.";
  public static final String CAT_REQUEST_RESPONSE =
      "Catalog request successful. Now checking for collection in the collection table.";
  public static final String COLLECTION_RESPONSE =
      "Collection not found. Proceeding to check for CRS.";
  public static final String CRS_RESPONSE =
      "CRS is correct. Trying to get the file size.";
  public static final String S3_RESPONSE= "Got the file size. Initiating the onboarding process.";
  public static final String ONBOARDING_RESPONSE =
      "Onboarding completed. Verifying collection in the database..";
  public static final String VERIFYING_RESPONSE =
          "Collection present in database. Updating bbox for the collection.";
  public static final String DB_CHECK_RESPONSE =
      "Updated bbox for the collection. Onboarding has been completed successfully.";
  public static final String ITEM_NOT_PRESENT_ERROR="Item not present in catalogue";
  public static final String RESOURCE_OWNERSHIP_ERROR = "Resource does not belong to the user.";
  public static final String CAT_RESPONSE_FAILURE="Failed to get response from Catalogue ";
  public static final String COLLECTION_PRESENT_ERROR="Collection already present.";
  public static final String CRS_ERROR="CRS not present as EPSG";
  public static final String CRS_FETCH_FAILED="Failed to fetch CRS.";
  public static final String OGR_INFO_FAILED="Failed in ogrInfo.";
  public static final String TABLE_NOT_EXIST_ERROR="Table does not exist.";
  public static final String COLLECTION_NOT_PRESENT_ERROR ="Collection not present in collections_details";
  public static final String OGR_2_OGR_FAILED = "Failed to onboard the collection in OGR2OGR.";
  public static final String ONBOARDING_FAILED_DB_ERROR="Failed to onboard the collection in db.";
}
