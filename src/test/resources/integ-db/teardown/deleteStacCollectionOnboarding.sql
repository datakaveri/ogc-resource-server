ALTER TABLE stac_collections_part DETACH PARTITION "ac14db94-4e9a-4336-9bec-072d37c0360e";
DROP TABLE "ac14db94-4e9a-4336-9bec-072d37c0360e";
delete from ri_details where id = 'ac14db94-4e9a-4336-9bec-072d37c0360e';
--delete from roles where user_id = '0ff3d306-9402-4430-8e18-6f95e4c03c97';
delete from collection_type where id ='ac14db94-4e9a-4336-9bec-072d37c0360e';
delete from collections_Details where id = 'ac14db94-4e9a-4336-9bec-072d37c0360e';

ALTER TABLE stac_collections_part DETACH PARTITION "0473a68a-c66a-42fb-93e3-ae9fd4c6e7dd";
DROP TABLE "0473a68a-c66a-42fb-93e3-ae9fd4c6e7dd";
delete from ri_details where id = '0473a68a-c66a-42fb-93e3-ae9fd4c6e7dd';
--delete from roles where user_id = '0ff3d306-9402-4430-8e18-6f95e4c03c97';
delete from collection_type where id ='0473a68a-c66a-42fb-93e3-ae9fd4c6e7dd';
delete from collections_Details where id = '0473a68a-c66a-42fb-93e3-ae9fd4c6e7dd';
