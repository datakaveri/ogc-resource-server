package ogc.rs.apiserver.router.gisentities.ogcfeatures;

/**
 * All geometry types supported by PostGIS.
 */
public enum PostgisGeomTypes {
  GEOMETRY, // generic type if the geometry column is not specified
  POINT, 
  LINESTRING, 
  LINEARRING, 
  POLYGON, 
  MULTIPOINT, 
  MULTILINESTRING, 
  MULTIPOLYGON, 
  GEOMETRYCOLLECTION, 
  POLYHEDRALSURFACE, 
  TRIANGLE, 
  TIN;
}
