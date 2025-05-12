-- remove from presigned URL process
UPDATE processes_table SET input = jsonb_set(input, '{inputs}', (input -> 'inputs') - 'bucketName' - 'region') WHERE title = 'S3PreSignedURLGeneration';

-- remove from multipart processes
UPDATE processes_table SET input = jsonb_set(input, '{inputs}', (input -> 'inputs') - 'bucketName') WHERE title = 'S3InitiateMultipartUpload' OR title = 'S3CompleteMultipartUpload';
