-- Existing STAC collection tables are recreated as partitions of the stac_collections_part table.
-- An empty partition is created for a particular collection, attached to the main partition table 
-- using the collection_id as the partition key. The data of the collection is inserted into the partition
-- table (p_id is omitted and collection_id is added). Finally, the old collection table is deleted and the 
-- newly created partition is renamed to the collection_id.

CREATE OR REPLACE FUNCTION make_existing_stac_collection_tables_into_partitions (tablename text)
    RETURNS VOID
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- create a new table with same schema as stac_collections_part
    CREATE TABLE temp (
        LIKE stac_collections_part INCLUDING DEFAULTS INCLUDING CONSTRAINTS
    );
    -- attach empty table as partition
    EXECUTE format('ALTER TABLE stac_collections_part ATTACH PARTITION temp FOR VALUES IN (%L::uuid)', tablename);
    -- add all data from the STAC collection table into the partitioned collection table including collection_id obtained from the table name - p_id will automatically be filled and will be continuous
    EXECUTE format('INSERT INTO stac_collections_part (id, collection_id, bbox, properties, geom) (SELECT id, %L::uuid, bbox, properties, geom FROM %I)', tablename, tablename);
    -- drop the STAC collections table
    EXECUTE format('DROP TABLE %I', tablename);
    -- rename temp partition table to the STAC collection ID
    EXECUTE format('ALTER TABLE temp RENAME TO %I', tablename);
END
$$;

-- run function for all existing STAC collection tables
SELECT
    make_existing_stac_collection_tables_into_partitions (collection_id::text)
FROM
    collection_type
WHERE
    type = 'STAC';

DROP FUNCTION make_existing_stac_collection_tables_into_partitions;
