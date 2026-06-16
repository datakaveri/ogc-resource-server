-- Adds the Maps Onboarding process to the processes table
INSERT INTO processes_table (
    description,
    input,
    output,
    subscriber,
    title,
    version,
    keywords,
    mode,
    response
) VALUES (
    'Process to onboard a COG map collection for OGC API Maps core',
    '{"inputs": {"resourceId": "resource-Id", "title": "title-of-map-collection", "description": "description-of-map-collection", "fileName": "s3-object-key-to-cog.tif", "s3BucketIdentifier": "default", "temporal": ["start-date-time", "end-date-time"], "version": "1.0.0", "wmsUrl": "optional-wms-url"}}'::jsonb,
    '{"type": "type of the job created", "jobId": "jobId once created", "status": "status of the job", "processId": "process id for which the job is created"}'::jsonb,
    'MapCollectionOnboarding',
    'MapCollectionOnboarding',
    '1.0.0',
    ARRAY['map', 'collection', 'onboarding', 'cog'],
    ARRAY['async-execute']::execution_mode[],
    ARRAY['value']::transmission_mode[]
) ON CONFLICT (title) DO NOTHING;
