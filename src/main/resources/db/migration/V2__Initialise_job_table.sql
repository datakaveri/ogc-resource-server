CREATE TYPE job_status_type AS ENUM ('ACCEPTED', 'RUNNING', 'SUCCESSFUL', 'FAILED', 'DISMISSED');

CREATE TYPE job_type AS ENUM ('PROCESS');

CREATE TABLE IF NOT EXISTS jobs_table
(
	id uuid DEFAULT public.gen_random_uuid(),
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
	CONSTRAINT process_id_fk FOREIGN KEY(process_id) REFERENCES processes_table(id)
);

ALTER TABLE jobs_table OWNER TO ${flyway:user};

