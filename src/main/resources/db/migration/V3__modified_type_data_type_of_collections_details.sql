CREATE TYPE item_type AS ENUM ('OGC_FEATURES', 'OGC_TILES', 'STAC');

ALTER TABLE collections_details ADD COLUMN temp_item_type item_type NOT NULL;

UPDATE collections_details
SET item_type = CASE
    WHEN type = 'Feature' THEN 'OGC_FEATURES'::item_type
    WHEN type = 'Tiles' THEN 'OGC_TILES'::item_type
    WHEN type = 'Stac' THEN 'STAC'::item_type
    ELSE NULL
END;

ALTER TABLE collections_details DROP COLUMN type;

ALTER TABLE collections_details RENAME COLUMN temp_item_type TO type;

Alter TABLE collections_details ADD COLUMN license VARCHAR(255);
