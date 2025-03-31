UPDATE processes_table SET input = jsonb_set(input, '{inputs}', input -> 'inputs' || '{"bucketName": "AWS S3 Bucket Name"}') WHERE title = 'S3InitiateMultipartUpload' OR title = 'S3CompleteMultipartUpload';

