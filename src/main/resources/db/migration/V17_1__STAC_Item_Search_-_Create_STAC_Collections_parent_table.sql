-- Creating sequence which will be used for pagination
-- all child tables of the STAC collection parent table will use the same sequence
-- hence pagination becomes simpler when doing STAC Item Search

CREATE SEQUENCE parent_sequence;

GRANT USAGE, SELECT ON SEQUENCE parent_sequence TO ${ogcUser};

CREATE TABLE stac_collection_parent (id VARCHAR(100), bbox double precision[], properties jsonb, geom geometry(Geometry, 4326), p_id INT PRIMARY KEY DEFAULT nextval('parent_sequence'));
