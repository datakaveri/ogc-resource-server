package ogc.rs.processes.mapCollectionOnboarding;

/** Raster extent and CRS extracted from a COG via {@code gdalinfo -json}. */
public final class GdalRasterMetadata {

  private final String crsUri;
  private final int srid;
  private final double[] contentBbox;
  private final double[] wgs84Bbox;
  private final Integer rasterWidth;
  private final Integer rasterHeight;

  /**
   * @param crsUri       OGC CRS URI stored on {@code collections_details.crs}
   * @param srid         native EPSG SRID
   * @param contentBbox  bbox in native CRS ({@code collection_map_metadata.content_bbox})
   * @param wgs84Bbox    bbox in WGS84 ({@code collections_details.bbox})
   * @param rasterWidth  pixel width from gdalinfo {@code size}, may be null
   * @param rasterHeight pixel height from gdalinfo {@code size}, may be null
   */
  public GdalRasterMetadata(
      String crsUri,
      int srid,
      double[] contentBbox,
      double[] wgs84Bbox,
      Integer rasterWidth,
      Integer rasterHeight) {
    this.crsUri = crsUri;
    this.srid = srid;
    this.contentBbox = contentBbox;
    this.wgs84Bbox = wgs84Bbox;
    this.rasterWidth = rasterWidth;
    this.rasterHeight = rasterHeight;
  }

  /** OGC CRS URI for the raster native CRS. */
  public String getCrsUri() {
    return crsUri;
  }

  /** EPSG SRID of the raster native CRS. */
  public int getSrid() {
    return srid;
  }

  /** Bounding box in native CRS as {@code [minX, minY, maxX, maxY]}. */
  public double[] getContentBbox() {
    return contentBbox;
  }

  /** Bounding box in WGS84 as {@code [minX, minY, maxX, maxY]}. */
  public double[] getWgs84Bbox() {
    return wgs84Bbox;
  }

  /** Raster width in pixels, if reported by gdalinfo. */
  public Integer getRasterWidth() {
    return rasterWidth;
  }

  /** Raster height in pixels, if reported by gdalinfo. */
  public Integer getRasterHeight() {
    return rasterHeight;
  }
}
