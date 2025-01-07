-- Add SELECT, UPDATE and DELETE grants to existing STAC collection table partitions.
-- INSERT grant is *not* added because the inserts must occur through the stac_collections_part table
-- to preserve the `p_id` sequence

CREATE OR REPLACE FUNCTION add_grants_to_existing_stac_collection_tables_partitions (tablename text)
    RETURNS VOID
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Add grants
    EXECUTE format('GRANT SELECT, UPDATE, DELETE ON %I TO ${ogcUser}', tablename);
END
$$;

-- run function for all existing STAC collection table partitions
SELECT
    add_grants_to_existing_stac_collection_tables_partitions (collection_id::text)
FROM
    collection_type
WHERE
    type = 'STAC';

DROP FUNCTION add_grants_to_existing_stac_collection_tables_partitions;
