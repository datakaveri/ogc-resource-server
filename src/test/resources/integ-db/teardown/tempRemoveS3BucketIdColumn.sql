ALTER TABLE tilematrixsets_relation DROP COLUMN s3_bucket_id;
ALTER TABLE collection_coverage DROP COLUMN s3_bucket_id;
ALTER TABLE collections_enclosure DROP COLUMN s3_bucket_id;
ALTER TABLE stac_items_assets DROP COLUMN s3_bucket_id;
ALTER TABLE stac_collections_assets DROP COLUMN s3_bucket_id;
