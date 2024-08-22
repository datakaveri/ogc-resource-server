

CREATE TABLE stac_items_assets (
    id UUID PRIMARY KEY DEFAULT public.gen_random_uuid(),
    collections_id UUID REFERENCES collections_details(id) not null,
    title VARCHAR(255) not null,
    description varchar(500) not null,
    href VARCHAR(255) not null,
    type VARCHAR(50) not null,
    size BIGINT not null,
    role asset_role_enum[] not null
);

ALTER TABLE ADD	CONSTRAINT unique_stac_asset UNIQUE (stac_collections_id, title)

GRANT SELECT,UPDATE,INSERT ON stac_items_assets TO ${ogcUser} ;
