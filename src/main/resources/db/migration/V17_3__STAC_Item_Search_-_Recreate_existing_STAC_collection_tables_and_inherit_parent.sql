-- existing STAC collection tables have their own sequences/serials with p_id
-- since we want the p_id column sequence to be continuous across 
-- all children of the STAC collections parent by forcing all child tables to use the same sequence
-- we need to recreate existing STAC collections tables and use INHERITS during table
-- creation itself. Dropping `p_id` and then using the `ALTER TABLE <table-name> INHERITS <parent-table>`
-- did not work.

CREATE OR REPLACE FUNCTION recreate_existing_stac_collection_table_and_inherit(tablename text) RETURNS VOID
  LANGUAGE plpgsql AS
$$
BEGIN
   -- create a new table that inherits the STAC parent
   CREATE TABLE temp () INHERITS (stac_collection_parent);
   -- add all data from the STAC collection table into new table - p_id will automatically be filled and will be continuous
   EXECUTE format('INSERT INTO temp (id, bbox, properties, geom) (SELECT id, bbox, properties, geom FROM %I)', tablename);
   -- drop the STAC collections table
   EXECUTE format('DROP TABLE %I', tablename);
   -- rename temp table to the STAC collections table
   EXECUTE format('ALTER TABLE temp RENAME TO %I', tablename);
END
$$;

-- run function for all existing STAC collection tables
SELECT recreate_existing_stac_collection_table_and_inherit(collection_id::text) FROM collection_type WHERE type = 'STAC';

DROP FUNCTION recreate_existing_stac_collection_table_and_inherit;
