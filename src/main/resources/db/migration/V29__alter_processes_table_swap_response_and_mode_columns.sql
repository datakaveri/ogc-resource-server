BEGIN;

-- 1. Rename the old columns
ALTER TABLE processes_table RENAME COLUMN response TO old_response;
ALTER TABLE processes_table RENAME COLUMN mode TO old_mode;

-- 2. Add new columns
ALTER TABLE processes_table ADD COLUMN response transmission_mode[];
ALTER TABLE processes_table ADD COLUMN mode execution_mode[];

-- 3. Migrate old_mode → response
UPDATE processes_table
SET response = old_mode;

-- 4. Migrate and transform old_response → mode
UPDATE processes_table
SET mode = (
  SELECT ARRAY(
    SELECT CASE
      WHEN val = 'SYNC' THEN 'sync-execute'
      WHEN val = 'ASYNC' THEN 'async-execute'
      ELSE LOWER(val::text)::execution_mode
    END
    FROM unnest(old_response) AS val
  )
);

-- 5. Drop old columns
ALTER TABLE processes_table DROP COLUMN old_response;
ALTER TABLE processes_table DROP COLUMN old_mode;

COMMIT;
