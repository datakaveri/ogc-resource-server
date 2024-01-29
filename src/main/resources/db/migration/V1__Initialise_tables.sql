CREATE TYPE status_type AS ENUM ('ACCEPTED', 'RUNNING', 'SUCCESSFUL', 'FAILED', 'DISMISSED');

CREATE type job_type as ENUM ('PROCESS');

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

create table if not exists jobs_table
(

	id uuid DEFAULT uuid_generate_v4() NOT NULL,
	process_id uuid NOT NULL,
	user_id uuid NOT NULL,
	created_at timestamp without time zone NOT NULL,
	updated_at timestamp without time zone NOT NULL,
	started_at timestamp without time zone DEFAULT NULL,
	finished_at timestamp without time zone DEFAULT NULL,
	input jsonb,
	output jsonb,
	progress decimal(4,2),
	status status_type NOT NULL,
	type job_type NOT NULL,
	message varchar,
	CONSTRAINT job_id_pk PRIMARY KEY (id),
	CONSTRAINT process_id_fk FOREIGN KEY(process_id) REFERENCES processes_table(id),

);

ALTER TABLE jobs_table OWNER TO ${flyway:user};


---
-- Functions for audit[created,updated,started,finished] on table/column
---

-- updated_at column function
create
or replace
   function update_modified () RETURNS trigger AS $$
begin NEW.updated_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';

-- created_at column function
create
or replace
   function update_created () RETURNS trigger AS $$
begin NEW.created_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE OR REPLACE FUNCTION update_started()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'RUNNING' AND (OLD.status IS NULL OR OLD.status = 'STARTED') THEN
        NEW.started_at = NOW();
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';


-- finished_at column function
CREATE OR REPLACE FUNCTION update_finished()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'FINISHED' AND OLD.status = 'RUNNING' THEN
        NEW.finished_at = NOW();
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';


-- job_table triggers

CREATE TRIGGER trigger_update_created_at
BEFORE INSERT ON jobs_table
FOR EACH ROW
EXECUTE PROCEDURE update_created();

CREATE TRIGGER trigger_update_modified_at
BEFORE INSERT OR UPDATE ON jobs_table
FOR EACH ROW
EXECUTE PROCEDURE update_modified();

CREATE TRIGGER trigger_update_started_at
BEFORE UPDATE ON jobs_table
FOR EACH ROW
WHEN (NEW.status <> OLD.status) -- This line ensures the trigger only fires when status changes
EXECUTE FUNCTION update_started();


CREATE TRIGGER trigger_update_finished_at
BEFORE UPDATE ON jobs_table
FOR EACH ROW
WHEN (NEW.status <> OLD.status) -- This line ensures the trigger only fires when status changes
EXECUTE FUNCTION update_finished();

