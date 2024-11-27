CREATE TABLE stac_collections_part (
    id varchar(100),
    bbox double precision[],
    properties jsonb,
    collection_id uuid REFERENCES collections_details (id),
    geom geometry(Geometry, 4326),
    p_id int GENERATED ALWAYS AS IDENTITY
)
PARTITION BY LIST (collection_id);

-- Add primary key on p_id and collection_id

ALTER TABLE stac_collections_part ADD PRIMARY KEY (p_id, collection_id);

-- Add indexes on id, geom and properties

CREATE INDEX idx_stac_collections_part_id ON stac_collections_part (id);
CREATE INDEX idx_stac_collections_part_geom ON stac_collections_part USING GIST (geom);
CREATE INDEX idx_stac_collections_part_properties ON stac_collections_part USING GIN (properties);

GRANT INSERT, SELECT, UPDATE, DELETE ON stac_collections_part TO ${ogcUser};

