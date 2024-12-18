CREATE TABLE collections_enclosure (
    id UUID PRIMARY KEY DEFAULT public.gen_random_uuid(),
    collections_id UUID REFERENCES collections_details(id) NOT NULL,
    title VARCHAR(255) NOT NULL,
    href VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    size BIGINT NOT NULL,
    CONSTRAINT unique_enclosure UNIQUE (collections_id, title)
);

INSERT INTO collections_enclosure (id, collections_id, title, href, type, size)
SELECT id, stac_collections_id, title, href, type, size
FROM stac_collections_assets;

CREATE INDEX idx_collections_enclosure_collections_id
ON collections_enclosure (collections_id);

GRANT SELECT, INSERT, DELETE ON collections_enclosure TO  ${ogcUser}

-- TRUNCATE table stac_collections_assets as the data has been shifted to collections_enclosure
TRUNCATE TABLE stac_collections_assets;

