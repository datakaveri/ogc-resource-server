-- temporal and wmsUrl are optional at execute time; remove from required input schema.
UPDATE processes_table
SET input = jsonb_set(
    input,
    '{inputs}',
    (input->'inputs') - 'temporal' - 'wmsUrl'
)
WHERE title = 'MapCollectionOnboarding';
