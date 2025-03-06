UPDATE processes_table SET input = jsonb_set(input, '{inputs}', (input -> 'inputs') - 'bucketName' - 'region') WHERE title = 'S3PreSignedURLGeneration';
