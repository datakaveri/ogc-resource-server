SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: ${flyway:user}
--
---
-- ${flyway:user} is a superuser for executing all the DDL commands
-- ${ogcUser} is a restricted privilege user; used by the OGC-RS Server
---
ALTER SCHEMA ${flyway:defaultSchema} OWNER TO ${flyway:user};

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${ogcUser};

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry and geography spatial types and functions';


--
-- Name: access_enum; Type: TYPE; Schema: -; Owner: iudx_gis_user
--

CREATE TYPE access_enum AS ENUM (
    'OPEN',
    'SECURE',
    'PRIVATE'
);


ALTER TYPE access_enum OWNER TO ${flyway:user};

--
-- Name: execution_mode; Type: TYPE; Schema: -; Owner: iudx_gis_user
--

CREATE TYPE execution_mode AS ENUM (
    'async',
    'sync'
);


ALTER TYPE execution_mode OWNER TO ${flyway:user};

--
-- Name: role_enum; Type: TYPE; Schema: -; Owner: iudx_gis_user
--

CREATE TYPE role_enum AS ENUM (
    'PROVIDER',
    'CONSUMER',
    'DELEGATE'
);


ALTER TYPE role_enum OWNER TO ${flyway:user};

--
-- Name: transmission_mode; Type: TYPE; Schema: -; Owner: iudx_gis_user
--

CREATE TYPE transmission_mode AS ENUM (
    'value',
    'reference'
);


ALTER TYPE transmission_mode OWNER TO ${flyway:user};

--
-- Name: get_geom_values(); Type: FUNCTION; Schema: schema; Owner: iudx_gis_user
--

CREATE FUNCTION get_geom_values() RETURNS TABLE(id uuid, geom geometry)
    LANGUAGE plpgsql
    AS $$
declare
  table_name text;
  query_text text;
  record_row RECORD;
begin
  for record_row in select id from collections_details
  LOOP
   table_name := record_row.id::text;
   query_text := 'SELECT id, geom FROM' || table_name || 'AS dynamic_table' ;
   return QUERY EXECUTE query_text;
  end LOOP;
END;
$$;


ALTER FUNCTION get_geom_values() OWNER TO ${flyway:user};

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ac; Type: TABLE; Schema: ; Owner: iudx_gis_user
--

CREATE TABLE ac (
    id uuid DEFAULT public.gen_random_uuid(),
    geom geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'Feature'::character varying
);


ALTER TABLE ac OWNER TO ${flyway:user};

--
-- Name: ri_details; Type: TABLE; Schema: -; Owner: iudx_gis_user
--

CREATE TABLE ri_details (
    id uuid NOT NULL,
    role_id uuid NOT NULL,
    access access_enum DEFAULT 'SECURE'::access_enum
);


ALTER TABLE ri_details OWNER TO ${flyway:user};
--
-- Name: auditing_ogc; Type: TABLE; Schema: -; Owner: postgres
--
CREATE TABLE auditing_ogc (
    id character varying NOT NULL,
    api character varying NOT NULL,
    userid character varying NOT NULL,
    epochtime numeric NOT NULL,
    resourceid character varying NOT NULL,
    providerid character varying NOT NULL,
    itemtype character varying NOT NULL,
    size numeric NOT NULL,
    "time" timestamp without time zone
);


ALTER TABLE auditing_ogc OWNER TO ${flyway:user};

--
-- Name: catalog_test; Type: TABLE; Schema: -; Owner: iudx_gis_user
--

CREATE TABLE catalog_test (
    type character varying(255),
    stac_version character varying(255),
    id character varying(255),
    title character varying(255),
    description character varying(255)
);


ALTER TABLE catalog_test OWNER TO ${flyway:user};

--
-- Name: collection_supported_crs; Type: TABLE; Schema: -; Owner: iudx_gis_user
--

CREATE TABLE collection_supported_crs (
    id integer NOT NULL,
    collection_id uuid NOT NULL,
    crs_id integer NOT NULL
);


ALTER TABLE collection_supported_crs OWNER TO ${flyway:user};

--
-- Name: collection_supported_crs_id_seq; Type: SEQUENCE; Schema: -; Owner: iudx_gis_user
--

CREATE SEQUENCE collection_supported_crs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE collection_supported_crs_id_seq OWNER TO ${flyway:user};

--
-- Name: collection_supported_crs_id_seq; Type: SEQUENCE OWNED BY; Schema: -; Owner: iudx_gis_user
--

ALTER SEQUENCE collection_supported_crs_id_seq OWNED BY collection_supported_crs.id;


--
-- Name: collections_details; Type: TABLE; Schema: -; Owner: ${flyway:user}
--

CREATE TABLE collections_details (
    id uuid DEFAULT public.gen_random_uuid() NOT NULL,
    title character varying(50) DEFAULT ''::character varying,
    description character varying(100) DEFAULT ''::character varying,
    datetime_key character varying(50),
    crs character varying(50) DEFAULT 'CRS84'::character varying,
    type character varying(20) DEFAULT 'Feature'::character varying,
    bbox numeric[],
    temporal text[]
);


ALTER TABLE collections_details OWNER TO ${flyway:user};

--
-- Name: crs_to_srid; Type: TABLE; Schema: -; Owner: iudx_gis_user
--

CREATE TABLE crs_to_srid (
    id integer NOT NULL,
    crs character varying(100) NOT NULL,
    srid integer NOT NULL
);


ALTER TABLE crs_to_srid OWNER TO ${flyway:user};

--
-- Name: crs_to_srid_id_seq; Type: SEQUENCE; Schema: -; Owner: iudx_gis_user
--

CREATE SEQUENCE crs_to_srid_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE crs_to_srid_id_seq OWNER TO ${flyway:user};

--
-- Name: crs_to_srid_id_seq; Type: SEQUENCE OWNED BY; Schema: -; Owner: iudx_gis_user
--

ALTER SEQUENCE crs_to_srid_id_seq OWNED BY crs_to_srid.id;


--
-- Name: processes_table; Type: TABLE; Schema: -; Owner: iudx_gis_user
--

CREATE TABLE processes_table (
    id uuid NOT NULL,
    description character varying,
    input jsonb,
    output jsonb,
    subscriber character varying,
    title character varying,
    version character varying,
    keywords character varying[],
    response execution_mode[],
    mode transmission_mode[]
);


ALTER TABLE processes_table OWNER TO ${flyway:user};

--
-- Name: roles; Type: TABLE; Schema: -; Owner: iudx_gis_user
--

CREATE TABLE roles (
    user_id uuid NOT NULL,
    role role_enum NOT NULL
);


ALTER TABLE roles OWNER TO ${flyway:user};

--
-- Name: tilematrixset_metadata; Type: TABLE; Schema: -; Owner: iudx_gis_user
--

CREATE TABLE tilematrixset_metadata (
    id uuid DEFAULT public.gen_random_uuid(),
    title character varying(20),
    description character varying(50),
    scaledenominator numeric,
    cellsize numeric,
    corneroforigin character varying(20),
    matrixwidth integer,
    matrixheight integer,
    tilematrix_id integer NOT NULL,
    tilematrixset_id character varying(50) NOT NULL
);


ALTER TABLE tilematrixset_metadata OWNER TO ${flyway:user};

--
-- Name: tilematrixsets_relation; Type: TABLE; Schema: -; Owner: iudx_gis_user
--

CREATE TABLE tilematrixsets_relation (
    collection_id uuid,
    id character varying(50) DEFAULT public.gen_random_uuid(),
    title character varying(50),
    datatype character varying(10),
    uri character varying(100),
    pointoforigin numeric[],
    tilewidth integer,
    tileheight integer,
    crs character varying(100)
);


ALTER TABLE tilematrixsets_relation OWNER TO ${flyway:user};

--
-- Name: collection_supported_crs id; Type: DEFAULT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY collection_supported_crs ALTER COLUMN id SET DEFAULT nextval('collection_supported_crs_id_seq'::regclass);


--
-- Name: crs_to_srid id; Type: DEFAULT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY crs_to_srid ALTER COLUMN id SET DEFAULT nextval('crs_to_srid_id_seq'::regclass);


--
-- Name: collection_supported_crs collection_supported_crs_pkey; Type: CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY collection_supported_crs
    ADD CONSTRAINT collection_supported_crs_pkey PRIMARY KEY (id);


--
-- Name: collections_details collections_details_pkey; Type: CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY collections_details
    ADD CONSTRAINT collections_details_pkey PRIMARY KEY (id);


--
-- Name: crs_to_srid crs_to_srid_pkey; Type: CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY crs_to_srid
    ADD CONSTRAINT crs_to_srid_pkey PRIMARY KEY (id);


--
-- Name: crs_to_srid crs_unique; Type: CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY crs_to_srid
    ADD CONSTRAINT crs_unique UNIQUE (crs);


--
-- Name: processes_table processes_table_pkey; Type: CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY processes_table
    ADD CONSTRAINT processes_table_pkey PRIMARY KEY (id);


--
-- Name: ri_details ri_id_pk; Type: CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY ri_details
    ADD CONSTRAINT ri_id_pk PRIMARY KEY (id);


--
-- Name: tilematrixset_metadata tilematrixset_metadata_pkey; Type: CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY tilematrixset_metadata
    ADD CONSTRAINT tilematrixset_metadata_pkey PRIMARY KEY (tilematrixset_id, tilematrix_id);


--
-- Name: roles users_pk; Type: CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY roles
    ADD CONSTRAINT users_pk PRIMARY KEY (user_id);


--
-- Name: tilematrixsets_relation collection_fkey; Type: FK CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY tilematrixsets_relation
    ADD CONSTRAINT collection_fkey FOREIGN KEY (collection_id) REFERENCES collections_details(id);


--
-- Name: collection_supported_crs collection_supported_crs_collection_id_fkey; Type: FK CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY collection_supported_crs
    ADD CONSTRAINT collection_supported_crs_collection_id_fkey FOREIGN KEY (collection_id) REFERENCES collections_details(id);


--
-- Name: collection_supported_crs collection_supported_crs_crs_id_fkey; Type: FK CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY collection_supported_crs
    ADD CONSTRAINT collection_supported_crs_crs_id_fkey FOREIGN KEY (crs_id) REFERENCES crs_to_srid(id);


--
-- Name: collections_details crs_fk; Type: FK CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY collections_details
    ADD CONSTRAINT crs_fk FOREIGN KEY (crs) REFERENCES crs_to_srid(crs);


--
-- Name: ri_details rg_fk; Type: FK CONSTRAINT; Schema: -; Owner: iudx_gis_user
--

ALTER TABLE ONLY ri_details
    ADD CONSTRAINT users_fk FOREIGN KEY (role_id) REFERENCES roles(user_id);

--
-- Name: TABLE auditing_ogc; Type: ACL; Schema: -; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE auditing_ogc TO ${flyway:user};

