DO $$
DECLARE
    new_id UUID;
    crs_id UUID;
    trigger_name TEXT;
BEGIN
    -- Insert collection and get ID
    INSERT INTO collections_details (
        title, description, datetime_key, crs, bbox, temporal, license
    ) VALUES (
        'collection for testing records API',
        'collection for testing records API',
        NULL,
        'http://www.opengis.net/def/crs/OGC/1.3/CRS84',
        '{-79.77,40.47,-71.8,45.02}',
        ARRAY[NOW()::TEXT, NOW()::TEXT],
        NULL
    )
    RETURNING id INTO new_id;

    -- Select the crs_id based on srid = 4326
    SELECT id INTO crs_id FROM crs_to_srid WHERE srid = 4326 LIMIT 1;

    INSERT INTO collection_supported_crs (collection_id, crs_id)
    VALUES (new_id, crs_id);

    INSERT INTO collection_type (collection_id, type)
    VALUES (new_id, 'COLLECTION');

    INSERT INTO ri_details (id, role_id, access)
    VALUES (new_id, '00000000-0000-0000-0000-000000000000', 'OPEN');

    -- Create table with UUID as the table name and SERIAL ID for auto-increment
    EXECUTE format($f$
        CREATE TABLE public.%I (
            id SERIAL PRIMARY KEY,
            created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            title character varying(50) DEFAULT ''::character varying,
            description character varying(100) DEFAULT ''::character varying,
            keywords TEXT[],
            externalids UUID[],
            bbox NUMERIC[],
            temporal TEXT[],
            geometry GEOMETRY,
            collection_id UUID,
            CONSTRAINT fk_collection
              FOREIGN KEY (collection_id)
              REFERENCES collections_details(id)
              ON DELETE CASCADE
        )
    $f$, new_id);

    -- Create trigger
    trigger_name := 'trg_set_geometry_from_bbox_' || replace(new_id::TEXT, '-', '_');

    EXECUTE format($f$
        CREATE TRIGGER %I
        BEFORE INSERT OR UPDATE ON public.%I
        FOR EACH ROW
        EXECUTE FUNCTION set_geometry_from_bbox()
    $f$, trigger_name, new_id);

   EXECUTE format($f$
    GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO %s
$f$, new_id, ${ogcUser});
END $$ LANGUAGE plpgsql;