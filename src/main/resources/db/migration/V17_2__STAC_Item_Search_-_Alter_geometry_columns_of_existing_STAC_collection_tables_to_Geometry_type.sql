-- Child tables must have the same kind of geometry columns as the parent, hence changing
-- existing STAC tables to have the generic geometry type of Geometry

CREATE OR REPLACE FUNCTION update_geom_cols_for_existing_stac_table(tablename text) RETURNS VOID
  LANGUAGE plpgsql AS
$$
BEGIN
   EXECUTE format('ALTER TABLE %I ALTER COLUMN geom TYPE geometry(Geometry,4326)', tablename); 
END
$$;

-- run function for all existing STAC collection tables
SELECT update_geom_cols_for_existing_stac_table(collection_id::text) FROM collection_type WHERE type = 'STAC';

DROP FUNCTION update_geom_cols_for_existing_stac_table;
