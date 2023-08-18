---
-- ogc audit table
---

CREATE TABLE IF NOT EXISTS auditing_ogc
(
   id varchar NOT NULL,
   api varchar NOT NULL,
   userid varchar NOT NULL,
   epochtime numeric NOT NULL,
   resourceid varchar NOT NULL,
   providerid varchar NOT NULL,
   itemType varchar NOT NULL,
   size numeric NOT NULL,
   time timestamp without time zone,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL
);
---
-- Functions for audit[created,updated] on table/column
---

-- modified_at column function
CREATE
OR REPLACE
   FUNCTION update_modified () RETURNS TRIGGER AS $$
BEGIN NEW.modified_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';

-- created_at column function
CREATE
OR REPLACE
   FUNCTION update_created () RETURNS TRIGGER AS $$
BEGIN NEW.created_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_ogc_created BEFORE INSERT ON auditing_ogc FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_ogc_modified BEFORE INSERT
OR UPDATE ON
   auditing_ogc FOR EACH ROW EXECUTE PROCEDURE update_modified ();

-- Add  indexes for auditing_ogc.
CREATE INDEX ogc_userid_index ON auditing_ogc USING HASH (userid);
CREATE INDEX ogc_providerid_index ON auditing_ogc USING HASH (providerid);
CREATE INDEX ogc_resourceid_index on auditing_ogc (resourceid,epochtime);

ALTER TABLE auditing_ogc OWNER TO ${flyway:user};
GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${ogcUser};

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE auditing_ogc TO ${ogcUser};
