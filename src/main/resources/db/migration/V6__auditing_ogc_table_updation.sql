ALTER TABLE auditing_ogc
DROP COLUMN itemtype;

ALTER TABLE auditing_ogc
ADD COLUMN request_json json NOT NULL DEFAULT '{}',
ADD COLUMN delegator_id uuid NOT NULL,
ADD COLUMN resource_group uuid NOT NULL,
Add COLUMN isotime varchar NOT NULL;

