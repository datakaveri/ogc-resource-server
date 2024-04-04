package ogc.rs.processes.util;

import ogc.rs.apiserver.util.OgcException;

public class Constants {
  public static final String FEATURE = "FEATURE";

  public static final String SECURE_ACCESS_KEY = "SECURE";
  public static final String COLLECTION_ROLE = "{data}";
  public static final int FILE_SIZE = 0;
  public static final String COLLECTION_TYPE = "application/geopackage+sqlite3";
  public static final String GRANT_QUERY =
    "GRANT SELECT, INSERT ON  \"collections_details_id\" TO databaseUser";
  public static final String COLLECTIONS_DETAILS_INSERT_QUERY =
    "INSERT INTO collections_details (id, title, description, crs, type) VALUES ($1::UUID, $2, $3, $4, $5)";
  public static final String COLLECTION_SUPPORTED_CRS_INSERT_QUERY =
    "with data as (select $1::uuid," +
      " id as crs_id from crs_to_srid where srid = 4326 or srid = $2) insert into collection_supported_crs (collection_id, crs_id) select * from data";
  public static final String ROLES_INSERT_QUERY =
    "INSERT INTO roles (user_id,role) VALUES($1,$2) ON CONFLICT DO NOTHING;";
  public static final String RG_DETAILS_INSERT_QUERY =
    "INSERT INTO rg_details (id,role_id,access) VALUES($1,$2,$3) ON CONFLICT DO NOTHING;";
  public static final String RI_DETAILS_INSERT_QUERY =
    "INSERT INTO ri_details (id,rg_id,access) VALUES($1,$2,$3);";
  public static final String UPDATE_JOB_TABLE_STATUS_QUERY =
    "UPDATE JOBS_TABLE SET UPDATED_AT = NOW(), STARTED_AT = CASE WHEN $1 = 'RUNNING' THEN NOW() ELSE STARTED_AT END, FINISHED_AT = CASE WHEN $1 IN ('FAILED', 'SUCCESSFUL') THEN NOW() ELSE NULL END, PROGRESS = CASE WHEN $1 = 'SUCCESSFUL' THEN 100.0 WHEN $1 = 'RUNNING' THEN 16.67  ELSE PROGRESS END, STATUS = $1::JOB_STATUS_TYPE, MESSAGE = $2 WHERE ID = $3;";

  public static final String UPDATE_JOB_STATUS_PROGRESS =
    "UPDATE JOBS_TABLE SET PROGRESS = $1 WHERE ID = $2;";
  public static final String STAC_COLLECTION_ASSETS_INSERT_QUERY =
    "INSERT INTO stac_collections_assets (stac_collections_id,title,href,type,size,role) VALUES ($1::UUID, $2, $3, $4, $5,$6) returning id;";

  public static final String NEW_JOB_INSERT_QUERY =
    "INSERT INTO JOBS_TABLE (process_id,user_id,created_at," +
      "updated_at,input,output,progress,status,type,message) VALUES($1,$2,NOW(),NOW()," +
      "$3,$4,'0.0',$5,'PROCESS',$6) RETURNING ID,status;";
  public static final String PROCESS_EXIST_CHECK_QUERY =
    "SELECT * FROM PROCESSES_TABLE WHERE ID=$1";
  public static final String COLLECTIONS_DETAILS_SELECT_QUERY =
    "SELECT * FROM collections_details where id=$1::UUID;";
  public static final String COLLECTIONS_DETAILS_TABLE_EXIST_QUERY =
    "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = $1) AS table_existence;";
  public static final String CRS_TO_SRID_SELECT_QUERY =
    "SELECT crs,srid FROM CRS_TO_SRID WHERE SRID = $1;";

  public static final String DEFAULT_SERVER_CRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
  public static final  OgcException
    ogcException500 = new OgcException(500,"Internal Server Error","Internal Server Error");

}
