-- Create a temporary new ENUM type with the desired values.
-- We use a new name to avoid conflicts with the old type.
CREATE TYPE access_enum_new AS ENUM (
    'PRIVATE',
    'OPEN',
    'RESTRICTED'
);

-- Add a new column to the table that will use the new ENUM type.
ALTER TABLE ri_details ADD COLUMN access_new access_enum_new;

-- Update the new column with data from the old column, mapping the old values to the new ones.
-- We use a CASE statement to handle the mapping, especially changing 'SECURE' to 'RESTRICTED'.
UPDATE ri_details
SET access_new = CASE
    WHEN access = 'OPEN' THEN 'OPEN'::access_enum_new
    WHEN access = 'SECURE' THEN 'RESTRICTED'::access_enum_new
    WHEN access = 'PRIVATE' THEN 'PRIVATE'::access_enum_new
    ELSE NULL
END;

-- Drop the old column from the table.
ALTER TABLE ri_details DROP COLUMN access;

-- Rename the new column to the old column's name.
ALTER TABLE ri_details RENAME COLUMN access_new TO access;

-- Drop the old ENUM type since it's no longer in use.
DROP TYPE access_enum;

-- Set the new default value for the column.
-- It's crucial to cast to the new enum type here.
ALTER TABLE ri_details ALTER COLUMN access SET DEFAULT 'RESTRICTED'::access_enum_new;


GRANT ALL PRIVILEGES ON SCHEMA public TO  ${ogcUser};
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE stac_items_assets TO ${ogcUser};
