DO $$
DECLARE
    record_table text;
    sql text;
BEGIN
    SELECT collections_details.id INTO record_table
    FROM collections_details
    LEFT JOIN collections_enclosure
        ON collections_details.id = collections_enclosure.collections_id
    JOIN collection_supported_crs
        ON collections_details.id = collection_supported_crs.collection_id
    JOIN crs_to_srid
        ON crs_to_srid.id = collection_supported_crs.crs_id
    JOIN collection_type
        ON collections_details.id = collection_type.collection_id
    WHERE collection_type.type = 'COLLECTION'
    GROUP BY collections_details.id
    LIMIT 1;

    sql := format(
        'ALTER TABLE %I
         ALTER COLUMN provider_name TYPE VARCHAR(500),
         ALTER COLUMN provider_contacts TYPE VARCHAR(500)',
        record_table
    );

    EXECUTE sql;
END $$;