-- Map metadata is looked up by collection_id (PK); href is only read from the row for S3 paths.
DROP INDEX IF EXISTS idx_collection_map_metadata_href;
