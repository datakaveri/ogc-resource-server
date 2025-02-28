UPDATE processes_table SET input = jsonb_set(input, '{inputs}', input -> 'inputs' || '{"region": "AWS S3 Region", "bucketName": "AWS S3 Bucket Name"}') WHERE title = 'S3PreSignedURLGeneration';

