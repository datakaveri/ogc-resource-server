package ogc.rs.processes.mapCollectionOnboarding;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ogc.rs.common.S3Config;
import org.apache.commons.exec.CommandLine;

/**
 * Builds {@code gdalinfo -json} for {@code /vsis3/} COGs and retrieves the fields needed for onboarding.
 */
public final class GdalRasterMetadataExtractor {

  /** Last {@code ID["EPSG",…]} in WKT is usually the dataset CRS. */
  private static final Pattern EPSG_IN_WKT =
      Pattern.compile("ID\\[\"EPSG\",(\\d+)\\]", Pattern.CASE_INSENSITIVE);

  private GdalRasterMetadataExtractor() {}

  /**
   * Builds {@code gdalinfo -json} against {@code /vsis3/{bucket}/{objectKey}} with S3 credentials.
   *
   * @param s3c       bucket and endpoint configuration
   * @param objectKey COG key within the bucket (leading {@code /} is stripped)
   * @return command line for {@link org.apache.commons.exec.DefaultExecutor}
   */
  public static CommandLine buildGdalInfoCommand(S3Config s3c, String objectKey) {
    String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
    CommandLine cmd = new CommandLine("gdalinfo");
    cmd.addArgument("-json");
    configureGdalS3Environment(cmd, s3c);
    cmd.addArgument(String.format("/vsis3/%s/%s", s3c.getBucket(), key));
    return cmd;
  }

  /**
   * Appends GDAL {@code --config} entries so VSIS3 can read from the configured bucket.
   *
   * @param cmd command line under construction
   * @param s3c S3 access key, secret, endpoint, HTTPS, and path-style flags
   */
  public static void configureGdalS3Environment(CommandLine cmd, S3Config s3c) {
    cmd.addArgument("--config");
    cmd.addArgument("CPL_VSIL_USE_TEMP_FILE_FOR_RANDOM_WRITE");
    cmd.addArgument("NO");
    cmd.addArgument("--config");
    cmd.addArgument("AWS_ACCESS_KEY_ID");
    cmd.addArgument(s3c.getAccessKey());
    cmd.addArgument("--config");
    cmd.addArgument("AWS_SECRET_ACCESS_KEY");
    cmd.addArgument(s3c.getSecretKey());
    cmd.addArgument("--config");
    cmd.addArgument("AWS_S3_ENDPOINT");
    cmd.addArgument(s3c.getEndpoint().replaceFirst("https?://", ""));
    if (!Boolean.TRUE.equals(s3c.isHttps())) {
      cmd.addArgument("--config");
      cmd.addArgument("AWS_HTTPS");
      cmd.addArgument("NO");
    }
    if (Boolean.TRUE.equals(s3c.isPathBasedAccess())) {
      cmd.addArgument("--config");
      cmd.addArgument("AWS_VIRTUAL_HOSTING");
      cmd.addArgument("FALSE");
    }
  }

  /**
   * EPSG code from gdalinfo JSON: {@code stac["proj:epsg"]}, then {@code coordinateSystem.projjson},
   * then the last {@code ID["EPSG",…]} in WKT.
   *
   * @param info parsed {@code gdalinfo -json} output
   * @return EPSG numeric code, or {@code null} if none was found
   */
  public static Integer extractEpsgCode(JsonObject info) {
    JsonObject stac = info.getJsonObject("stac");
    if (stac != null) {
      Integer epsg = stac.getInteger("proj:epsg");
      if (epsg != null && epsg > 0) {
        return epsg;
      }
    }

    JsonObject coordinateSystem = info.getJsonObject("coordinateSystem");
    if (coordinateSystem != null) {
      Integer fromProjjson = epsgFromProjjson(coordinateSystem.getJsonObject("projjson"));
      if (fromProjjson != null) {
        return fromProjjson;
      }
      Object projjsonValue = coordinateSystem.getValue("projjson");
      if (projjsonValue instanceof String) {
        try {
          fromProjjson = epsgFromProjjson(new JsonObject((String) projjsonValue));
          if (fromProjjson != null) {
            return fromProjjson;
          }
        } catch (Exception ignored) {
          // not JSON
        }
      }
      Integer fromWkt = epsgFromWkt(coordinateSystem.getString("wkt"));
      if (fromWkt != null) {
        return fromWkt;
      }
    }

    return null;
  }

  /**
   * Builds onboarding metadata from gdalinfo JSON and a CRS row from {@code crs_to_srid}.
   *
   * @param info   parsed {@code gdalinfo -json} output
   * @param crsUri OGC CRS URI for the raster native CRS
   * @param srid   EPSG SRID matching {@code crsUri}
   * @return native and WGS84 bboxes plus optional raster dimensions
   * @throws IllegalArgumentException if required extent fields are missing
   */
  public static GdalRasterMetadata parse(JsonObject info, String crsUri, int srid) {
    double[] contentBbox = nativeBbox(info);
    double[] wgs84Bbox = wgs84Bbox(info, contentBbox, srid);
    Integer width = null;
    Integer height = null;
    JsonArray size = info.getJsonArray("size");
    if (size != null && size.size() >= 2) {
      width = size.getInteger(0);
      height = size.getInteger(1);
    }
    return new GdalRasterMetadata(crsUri, srid, contentBbox, wgs84Bbox, width, height);
  }

  /**
   * @param fileName S3 object key or file name
   * @return {@code true} if the name ends with {@code .tif} or {@code .tiff} (case-insensitive)
   */
  public static boolean isTiffFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return false;
    }
    String lower = fileName.toLowerCase(Locale.ROOT);
    return lower.endsWith(".tif") || lower.endsWith(".tiff");
  }

  /**
   * Reads {@code id.authority} / {@code id.code} from a PROJJSON object when authority is EPSG.
   *
   * @param projjson {@code coordinateSystem.projjson} object or parsed string
   * @return EPSG code, or {@code null}
   */
  private static Integer epsgFromProjjson(JsonObject projjson) {
    if (projjson == null) {
      return null;
    }
    Object idValue = projjson.getValue("id");
    if (!(idValue instanceof JsonObject)) {
      return null;
    }
    JsonObject id = (JsonObject) idValue;
    if (!"EPSG".equalsIgnoreCase(id.getString("authority"))) {
      return null;
    }
    Object code = id.getValue("code");
    if (code == null) {
      return null;
    }
    try {
      return Integer.parseInt(code.toString());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Returns the last {@code ID["EPSG",code]} match in WKT (dataset CRS, not parameter codes).
   *
   * @param wkt {@code coordinateSystem.wkt} from gdalinfo
   * @return EPSG code, or {@code null}
   */
  private static Integer epsgFromWkt(String wkt) {
    if (wkt == null || wkt.isBlank()) {
      return null;
    }
    Matcher matcher = EPSG_IN_WKT.matcher(wkt);
    Integer last = null;
    while (matcher.find()) {
      last = Integer.parseInt(matcher.group(1));
    }
    return last;
  }

  /**
   * @param info parsed gdalinfo JSON
   * @return {@code [minX, minY, maxX, maxY]} from {@code cornerCoordinates}
   * @throws IllegalArgumentException if corners are missing
   */
  private static double[] nativeBbox(JsonObject info) {
    JsonObject corners = info.getJsonObject("cornerCoordinates");
    if (corners == null) {
      throw new IllegalArgumentException(Constants.MISSING_NATIVE_EXTENT_MESSAGE);
    }
    JsonArray lowerLeft = corners.getJsonArray("lowerLeft");
    JsonArray upperRight = corners.getJsonArray("upperRight");
    if (lowerLeft == null || upperRight == null || lowerLeft.size() < 2 || upperRight.size() < 2) {
      throw new IllegalArgumentException(Constants.MISSING_NATIVE_EXTENT_MESSAGE);
    }
    return new double[] {
      lowerLeft.getDouble(0),
      lowerLeft.getDouble(1),
      upperRight.getDouble(0),
      upperRight.getDouble(1)
    };
  }

  /**
   * @param info       parsed gdalinfo JSON
   * @param nativeBbox bbox in the dataset native CRS
   * @param srid       native EPSG SRID
   * @return WGS84 {@code [minLon, minLat, maxLon, maxLat]}
   * @throws IllegalArgumentException if {@code wgs84Extent} is absent and SRID is not 4326
   */
  private static double[] wgs84Bbox(JsonObject info, double[] nativeBbox, int srid) {
    JsonObject extent = info.getJsonObject("wgs84Extent");
    if (extent != null) {
      return bboxFromExtent(extent);
    }
    if (srid == 4326) {
      return nativeBbox.clone();
    }
    throw new IllegalArgumentException(Constants.MISSING_WGS84_EXTENT_MESSAGE);
  }

  /**
   * Min/max lon/lat from a GeoJSON polygon {@code coordinates} ring (gdalinfo {@code wgs84Extent}).
   *
   * @param extent GeoJSON polygon object from gdalinfo
   * @return {@code [minLon, minLat, maxLon, maxLat]}
   * @throws IllegalArgumentException if coordinates are missing or empty
   */
  private static double[] bboxFromExtent(JsonObject extent) {
    Object coordinatesObj = extent.getValue("coordinates");
    if (!(coordinatesObj instanceof JsonArray)) {
      throw new IllegalArgumentException(Constants.MISSING_WGS84_EXTENT_MESSAGE);
    }
    JsonArray coordinates = (JsonArray) coordinatesObj;
    if (coordinates.isEmpty()) {
      throw new IllegalArgumentException(Constants.MISSING_WGS84_EXTENT_MESSAGE);
    }
    Object ringObj = coordinates.getValue(0);
    if (!(ringObj instanceof JsonArray)) {
      throw new IllegalArgumentException(Constants.MISSING_WGS84_EXTENT_MESSAGE);
    }
    JsonArray ring = (JsonArray) ringObj;

    double minLon = Double.POSITIVE_INFINITY;
    double minLat = Double.POSITIVE_INFINITY;
    double maxLon = Double.NEGATIVE_INFINITY;
    double maxLat = Double.NEGATIVE_INFINITY;

    for (Object ptObj : ring) {
      if (!(ptObj instanceof JsonArray)) {
        continue;
      }
      JsonArray pt = (JsonArray) ptObj;
      if (pt.size() < 2) {
        continue;
      }
      Double lon = pt.getDouble(0);
      Double lat = pt.getDouble(1);
      if (lon == null || lat == null) {
        continue;
      }
      minLon = Math.min(minLon, lon);
      minLat = Math.min(minLat, lat);
      maxLon = Math.max(maxLon, lon);
      maxLat = Math.max(maxLat, lat);
    }

    if (!Double.isFinite(minLon)) {
      throw new IllegalArgumentException(Constants.MISSING_WGS84_EXTENT_MESSAGE);
    }
    return new double[] {minLon, minLat, maxLon, maxLat};
  }
}
