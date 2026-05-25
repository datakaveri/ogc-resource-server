-- Map collection metadata (CRS, bbox, raster source, WMS link) populated at maps onboarding.
CREATE TABLE collection_map_metadata (
    collection_id UUID PRIMARY KEY
        REFERENCES collections_details(id) ON DELETE CASCADE,
    href VARCHAR(512) NOT NULL,
    s3_bucket_id VARCHAR NOT NULL DEFAULT 'default',
    native_crs VARCHAR(255) NOT NULL,
    content_bbox DOUBLE PRECISION[4] NOT NULL,
    wgs84_bbox DOUBLE PRECISION[4],
    wms_url VARCHAR(1024),
    raster_width INTEGER,
    raster_height INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_collection_map_metadata_href ON collection_map_metadata (href);

GRANT SELECT, INSERT, UPDATE, DELETE ON collection_map_metadata TO ${ogcUser};
