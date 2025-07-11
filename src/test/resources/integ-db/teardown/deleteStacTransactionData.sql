ALTER TABLE stac_collections_part DETACH PARTITION "0bfd0f9a-a31c-4ee9-9728-0a97248a4637";
DROP TABLE "0bfd0f9a-a31c-4ee9-9728-0a97248a4637";

ALTER TABLE stac_collections_part DETACH PARTITION "6f95f983-a826-42e0-8e97-e224a546fe32";
DROP TABLE "6f95f983-a826-42e0-8e97-e224a546fe32";


DELETE FROM stac_items_assets where item_id like '%testing_stac_item%';
DELETE FROM stac_collections_part where id like '%testing_stac_item%';


DELETE FROM ri_details WHERE id = '6f95f983-a826-42e0-8e97-e224a546fe32'::uuid OR id = '0bfd0f9a-a31c-4ee9-9728-0a97248a4637'::uuid;
DELETE FROM collection_type WHERE id ='0bfd0f9a-a31c-4ee9-9728-0a97248a4637'::uuid OR id = '6f95f983-a826-42e0-8e97-e224a546fe32'::uuid;
DELETE FROM collections_details WHERE id = '0bfd0f9a-a31c-4ee9-9728-0a97248a4637'::uuid OR id = '6f95f983-a826-42e0-8e97-e224a546fe32'::uuid;