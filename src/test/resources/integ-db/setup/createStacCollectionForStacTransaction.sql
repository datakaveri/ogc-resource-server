INSERT INTO collections_details (id, title, description, crs)
 VALUES ('0bfd0f9a-a31c-4ee9-9728-0a97248a4637'
, 'Test Collection for STAC transaction'
, 'A short description'
, 'http://www.opengis.net/def/crs/OGC/1.3/CRS84')
, ('6f95f983-a826-42e0-8e97-e224a546fe32'
  , 'Another Test Collection for STAC transaction'
  , 'A short description'
  , 'http://www.opengis.net/def/crs/OGC/1.3/CRS84');

INSERT INTO ri_details (id, access, role_id)
 VALUES ('0bfd0f9a-a31c-4ee9-9728-0a97248a4637','OPEN','0ff3d306-9402-4430-8e18-6f95e4c03c97')
 , ('6f95f983-a826-42e0-8e97-e224a546fe32','SECURE','0ff3d306-9402-4430-8e18-6f95e4c03c97' );


INSERT INTO collection_type (collection_id , type) VALUES ('0bfd0f9a-a31c-4ee9-9728-0a97248a46375', 'STAC')
, ('6f95f983-a826-42e0-8e97-e224a546fe32', 'STAC');

CREATE TABLE "0bfd0f9a-a31c-4ee9-9728-0a97248a46375" (LIKE stac_collections_part including defaults including CONSTRAINTS);
ALTER TABLE stac_collections_part attach partition "0bfd0f9a-a31c-4ee9-9728-0a97248a46375"
 FOR VALUES IN ('0bfd0f9a-a31c-4ee9-9728-0a97248a46375'::uuid);

CREATE TABLE "6f95f983-a826-42e0-8e97-e224a546fe32" (LIKE stac_collections_part including defaults including CONSTRAINTS);
ALTER TABLE stac_collections_part attach partition "6f95f983-a826-42e0-8e97-e224a546fe32"
 FOR VALUES IN ('6f95f983-a826-42e0-8e97-e224a546fe32'::uuid);

-- grant select access to all users
GRANT SELECT, INSERT ON "0bfd0f9a-a31c-4ee9-9728-0a97248a46375" TO PUBLIC;
GRANT SELECT, USAGE ON SEQUENCE "0bfd0f9a-a31c-4ee9-9728-0a97248a46375_id_seq" TO PUBLIC;

GRANT SELECT, INSERT ON "6f95f983-a826-42e0-8e97-e224a546fe32" TO PUBLIC;
GRANT SELECT, USAGE ON SEQUENCE "6f95f983-a826-42e0-8e97-e224a546fe32_id_seq" TO PUBLIC;
