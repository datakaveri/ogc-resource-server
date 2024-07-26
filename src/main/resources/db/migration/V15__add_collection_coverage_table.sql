-- create collection_coverage table with foreign key constraint
CREATE TABLE collection_coverage (
    collection_id UUID PRIMARY KEY,
    schema JSONB,
    href VARCHAR,
    CONSTRAINT fk_collection
        FOREIGN KEY (collection_id)
        REFERENCES collections_details (id)
);
