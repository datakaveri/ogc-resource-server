INSERT INTO collections_details (id, title, description, crs) VALUES ('5d568f26-ccaf-456d-ba04-7feb589c1185', 'Test_ExistingFeatureFlow_TilesMetaDataOnboarding', 'Tiles Meta Data Onboarding Existing Feature Test', 'http://www.opengis.net/def/crs/OGC/1.3/CRS84');

INSERT INTO ri_details (id, access, role_id) VALUES ('5d568f26-ccaf-456d-ba04-7feb589c1185','OPEN','0ff3d306-9402-4430-8e18-6f95e4c03c97' );

WITH data AS (select '5d568f26-ccaf-456d-ba04-7feb589c1185'::uuid, id AS crs_id FROM crs_to_srid WHERE srid = 4326) INSERT INTO collection_supported_crs (collection_id, crs_id) SELECT * FROM data;

INSERT INTO collection_type (collection_id , type) VALUES ('5d568f26-ccaf-456d-ba04-7feb589c1185', 'FEATURE');

CREATE TABLE "5d568f26-ccaf-456d-ba04-7feb589c1185" (id serial, postcode varchar, positional_quality_indicator integer, country_code varchar, nhs_regional_ha_code varchar, nhs_ha_code varchar, admin_county_code varchar, admin_district_code varchar, admin_ward_code varchar, geom geometry(Point, 4326));

-- grant select access to all users
GRANT SELECT, INSERT ON "5d568f26-ccaf-456d-ba04-7feb589c1185" TO PUBLIC;
GRANT SELECT, USAGE ON SEQUENCE "5d568f26-ccaf-456d-ba04-7feb589c1185_id_seq" TO PUBLIC;
