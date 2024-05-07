-- create collection_type table
CREATE TABLE collection_type (id uuid DEFAULT PUBLIC.gen_random_uuid(), collection_id uuid, type item_type, PRIMARY KEY (id));
-- add foreign key constraint
ALTER TABLE collection_type ADD CONSTRAINT collection_type_collection_id_fkey FOREIGN KEY (collection_id) REFERENCES collections_details(id) ON DELETE CASCADE;
-- create index on collection_id
CREATE INDEX collection_type_idx on collection_type(collection_id);

-- insert/update collection_type table using collections_details column 'type'
INSERT INTO collection_type (collection_id, type) SELECT id, type from collections_details;

-- delete 'type' column from collections_details
ALTER TABLE collections_details DROP COLUMN type;
