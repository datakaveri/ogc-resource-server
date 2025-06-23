INSERT INTO crs_to_srid (crs, srid) VALUES
('http://www.opengis.net/def/crs/OGC/1.3/CRS84', 4326) ON CONFLICT (crs) DO NOTHING;