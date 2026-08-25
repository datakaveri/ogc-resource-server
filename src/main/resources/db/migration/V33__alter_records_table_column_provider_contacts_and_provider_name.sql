DO $$
DECLARE
    record_table text;
BEGIN
    FOR record_table IN
        SELECT collections_details.id
        FROM collections_details
        JOIN collection_type
            ON collections_details.id = collection_type.collection_id
        WHERE collection_type.type = 'COLLECTION'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I
             ALTER COLUMN provider_name TYPE VARCHAR(500),
             ALTER COLUMN provider_contacts TYPE VARCHAR(500)',
            record_table
        );
    END LOOP;
END $$;