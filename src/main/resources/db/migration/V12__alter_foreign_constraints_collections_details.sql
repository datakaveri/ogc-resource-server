ALTER TABLE ONLY tilematrixsets_relation DROP CONSTRAINT collection_fkey,ADD CONSTRAINT collection_fkey
FOREIGN KEY (collection_id) REFERENCES collections_details(id) ON DELETE CASCADE;

ALTER TABLE collection_supported_crs DROP CONSTRAINT collection_supported_crs_collection_id_fkey,
ADD CONSTRAINT collection_supported_crs_collection_id_fkey
FOREIGN KEY (collection_id) REFERENCES collections_details(id) ON DELETE CASCADE;