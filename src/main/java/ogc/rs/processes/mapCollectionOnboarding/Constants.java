package ogc.rs.processes.mapCollectionOnboarding;

public final class Constants {

  private Constants() {}

  /** OGC URI template for EPSG codes ({@code %d} = SRID). */
  public static final String EPSG_OGC_CRS_URI_TEMPLATE =
      "http://www.opengis.net/def/crs/EPSG/0/%d";

  public static final String MAP_COLLECTION_TYPE = "MAP";
  public static final String MAP_ENCLOSURE_MIME_TYPE = "image/tiff";
  public static final String MESSAGE = "message";

  public static final String START_MAP_COLLECTION_ONBOARDING =
      "Starting map collection onboarding process.";
  public static final String CATALOGUE_CHECK_MESSAGE =
      "Catalogue resource verified. Validating new map collection.";
  public static final String COLLECTION_STATE_MESSAGE =
      "New map collection validated. Verifying COG in S3.";
  public static final String S3_FILE_CHECK_MESSAGE = "COG present in S3. Extracting raster metadata.";
  public static final String GDAL_METADATA_MESSAGE =
      "Raster metadata extracted. Persisting collection and map metadata.";
  public static final String MAP_COLLECTION_ONBOARDING_SUCCESS_MESSAGE =
      "Map collection onboarding completed successfully.";
  public static final String MAP_COLLECTION_ONBOARDING_FAILURE_MESSAGE =
      "Map collection onboarding failed.";

  public static final String INVALID_FILE_EXTENSION_MESSAGE =
      "fileName must reference a GeoTIFF (.tif or .tiff).";
  public static final String S3_FILE_EXISTENCE_FAIL_MESSAGE = "COG not found in S3.";
  public static final String S3_EMPTY_FILE_MESSAGE = "COG exists in S3 but is empty.";
  public static final String GDAL_INFO_FAILED = "Failed to extract metadata with gdalinfo.";
  public static final String CRS_ERROR = "CRS not present as EPSG in raster.";
  public static final String CRS_FETCH_FAILED = "Failed to fetch CRS from database.";
  public static final String COLLECTION_ALREADY_EXISTS_MESSAGE =
      "Collection already exists in the database; map onboarding requires a catalogue resource "
          + "that has not been onboarded as a feature or other collection type.";
  public static final String COLLECTION_EXISTENCE_CHECK_FAILURE_MESSAGE =
      "Failed to check collection existence in database.";
  public static final String ONBOARDING_FAILED_DB_ERROR =
      "Failed to persist map collection metadata in database.";
  public static final String HANDLE_FAILURE_MESSAGE = "Failed to update job status after failure.";
  public static final String MISSING_WGS84_EXTENT_MESSAGE =
      "gdalinfo did not report wgs84Extent for the COG.";
  public static final String MISSING_NATIVE_EXTENT_MESSAGE =
      "gdalinfo did not report native corner coordinates for the COG.";
}
