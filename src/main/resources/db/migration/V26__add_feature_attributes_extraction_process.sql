-- Migration to insert FeatureAttributesExtraction process into processes_table

INSERT INTO processes_table (
  id,
  description,
  input,
  output,
  title,
  version,
  keywords,
  response,
  mode
)
VALUES (
  'e885d45d-5334-4755-aad2-67ed6ac1b989',
  'Process to extract attributes of a feature collection',
  '{
    "inputs": {
      "collectionId": "resource-id",
      "attributes": ["attr1","attr2","attr3"]
    }
  }'::jsonb,
  '{
    "features": [
      {
        "id": 1,
        "properties": {
          "attr1": "value",
          "attr2": "value",
          "attr3": "value"
        }
      },
      {
        "id": 2,
        "properties": {
          "attr1": "value",
          "attr2": "value",
          "attr3": "value"
        }
      }
    ],
    "status": "SUCCESSFUL"
  }'::jsonb,
  'FeatureAttributesExtraction',
  '1.0.0',
  ARRAY['feature','attributes','extraction'],
  ARRAY['SYNC']::execution_mode[],
  ARRAY['VALUE']::transmission_mode[]
);
