INSERT INTO collections_details (id, title, description, crs) VALUES ('61f2187e-affe-4f28-be0e-fe1cd37dbd4e', 'Append process testing', 'Append process testing', 'http://www.opengis.net/def/crs/OGC/1.3/CRS84');

INSERT INTO ri_details (id, access, role_id) VALUES ('61f2187e-affe-4f28-be0e-fe1cd37dbd4e','OPEN','0ff3d306-9402-4430-8e18-6f95e4c03c97' );

WITH data AS (select '61f2187e-affe-4f28-be0e-fe1cd37dbd4e'::uuid, id AS crs_id FROM crs_to_srid WHERE srid = 4326) INSERT INTO collection_supported_crs (collection_id, crs_id) SELECT * FROM data;

INSERT INTO collection_type (collection_id , type) VALUES ('61f2187e-affe-4f28-be0e-fe1cd37dbd4e', 'FEATURE');

CREATE TABLE "61f2187e-affe-4f28-be0e-fe1cd37dbd4e" (id serial, postcode varchar, positional_quality_indicator integer, country_code varchar, nhs_regional_ha_code varchar, nhs_ha_code varchar, admin_county_code varchar, admin_district_code varchar, admin_ward_code varchar, geom geometry(Point, 4326));
