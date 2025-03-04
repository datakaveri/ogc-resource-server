-- Add s3_bucket_id column to all tables making use of S3. Also add default value of `default` for existing data
ALTER TABLE tilematrixsets_relation ADD COLUMN s3_bucket_id VARCHAR DEFAULT 'default' NOT NULL;
ALTER TABLE collection_coverage ADD COLUMN s3_bucket_id VARCHAR DEFAULT 'default' NOT NULL;
ALTER TABLE collections_enclosure ADD COLUMN s3_bucket_id VARCHAR DEFAULT 'default' NOT NULL;
ALTER TABLE stac_items_assets ADD COLUMN s3_bucket_id VARCHAR DEFAULT 'default' NOT NULL;
ALTER TABLE stac_collections_assets ADD COLUMN s3_bucket_id VARCHAR DEFAULT 'default' NOT NULL;
