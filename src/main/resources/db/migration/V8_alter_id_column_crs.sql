-- This script converts primary keys from integers to UUIDs in two tables.
-- It updates the foreign key in `collection_supported_crs` to reference the new UUID in `crs_to_srid`.
-- It also drops the original integer-based primary keys, renames the new UUID columns, and re-establishes the appropriate primary and foreign key constraints.

-- Add new UUID column with auto-generation
ALTER TABLE crs_to_srid
ADD COLUMN new_id uuid DEFAULT gen_random_uuid();

ALTER TABLE collection_supported_crs
ADD COLUMN new_id uuid DEFAULT gen_random_uuid();

ALTER TABLE collection_supported_crs
ADD COLUMN new_crs_id uuid;

-- Replace crs_id in collection_supported_crs with new UUID keys from crs_to_srid
UPDATE collection_supported_crs c
SET new_crs_id = (SELECT new_id FROM crs_to_srid cr WHERE cr.id = c.crs_id);

-- Drop foreign key constraint
ALTER TABLE collection_supported_crs
DROP CONSTRAINT collection_supported_crs_crs_id_fkey;

-- Drop primary key constraints
ALTER TABLE crs_to_srid
DROP CONSTRAINT crs_to_srid_pkey;

ALTER TABLE collection_supported_crs
DROP CONSTRAINT collection_supported_crs_pkey;

-- Drop the old ID columns
ALTER TABLE crs_to_srid
DROP COLUMN id;

ALTER TABLE collection_supported_crs
DROP COLUMN id;

ALTER TABLE collection_supported_crs
DROP COLUMN crs_id;

-- Rename new UUID columns to id
ALTER TABLE crs_to_srid
RENAME COLUMN new_id TO id;

ALTER TABLE collection_supported_crs
RENAME COLUMN new_id TO id;

ALTER TABLE collection_supported_crs
RENAME COLUMN new_crs_id TO crs_id;

-- Add primary key constraints
ALTER TABLE crs_to_srid
ADD CONSTRAINT crs_to_srid_pkey PRIMARY KEY (id);

ALTER TABLE collection_supported_crs
ADD CONSTRAINT collection_supported_crs_pkey PRIMARY KEY (id);

-- Add foreign key constraint
ALTER TABLE collection_supported_crs
ADD CONSTRAINT collection_supported_crs_id_fkey FOREIGN KEY (crs_id) REFERENCES crs_to_srid(id);
