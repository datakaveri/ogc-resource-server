CREATE TYPE asset_role_enum AS ENUM ('thumbnail', 'overview', 'data', 'metadata');

CREATE TABLE stac_assets (
    id UUID PRIMARY KEY,
    stac_collections_id UUID REFERENCES collections_details(id),
    asset_name VARCHAR(255) UNIQUE,
    href VARCHAR(255),
    asset_type VARCHAR(50),
    size INTEGER,
	asset_role asset_role_enum[] not null

);