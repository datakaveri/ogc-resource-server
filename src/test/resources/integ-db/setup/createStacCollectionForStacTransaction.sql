INSERT INTO collections_details (id, title, description, crs)
 VALUES ('0bfd0f9a-a31c-4ee9-9728-0a97248a4637'::uuid
, 'Test Collection for STAC transaction'
, 'A short description'
, 'http://www.opengis.net/def/crs/OGC/1.3/CRS84')
, ('6f95f983-a826-42e0-8e97-e224a546fe32'::uuid
  , 'Another Test Collection for STAC transaction'
  , 'A short description'
  , 'http://www.opengis.net/def/crs/OGC/1.3/CRS84');

INSERT INTO ri_details (id, access, role_id)
 VALUES ('0bfd0f9a-a31c-4ee9-9728-0a97248a4637'::uuid,'OPEN','0ff3d306-9402-4430-8e18-6f95e4c03c97'::uuid)
 , ('6f95f983-a826-42e0-8e97-e224a546fe32'::uuid,'SECURE','0ff3d306-9402-4430-8e18-6f95e4c03c97'::uuid );


INSERT INTO collection_type (collection_id , type) VALUES ('0bfd0f9a-a31c-4ee9-9728-0a97248a4637'::uuid, 'STAC')
, ('6f95f983-a826-42e0-8e97-e224a546fe32'::uuid, 'STAC');

CREATE TABLE "0bfd0f9a-a31c-4ee9-9728-0a97248a4637" (LIKE stac_collections_part including defaults including CONSTRAINTS);
ALTER TABLE stac_collections_part attach partition "0bfd0f9a-a31c-4ee9-9728-0a97248a4637"
 FOR VALUES IN ('0bfd0f9a-a31c-4ee9-9728-0a97248a4637'::uuid);

CREATE TABLE "6f95f983-a826-42e0-8e97-e224a546fe32" (LIKE stac_collections_part including defaults including CONSTRAINTS);
ALTER TABLE stac_collections_part attach partition "6f95f983-a826-42e0-8e97-e224a546fe32"
 FOR VALUES IN ('6f95f983-a826-42e0-8e97-e224a546fe32'::uuid);

-- grant select access to all users
GRANT SELECT, INSERT ON "0bfd0f9a-a31c-4ee9-9728-0a97248a4637" TO PUBLIC;

GRANT SELECT, INSERT ON "6f95f983-a826-42e0-8e97-e224a546fe32" TO PUBLIC;
