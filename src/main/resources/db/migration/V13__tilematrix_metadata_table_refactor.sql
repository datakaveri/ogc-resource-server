-- drop existing table
DROP TABLE tilematrixset_metadata;

-- create new table for storing TileMatrixSet static data
CREATE TABLE tms_metadata (id uuid primary key default public.gen_random_uuid(), title character varying (50)
 , uri character varying (100), tilewidth integer, tileheight integer, crs character varying (100));
-- add unique constraint for title
ALTER TABLE tms_metadata ADD CONSTRAINT unique_tms UNIQUE (title);

-- add metadata from tilematrixset_metadata to tms_metadata
INSERT INTO tms_metadata (title, uri, tilewidth, tileheight, crs) SELECT DISTINCT(id), uri, tilewidth, tileheight, crs
 FROM tilematrixsets_relation;

-- add new column for foreign key relation linking tilematrixset_relations and tms_metadata
ALTER TABLE tilematrixsets_relation ADD COLUMN tms_id uuid;
-- update the tms data in the tilematrixsets_relation
UPDATE tilematrixsets_relation SET tms_id =tms_metadata.id FROM tms_metadata
 WHERE tilematrixsets_relation.id= tms_metadata.title;

-- refactor tilematrixsets_relation table to remove redundant row and column data
ALTER TABLE tilematrixsets_relation DROP COLUMN datatype;
ALTER TABLE tilematrixsets_relation DROP COLUMN id;
ALTER TABLE tilematrixsets_relation DROP COLUMN title;
ALTER TABLE tilematrixsets_relation DROP COLUMN uri;
ALTER TABLE tilematrixsets_relation DROP COLUMN tilewidth;
ALTER TABLE tilematrixsets_relation DROP COLUMN tileheight;
ALTER TABLE tilematrixsets_relation DROP COLUMN crs;
ALTER TABLE tilematrixsets_relation ADD COLUMN id uuid PRIMARY KEY DEFAULT public.gen_random_uuid();

-- add foreign key constraint
ALTER TABLE tilematrixsets_relation ADD CONSTRAINT tms_fkey FOREIGN KEY (tms_id)
 REFERENCES tms_metadata (id) ON DELETE CASCADE;
-- add NULL constraint on tms_id on tilematrixset_relation
ALTER TABLE tilematrixsets_relation ALTER COLUMN tms_id SET NOT NULL;

-- Add GRANTS to tms_metadata table
GRANT SELECT, INSERT ON tms_metadata to ${ogcUser};