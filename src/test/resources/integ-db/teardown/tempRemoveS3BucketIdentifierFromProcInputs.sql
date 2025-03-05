UPDATE processes_table SET input = jsonb_set(input, '{inputs}', (input -> 'inputs') - 's3BucketIdentifier');
