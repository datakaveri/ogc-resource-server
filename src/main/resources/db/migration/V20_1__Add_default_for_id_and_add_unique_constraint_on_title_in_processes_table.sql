-- set default of random UUID for processes_table.id
ALTER TABLE processes_table ALTER COLUMN id SET DEFAULT public.gen_random_uuid();

-- add unique constraint on title field since that's what's used everywhere
ALTER TABLE processes_table ADD UNIQUE (title);

