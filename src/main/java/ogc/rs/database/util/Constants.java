package ogc.rs.database.util;

public class Constants {

    public static final String PROCESSES_TABLE_NAME = "processes_table";
    public static final String INSERT_COLLECTIONS_DETAILS = "INSERT INTO collections_Details (id, title, description, datetime_key, crs, bbox, temporal, license) " +
            "VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";
    public static final String INSERT_COLLECTION_TYPE = "INSERT INTO collection_type (id, collection_id, type) VALUES (gen_random_uuid(), $1, 'STAC')";
    public static final String INSERT_ROLES = "INSERT INTO roles (user_id,role) VALUES($1,$2) ON CONFLICT DO NOTHING";
    public static final String INSERT_RI_DETAILS = "INSERT INTO ri_details (id,role_id,access) VALUES($1,$2,$3)";
    public static final String CREATE_TABLE_BY_ID = "CREATE TABLE \"$1\" (LIKE stac_collections_part INCLUDING CONSTRAINTS)";
    public static final String ATTACH_PARTITION = "ALTER TABLE stac_collections_part ATTACH PARTITION \"$1\" FOR VALUES IN ('$1')";
    public static final String GRANT_PRIVILEGES = "GRANT SELECT, UPDATE, DELETE ON \"$1\" TO \"$2\"";
    public static final String DATABASE_USER = "databaseUser";
    public static final String UPDATE_COLLECTIONS_DETAILS = "UPDATE collections_Details SET title = COALESCE($2, title), " +
            "description = COALESCE($3, description), " +
            "crs = COALESCE($5, crs), bbox = COALESCE($6, bbox), temporal = COALESCE($7, temporal), " +
            "license = COALESCE($8, license) WHERE id = $1";
    public static final String STAC_ITEMS_DATETIME_KEY = "properties ->> 'datetime'";
}

