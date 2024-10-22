

CREATE TABLE stac_items_assets (
    id UUID PRIMARY KEY DEFAULT public.gen_random_uuid(),
    collection_id UUID REFERENCES collections_details(id) not null,
    item_id VARCHAR(255) not null,
    title VARCHAR(255),
    description varchar(500),
    href VARCHAR(255) not null,
    type VARCHAR(50) not null,
    size BIGINT not null,
    roles asset_role_enum[] not null,
    properties jsonb
);

ALTER TABLE stac_items_assets ADD CONSTRAINT unique_collection_stac_item_asset UNIQUE (collection_id, item_id, id);

GRANT SELECT,UPDATE,INSERT ON stac_items_assets TO ${ogcUser} ;
