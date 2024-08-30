DROP TABLE IF EXISTS "2cfc08b8-a43d-40d4-ba98-c6fdfa76a0c1";
DELETE FROM ri_details WHERE id = '2cfc08b8-a43d-40d4-ba98-c6fdfa76a0c1'; 
DELETE FROM stac_collections_assets WHERE stac_collections_id = '2cfc08b8-a43d-40d4-ba98-c6fdfa76a0c1';
DELETE FROM collections_details WHERE id = '2cfc08b8-a43d-40d4-ba98-c6fdfa76a0c1';

DROP TABLE IF EXISTS "5d568f26-ccaf-456d-ba04-7feb589c1185";
DELETE FROM ri_details WHERE id = '5d568f26-ccaf-456d-ba04-7feb589c1185';
DELETE FROM stac_collections_assets WHERE stac_collections_id = '5d568f26-ccaf-456d-ba04-7feb589c1185';
DELETE FROM collections_details WHERE id = '5d568f26-ccaf-456d-ba04-7feb589c1185';
