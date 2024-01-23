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
-- Name: access_enum; Type: TYPE; Schema: public; Owner: iudx_gis_user
--

CREATE TYPE access_enum AS ENUM (
    'OPEN',
    'SECURE',
    'PRIVATE'
);


ALTER TYPE access_enum OWNER TO ${flyway:user};

--
-- Name: execution_mode; Type: TYPE; Schema: public; Owner: iudx_gis_user
--

CREATE TYPE execution_mode AS ENUM (
    'async',
    'sync'
);


ALTER TYPE execution_mode OWNER TO ${flyway:user};

--
-- Name: role_enum; Type: TYPE; Schema: public; Owner: iudx_gis_user
--

CREATE TYPE role_enum AS ENUM (
    'PROVIDER',
    'CONSUMER',
    'DELEGATE'
);


ALTER TYPE role_enum OWNER TO ${flyway:user};

--
-- Name: transmission_mode; Type: TYPE; Schema: public; Owner: iudx_gis_user
--

CREATE TYPE transmission_mode AS ENUM (
    'value',
    'reference'
);


ALTER TYPE transmission_mode OWNER TO ${flyway:user};

--
-- Name: get_geom_values(); Type: FUNCTION; Schema: public; Owner: iudx_gis_user
--

CREATE FUNCTION public.get_geom_values() RETURNS TABLE(id uuid, geom public.geometry)
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


ALTER FUNCTION public.get_geom_values() OWNER TO ${flyway:user};

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: 0b94ed06-718a-439c-bb74-012beda7c102; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "0b94ed06-718a-439c-bb74-012beda7c102" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "0b94ed06-718a-439c-bb74-012beda7c102" OWNER TO ${flyway:user};

--
-- Name: 17aa4b7c-2e10-442f-a7a1-1677bd623593; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "17aa4b7c-2e10-442f-a7a1-1677bd623593" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    id uuid DEFAULT public.gen_random_uuid(),
    itemtype character varying(20) DEFAULT 'feature'::character varying
);


ALTER TABLE "17aa4b7c-2e10-442f-a7a1-1677bd623593" OWNER TO ${flyway:user};

--
-- Name: 1de39c53-e68b-47f6-88d5-ab21f43ad08e; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "1de39c53-e68b-47f6-88d5-ab21f43ad08e" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "1de39c53-e68b-47f6-88d5-ab21f43ad08e" OWNER TO ${flyway:user};

--
-- Name: 1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a" (
    id uuid DEFAULT public.gen_random_uuid(),
    geom public.geometry(Geometry,27700),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'Feature'::character varying
);


ALTER TABLE "1e7f3be1-5d07-4cba-9c8c-5c3a2fd5c82a" OWNER TO ${flyway:user};

--
-- Name: 3d0c2c06-671d-4ed1-8b87-6242ae05f57c; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "3d0c2c06-671d-4ed1-8b87-6242ae05f57c" (
    type character varying(255),
    stac_version character varying(255),
    id uuid,
    geometry public.geometry(Point,4326),
    bbox numeric[],
    properties jsonb
);


ALTER TABLE "3d0c2c06-671d-4ed1-8b87-6242ae05f57c" OWNER TO ${flyway:user};

--
-- Name: 3f758964-acf2-44e9-ab78-ecc777a9a843; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "3f758964-acf2-44e9-ab78-ecc777a9a843" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "3f758964-acf2-44e9-ab78-ecc777a9a843" OWNER TO ${flyway:user};

--
-- Name: 50d73490-5722-4741-b11d-70bde5b1b5ba; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "50d73490-5722-4741-b11d-70bde5b1b5ba" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "50d73490-5722-4741-b11d-70bde5b1b5ba" OWNER TO ${flyway:user};

--
-- Name: 61fe61d8-f3ff-4138-96c6-d0a2694fce81; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "61fe61d8-f3ff-4138-96c6-d0a2694fce81" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "61fe61d8-f3ff-4138-96c6-d0a2694fce81" OWNER TO ${flyway:user};

--
-- Name: TABLE "61fe61d8-f3ff-4138-96c6-d0a2694fce81"; Type: COMMENT; Schema: public; Owner: iudx_gis_user
--

COMMENT ON TABLE "61fe61d8-f3ff-4138-96c6-d0a2694fce81" IS 'REQUIRED: A brief narrative summary of the data set.

REQUIRED: A summary of the intentions with which the data set was developed.';


--
-- Name: 63f6e7de-18a4-48e7-a671-af408abb2225; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "63f6e7de-18a4-48e7-a671-af408abb2225" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "63f6e7de-18a4-48e7-a671-af408abb2225" OWNER TO ${flyway:user};

--
-- Name: 6f38e831-f02f-4c55-959e-458585beb986; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "6f38e831-f02f-4c55-959e-458585beb986" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "6f38e831-f02f-4c55-959e-458585beb986" OWNER TO ${flyway:user};

--
-- Name: 6f8b98a9-0949-4daa-a490-6c5da220b815; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "6f8b98a9-0949-4daa-a490-6c5da220b815" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "6f8b98a9-0949-4daa-a490-6c5da220b815" OWNER TO ${flyway:user};

--
-- Name: 72f1e3d8-5d8a-4d29-934b-ee562ed44213; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "72f1e3d8-5d8a-4d29-934b-ee562ed44213" (
    type character varying,
    stac_version character varying,
    id uuid NOT NULL,
    title character varying,
    description text,
    license character varying
);


ALTER TABLE "72f1e3d8-5d8a-4d29-934b-ee562ed44213" OWNER TO ${flyway:user};

--
-- Name: 731da4c7-b505-49e4-acbe-77ab43335023; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "731da4c7-b505-49e4-acbe-77ab43335023" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "731da4c7-b505-49e4-acbe-77ab43335023" OWNER TO ${flyway:user};

--
-- Name: 7d08165b-b1b4-4bc7-ac00-b0840b09bcb0; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "7d08165b-b1b4-4bc7-ac00-b0840b09bcb0" (
    id uuid DEFAULT public.gen_random_uuid(),
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'Feature'::character varying
);


ALTER TABLE "7d08165b-b1b4-4bc7-ac00-b0840b09bcb0" OWNER TO ${flyway:user};

--
-- Name: 94e47087-481a-4898-b6ff-01ba13a374f0; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "94e47087-481a-4898-b6ff-01ba13a374f0" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "94e47087-481a-4898-b6ff-01ba13a374f0" OWNER TO ${flyway:user};

--
-- Name: 9bcad83e-47a4-45b3-8c80-f7c084810359; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "9bcad83e-47a4-45b3-8c80-f7c084810359" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "9bcad83e-47a4-45b3-8c80-f7c084810359" OWNER TO ${flyway:user};

--
-- Name: 9d4a038d-8139-423a-ac74-f360c4942026; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "9d4a038d-8139-423a-ac74-f360c4942026" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "9d4a038d-8139-423a-ac74-f360c4942026" OWNER TO ${flyway:user};

--
-- Name: 9f8b75fb-58be-432b-85ba-dde1d55a7d58; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "9f8b75fb-58be-432b-85ba-dde1d55a7d58" (
    geom public.geometry(Geometry,4326),
    id uuid DEFAULT public.gen_random_uuid() NOT NULL,
    properties jsonb DEFAULT '{}'::jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    datetime timestamp without time zone
);


ALTER TABLE "9f8b75fb-58be-432b-85ba-dde1d55a7d58" OWNER TO ${flyway:user};

--
-- Name: TABLE "9f8b75fb-58be-432b-85ba-dde1d55a7d58"; Type: COMMENT; Schema: public; Owner: iudx_gis_user
--

COMMENT ON TABLE "9f8b75fb-58be-432b-85ba-dde1d55a7d58" IS 'REQUIRED: A brief narrative summary of the data set.

REQUIRED: A summary of the intentions with which the data set was developed.';


--
-- Name: a1f6d6c9-8177-41cd-a13d-af04029d658e; Type: TABLE; Schema: public; Owner: ${flyway:user}
--

CREATE TABLE "a1f6d6c9-8177-41cd-a13d-af04029d658e" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "a1f6d6c9-8177-41cd-a13d-af04029d658e" OWNER TO ${flyway:user};

--
-- Name: a5a6e26f-d252-446d-b7dd-4d50ea945102; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "a5a6e26f-d252-446d-b7dd-4d50ea945102" (
    id uuid,
    properties jsonb,
    itemtype character varying(20),
    geom public.geometry(Geometry,4326)
);


ALTER TABLE "a5a6e26f-d252-446d-b7dd-4d50ea945102" OWNER TO ${flyway:user};

--
-- Name: ab1fbeea-3651-43e3-8e4f-90c9e86940a6; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "ab1fbeea-3651-43e3-8e4f-90c9e86940a6" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "ab1fbeea-3651-43e3-8e4f-90c9e86940a6" OWNER TO ${flyway:user};

--
-- Name: ac; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE ac (
    id uuid DEFAULT public.gen_random_uuid(),
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'Feature'::character varying
);


ALTER TABLE ac OWNER TO ${flyway:user};

--
-- Name: rg_details; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE rg_details (
    id uuid NOT NULL,
    role_id uuid NOT NULL,
    access access_enum DEFAULT 'SECURE'::access_enum
);


ALTER TABLE rg_details OWNER TO ${flyway:user};

--
-- Name: ri_details; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE ri_details (
    id uuid NOT NULL,
    rg_id uuid NOT NULL,
    access access_enum DEFAULT 'SECURE'::access_enum
);


ALTER TABLE ri_details OWNER TO ${flyway:user};

--
-- Name: access_view; Type: VIEW; Schema: public; Owner: iudx_gis_user
--

CREATE VIEW access_view AS
 SELECT rg_details.id,
    rg_details.access
   FROM rg_details
UNION
 SELECT ri_details.id,
    ri_details.access
   FROM ri_details;


ALTER TABLE access_view OWNER TO ${flyway:user};

--
-- Name: af1c526b-283e-441b-a4fb-09e6760de9c7; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "af1c526b-283e-441b-a4fb-09e6760de9c7" (
    geom public.geometry(Geometry,4326),
    id uuid DEFAULT public.gen_random_uuid() NOT NULL,
    properties jsonb DEFAULT '{}'::jsonb,
    itemtype character varying(20) DEFAULT 'Feature'::character varying
);


ALTER TABLE "af1c526b-283e-441b-a4fb-09e6760de9c7" OWNER TO ${flyway:user};

--
-- Name: auditing_ogc; Type: TABLE; Schema: public; Owner: postgres
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
-- Name: c08200c4-fff3-4808-afc4-d3b068bc723e; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "c08200c4-fff3-4808-afc4-d3b068bc723e" (
    geom public.geometry(Geometry,4326),
    id uuid DEFAULT public.gen_random_uuid() NOT NULL,
    properties jsonb DEFAULT '{}'::jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    datetime timestamp without time zone
);


ALTER TABLE "c08200c4-fff3-4808-afc4-d3b068bc723e" OWNER TO ${flyway:user};

--
-- Name: ca9fd590-b5ec-48f9-9036-46cd8b1048f6; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "ca9fd590-b5ec-48f9-9036-46cd8b1048f6" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "ca9fd590-b5ec-48f9-9036-46cd8b1048f6" OWNER TO ${flyway:user};

--
-- Name: catalog_test; Type: TABLE; Schema: public; Owner: iudx_gis_user
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
-- Name: collection_supported_crs; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE collection_supported_crs (
    id integer NOT NULL,
    collection_id uuid NOT NULL,
    crs_id integer NOT NULL
);


ALTER TABLE collection_supported_crs OWNER TO ${flyway:user};

--
-- Name: collection_supported_crs_id_seq; Type: SEQUENCE; Schema: public; Owner: iudx_gis_user
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
-- Name: collection_supported_crs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: iudx_gis_user
--

ALTER SEQUENCE collection_supported_crs_id_seq OWNED BY collection_supported_crs.id;


--
-- Name: collections_details; Type: TABLE; Schema: public; Owner: ${flyway:user}
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
-- Name: crs_to_srid; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE crs_to_srid (
    id integer NOT NULL,
    crs character varying(100) NOT NULL,
    srid integer NOT NULL
);


ALTER TABLE crs_to_srid OWNER TO ${flyway:user};

--
-- Name: crs_to_srid_id_seq; Type: SEQUENCE; Schema: public; Owner: iudx_gis_user
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
-- Name: crs_to_srid_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: iudx_gis_user
--

ALTER SEQUENCE crs_to_srid_id_seq OWNED BY crs_to_srid.id;

--
-- Name: fd6ef7c5-b18c-48e6-94e3-81037706763d; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "fd6ef7c5-b18c-48e6-94e3-81037706763d" (
    geom public.geometry(Geometry,4326),
    properties jsonb DEFAULT '{}'::jsonb NOT NULL,
    id uuid DEFAULT public.gen_random_uuid() NOT NULL,
    itemtype character varying DEFAULT 'feature'::character varying NOT NULL,
    geogc public.geography(Point,4326),
    datetime timestamp(0) with time zone DEFAULT now()
);


ALTER TABLE "fd6ef7c5-b18c-48e6-94e3-81037706763d" OWNER TO ${flyway:user};

--
-- Name: f64743f6-1f74-4ada-8020-45cfb2d08a1b; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "f64743f6-1f74-4ada-8020-45cfb2d08a1b" (
    geom public.geometry(Geometry,4326),
    id uuid DEFAULT public.gen_random_uuid() NOT NULL,
    properties jsonb DEFAULT '{}'::jsonb,
    itemtype character varying(20) DEFAULT 'Feature'::character varying
);


ALTER TABLE "f64743f6-1f74-4ada-8020-45cfb2d08a1b" OWNER TO ${flyway:user};

--
-- Name: fc6314a2-273b-447a-b9a0-22bf26b29389; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE "fc6314a2-273b-447a-b9a0-22bf26b29389" (
    geom public.geometry(Geometry,4326),
    properties jsonb,
    itemtype character varying(20) DEFAULT 'feature'::character varying,
    id uuid DEFAULT public.gen_random_uuid()
);


ALTER TABLE "fc6314a2-273b-447a-b9a0-22bf26b29389" OWNER TO ${flyway:user};

--
-- Name: processes_table; Type: TABLE; Schema: public; Owner: iudx_gis_user
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
-- Name: roles; Type: TABLE; Schema: public; Owner: iudx_gis_user
--

CREATE TABLE roles (
    user_id uuid NOT NULL,
    role role_enum NOT NULL
);


ALTER TABLE roles OWNER TO ${flyway:user};

--
-- Name: tilematrixset_metadata; Type: TABLE; Schema: public; Owner: iudx_gis_user
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
-- Name: tilematrixsets_relation; Type: TABLE; Schema: public; Owner: iudx_gis_user
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
-- Name: collection_supported_crs id; Type: DEFAULT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY collection_supported_crs ALTER COLUMN id SET DEFAULT nextval('collection_supported_crs_id_seq'::regclass);


--
-- Name: crs_to_srid id; Type: DEFAULT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY crs_to_srid ALTER COLUMN id SET DEFAULT nextval('crs_to_srid_id_seq'::regclass);


--
-- Name: af1c526b-283e-441b-a4fb-09e6760de9c7 af1c526b-283e-441b-a4fb-09e6760de9c7_pkey; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY "af1c526b-283e-441b-a4fb-09e6760de9c7"
    ADD CONSTRAINT "af1c526b-283e-441b-a4fb-09e6760de9c7_pkey" PRIMARY KEY (id);


--
-- Name: collection_supported_crs collection_supported_crs_pkey; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY collection_supported_crs
    ADD CONSTRAINT collection_supported_crs_pkey PRIMARY KEY (id);


--
-- Name: collections_details collections_details_pkey; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY collections_details
    ADD CONSTRAINT collections_details_pkey PRIMARY KEY (id);


--
-- Name: crs_to_srid crs_to_srid_pkey; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY crs_to_srid
    ADD CONSTRAINT crs_to_srid_pkey PRIMARY KEY (id);


--
-- Name: crs_to_srid crs_unique; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY crs_to_srid
    ADD CONSTRAINT crs_unique UNIQUE (crs);


--
-- Name: f64743f6-1f74-4ada-8020-45cfb2d08a1b f64743f6-1f74-4ada-8020-45cfb2d08a1b_pkey; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY "f64743f6-1f74-4ada-8020-45cfb2d08a1b"
    ADD CONSTRAINT "f64743f6-1f74-4ada-8020-45cfb2d08a1b_pkey" PRIMARY KEY (id);


--
-- Name: processes_table processes_table_pkey; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY processes_table
    ADD CONSTRAINT processes_table_pkey PRIMARY KEY (id);


--
-- Name: rg_details rg_id_pk; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY rg_details
    ADD CONSTRAINT rg_id_pk PRIMARY KEY (id);


--
-- Name: ri_details ri_id_pk; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY ri_details
    ADD CONSTRAINT ri_id_pk PRIMARY KEY (id);


--
-- Name: 72f1e3d8-5d8a-4d29-934b-ee562ed44213 stac_collecetion_test; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY "72f1e3d8-5d8a-4d29-934b-ee562ed44213"
    ADD CONSTRAINT stac_collecetion_test PRIMARY KEY (id);


--
-- Name: tilematrixset_metadata tilematrixset_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY tilematrixset_metadata
    ADD CONSTRAINT tilematrixset_metadata_pkey PRIMARY KEY (tilematrixset_id, tilematrix_id);


--
-- Name: roles users_pk; Type: CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY roles
    ADD CONSTRAINT users_pk PRIMARY KEY (user_id);


--
-- Name: a_hydro_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE UNIQUE INDEX a_hydro_id_idx ON "17aa4b7c-2e10-442f-a7a1-1677bd623593" USING btree (id);


--
-- Name: a_hydro_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX a_hydro_prop_idx ON "17aa4b7c-2e10-442f-a7a1-1677bd623593" USING gin (properties);


--
-- Name: a_hydrography_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX a_hydrography_geom_geom_idx ON "17aa4b7c-2e10-442f-a7a1-1677bd623593" USING gist (geom);


--
-- Name: bang_forest_18_19_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_18_19_id_idx ON "731da4c7-b505-49e4-acbe-77ab43335023" USING btree (id);


--
-- Name: bang_forest_18_19_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_18_19_prop_idx ON "731da4c7-b505-49e4-acbe-77ab43335023" USING gin (properties);


--
-- Name: bang_forest_19_20_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_19_20_id_idx ON "63f6e7de-18a4-48e7-a671-af408abb2225" USING btree (id);


--
-- Name: bang_forest_19_20_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_19_20_prop_idx ON "63f6e7de-18a4-48e7-a671-af408abb2225" USING gin (properties);


--
-- Name: bang_forest_20_21_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_20_21_id_idx ON "50d73490-5722-4741-b11d-70bde5b1b5ba" USING btree (id);


--
-- Name: bang_forest_20_21_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_20_21_prop_idx ON "50d73490-5722-4741-b11d-70bde5b1b5ba" USING gin (properties);


--
-- Name: bang_forest_21_22_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_21_22_id_idx ON "fc6314a2-273b-447a-b9a0-22bf26b29389" USING btree (id);


--
-- Name: bang_forest_21_22_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_21_22_prop_idx ON "fc6314a2-273b-447a-b9a0-22bf26b29389" USING gin (properties);


--
-- Name: bang_forest_22_23_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_22_23_id_idx ON "ca9fd590-b5ec-48f9-9036-46cd8b1048f6" USING btree (id);


--
-- Name: bang_forest_22_23_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bang_forest_22_23_prop_idx ON "ca9fd590-b5ec-48f9-9036-46cd8b1048f6" USING gin (properties);


--
-- Name: bangalore_forests_2018_2019_wkb_geometry_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bangalore_forests_2018_2019_wkb_geometry_geom_idx ON "731da4c7-b505-49e4-acbe-77ab43335023" USING gist (geom);


--
-- Name: bangalore_forests_2019_2020_wkb_geometry_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bangalore_forests_2019_2020_wkb_geometry_geom_idx ON "63f6e7de-18a4-48e7-a671-af408abb2225" USING gist (geom);


--
-- Name: bangalore_forests_2020_2021_wkb_geometry_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bangalore_forests_2020_2021_wkb_geometry_geom_idx ON "50d73490-5722-4741-b11d-70bde5b1b5ba" USING gist (geom);


--
-- Name: bangalore_forests_2021_2022_wkb_geometry_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bangalore_forests_2021_2022_wkb_geometry_geom_idx ON "fc6314a2-273b-447a-b9a0-22bf26b29389" USING gist (geom);


--
-- Name: bangalore_forests_2022_2023_wkb_geometry_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX bangalore_forests_2022_2023_wkb_geometry_geom_idx ON "ca9fd590-b5ec-48f9-9036-46cd8b1048f6" USING gist (geom);


--
-- Name: build_bangalore_google_props_gin_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX build_bangalore_google_props_gin_idx ON "af1c526b-283e-441b-a4fb-09e6760de9c7" USING gin (properties);


--
-- Name: build_bangalore_ms_props_gin_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX build_bangalore_ms_props_gin_idx ON "f64743f6-1f74-4ada-8020-45cfb2d08a1b" USING gin (properties);


--
-- Name: district_bound_props_gin_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX district_bound_props_gin_idx ON "c08200c4-fff3-4808-afc4-d3b068bc723e" USING gin (properties);


--
-- Name: district_boundaries_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX district_boundaries_geom_idx ON "c08200c4-fff3-4808-afc4-d3b068bc723e" USING gist (geom);


--
-- Name: district_hq_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX district_hq_geom_idx ON "fd6ef7c5-b18c-48e6-94e3-81037706763d" USING gist (geom);


--
-- Name: district_hq_props_gin_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX district_hq_props_gin_idx ON "fd6ef7c5-b18c-48e6-94e3-81037706763d" USING gin (properties);


--
-- Name: geography_points_gix; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX geography_points_gix ON "fd6ef7c5-b18c-48e6-94e3-81037706763d" USING gist (geogc);


--
-- Name: id_dis_bound_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX id_dis_bound_idx ON "c08200c4-fff3-4808-afc4-d3b068bc723e" USING btree (id);


--
-- Name: id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX id_idx ON "fd6ef7c5-b18c-48e6-94e3-81037706763d" USING btree (id);


--
-- Name: id_maj_town_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX id_maj_town_idx ON "9f8b75fb-58be-432b-85ba-dde1d55a7d58" USING btree (id);


--
-- Name: l_comm_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX l_comm_id_idx ON "1de39c53-e68b-47f6-88d5-ab21f43ad08e" USING btree (id);


--
-- Name: l_comm_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX l_comm_prop_idx ON "1de39c53-e68b-47f6-88d5-ab21f43ad08e" USING gin (properties);


--
-- Name: l_communications_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX l_communications_geom_geom_idx ON "1de39c53-e68b-47f6-88d5-ab21f43ad08e" USING gist (geom);


--
-- Name: l_hydro_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX l_hydro_id_idx ON "94e47087-481a-4898-b6ff-01ba13a374f0" USING btree (id);


--
-- Name: l_hydro_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX l_hydro_prop_idx ON "94e47087-481a-4898-b6ff-01ba13a374f0" USING gin (properties);


--
-- Name: l_hydrography_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX l_hydrography_geom_geom_idx ON "94e47087-481a-4898-b6ff-01ba13a374f0" USING gist (geom);


--
-- Name: major_towns_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX major_towns_geom_idx ON "9f8b75fb-58be-432b-85ba-dde1d55a7d58" USING gist (geom);


--
-- Name: major_towns_props_gin_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX major_towns_props_gin_idx ON "9f8b75fb-58be-432b-85ba-dde1d55a7d58" USING gin (properties);


--
-- Name: map_p_vegetation_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX map_p_vegetation_geom_geom_idx ON "9bcad83e-47a4-45b3-8c80-f7c084810359" USING gist (geom);


--
-- Name: map_vegetation_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX map_vegetation_id_idx ON "9bcad83e-47a4-45b3-8c80-f7c084810359" USING btree (id);


--
-- Name: map_vegetation_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX map_vegetation_prop_idx ON "9bcad83e-47a4-45b3-8c80-f7c084810359" USING gin (properties);


--
-- Name: p_build_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX p_build_id_idx ON "0b94ed06-718a-439c-bb74-012beda7c102" USING btree (id);


--
-- Name: p_build_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX p_build_prop_idx ON "0b94ed06-718a-439c-bb74-012beda7c102" USING gin (properties);


--
-- Name: p_buildings_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX p_buildings_geom_geom_idx ON "0b94ed06-718a-439c-bb74-012beda7c102" USING gist (geom);


--
-- Name: p_comm_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX p_comm_id_idx ON "a1f6d6c9-8177-41cd-a13d-af04029d658e" USING btree (id);


--
-- Name: p_comm_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX p_comm_prop_idx ON "a1f6d6c9-8177-41cd-a13d-af04029d658e" USING gin (properties);


--
-- Name: p_communications_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX p_communications_geom_geom_idx ON "a1f6d6c9-8177-41cd-a13d-af04029d658e" USING gist (geom);


--
-- Name: pune_area_1_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX pune_area_1_geom_geom_idx ON "6f38e831-f02f-4c55-959e-458585beb986" USING gist (geom);


--
-- Name: pune_area_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX pune_area_id_idx ON "6f38e831-f02f-4c55-959e-458585beb986" USING btree (id);


--
-- Name: pune_area_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX pune_area_prop_idx ON "6f38e831-f02f-4c55-959e-458585beb986" USING gin (properties);


--
-- Name: state_bound_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX state_bound_id_idx ON "ab1fbeea-3651-43e3-8e4f-90c9e86940a6" USING btree (id);


--
-- Name: state_bound_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX state_bound_prop_idx ON "ab1fbeea-3651-43e3-8e4f-90c9e86940a6" USING gin (properties);


--
-- Name: state_boundary_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX state_boundary_geom_geom_idx ON "ab1fbeea-3651-43e3-8e4f-90c9e86940a6" USING gist (geom);


--
-- Name: state_hq_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX state_hq_geom_geom_idx ON "61fe61d8-f3ff-4138-96c6-d0a2694fce81" USING gist (geom);


--
-- Name: state_hq_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX state_hq_id_idx ON "61fe61d8-f3ff-4138-96c6-d0a2694fce81" USING btree (id);


--
-- Name: state_hq_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX state_hq_prop_idx ON "61fe61d8-f3ff-4138-96c6-d0a2694fce81" USING gin (properties);


--
-- Name: subdist_bound_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX subdist_bound_id_idx ON "3f758964-acf2-44e9-ab78-ecc777a9a843" USING btree (id);


--
-- Name: subdist_bound_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX subdist_bound_prop_idx ON "3f758964-acf2-44e9-ab78-ecc777a9a843" USING btree (id);


--
-- Name: subdistrict_boundary_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX subdistrict_boundary_geom_geom_idx ON "3f758964-acf2-44e9-ab78-ecc777a9a843" USING gist (geom);


--
-- Name: surat_area_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX surat_area_geom_geom_idx ON "9d4a038d-8139-423a-ac74-f360c4942026" USING gist (geom);


--
-- Name: surat_area_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX surat_area_id_idx ON "9d4a038d-8139-423a-ac74-f360c4942026" USING btree (id);


--
-- Name: surat_area_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX surat_area_prop_idx ON "9d4a038d-8139-423a-ac74-f360c4942026" USING gin (properties);


--
-- Name: varanasi_area_geom_geom_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX varanasi_area_geom_geom_idx ON "6f8b98a9-0949-4daa-a490-6c5da220b815" USING gist (geom);


--
-- Name: varanasi_area_id_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX varanasi_area_id_idx ON "6f8b98a9-0949-4daa-a490-6c5da220b815" USING btree (id);


--
-- Name: varanasi_area_prop_idx; Type: INDEX; Schema: public; Owner: iudx_gis_user
--

CREATE INDEX varanasi_area_prop_idx ON "6f8b98a9-0949-4daa-a490-6c5da220b815" USING gin (properties);


--
-- Name: tilematrixsets_relation collection_fkey; Type: FK CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY tilematrixsets_relation
    ADD CONSTRAINT collection_fkey FOREIGN KEY (collection_id) REFERENCES collections_details(id);


--
-- Name: collection_supported_crs collection_supported_crs_collection_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY collection_supported_crs
    ADD CONSTRAINT collection_supported_crs_collection_id_fkey FOREIGN KEY (collection_id) REFERENCES collections_details(id);


--
-- Name: collection_supported_crs collection_supported_crs_crs_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY collection_supported_crs
    ADD CONSTRAINT collection_supported_crs_crs_id_fkey FOREIGN KEY (crs_id) REFERENCES crs_to_srid(id);


--
-- Name: collections_details crs_fk; Type: FK CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY collections_details
    ADD CONSTRAINT crs_fk FOREIGN KEY (crs) REFERENCES crs_to_srid(crs);


--
-- Name: ri_details rg_fk; Type: FK CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY ri_details
    ADD CONSTRAINT rg_fk FOREIGN KEY (rg_id) REFERENCES rg_details(id);


--
-- Name: rg_details users_fk; Type: FK CONSTRAINT; Schema: public; Owner: iudx_gis_user
--

ALTER TABLE ONLY rg_details
    ADD CONSTRAINT users_fk FOREIGN KEY (role_id) REFERENCES roles(user_id);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- Name: TABLE auditing_ogc; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE auditing_ogc TO ${flyway:user};

