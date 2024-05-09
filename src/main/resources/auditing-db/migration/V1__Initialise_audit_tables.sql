---
-- ogc audit table
---

CREATE TABLE IF NOT EXISTS auditing_ogc
(
    id VARCHAR DEFAULT NOT NULL PRIMARY KEY,
    userid UUID DEFAULT NOT NULL,
    api VARCHAR DEFAULT NOT NULL,
    request_json JSON DEFAULT NOT NULL,
    resourceid UUID DEFAULT NOT NULL,
    providerid UUID DEFAULT NOT NULL,
    resource_group UUID DEFAULT NOT NULL,
    epochtime NUMERIC NOT NULL,
    time TIMESTAMP WITHOUT TIME ZONE,
    isotime VARCHAR DEFAULT NOT NULL,
    size NUMERIC NOT NULL,
    delegator_id UUID NOT NULL
);

CREATE INDEX ogc_userid_index ON auditing_ogc USING HASH (userid);
CREATE INDEX ogc_providerid_index ON auditing_ogc USING HASH (providerid);
CREATE INDEX ogc_resourceid_index on auditing_ogc (resourceid,epochtime);
CREATE INDEX ogc_time_index ON auditing_ogc (time);


ALTER TABLE auditing_ogc OWNER TO ${flyway:user};

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};
GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE auditing_ogc TO ${rsUser};