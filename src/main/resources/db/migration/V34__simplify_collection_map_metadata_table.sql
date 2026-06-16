-- Native CRS and WGS84 bbox live on collections_details (crs, bbox). Map table keeps raster-specific fields.
ALTER TABLE collection_map_metadata
    DROP COLUMN IF EXISTS native_crs,
    DROP COLUMN IF EXISTS wgs84_bbox,
    DROP COLUMN IF EXISTS created_at,
    DROP COLUMN IF EXISTS updated_at;
