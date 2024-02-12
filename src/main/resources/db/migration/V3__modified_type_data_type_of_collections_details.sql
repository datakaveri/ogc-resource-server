CREATE TYPE item_type AS ENUM ('FEATURE', 'MAP', 'STAC');

ALTER TABLE collections_details ADD COLUMN temp_item_type item_type NOT NULL;

UPDATE collections_details
SET item_type = CASE
    WHEN type = 'Feature' THEN 'FEATURE'::item_type
    WHEN type = 'Tiles' THEN 'MAP'::item_type
    WHEN type = 'Stac' THEN 'STAC'::item_type
    ELSE NULL
END;

ALTER TABLE collections_details DROP COLUMN type;

ALTER TABLE collections_details RENAME COLUMN temp_item_type TO type;


