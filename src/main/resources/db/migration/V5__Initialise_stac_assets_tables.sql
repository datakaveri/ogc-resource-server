CREATE TYPE asset_role_enum AS ENUM ('thumbnail', 'overview', 'data', 'metadata');

CREATE TABLE stac_assets (
    id UUID PRIMARY KEY DEFAULT public.gen_random_uuid(),
    stac_collections_id UUID REFERENCES collections_details(id) not null,
    title VARCHAR(255) not null,
    href VARCHAR(255) not null,
    type VARCHAR(50) not null,
    size BIGINT not null,
    role asset_role_enum[] not null,
	constraint unique_stac_asset unique (stac_collections_id, title)

);

