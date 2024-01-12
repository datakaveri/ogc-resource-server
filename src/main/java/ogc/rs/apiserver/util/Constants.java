package ogc.rs.apiserver.util;

public class Constants {
    // Header params
    public static final String HEADER_TOKEN = "token";
    public static final String HEADER_CSV = "csv";
    public static final String HEADER_JSON = "json";
    public static final String HEADER_PARQUET = "parquet";
    public static final String HEADER_HOST = "Host";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ORIGIN = "Origin";
    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String HEADER_OPTIONS = "options";
    public static final String COUNT_HEADER = "Count";
    public static final String PUBLIC_TOKEN = "public";
    public static final String HEADER_PUBLIC_KEY = "publicKey";
    public static final String HEADER_RESPONSE_FILE_FORMAT = "format";
    public static final String MIME_APPLICATION_JSON = "application/json";
    public static final String LANDING_PAGE = "getLandingPage";
    public static final String CONFORMANCE_CLASSES = "getConformanceClasses";
    public static final String OPENAPI_SPEC = "/api";
    public static final String COLLECTIONS_API = "getCollections";
    public static final String COLLECTION_API = "describeCollection";
    public static final String FEATURES_API = "getFeatures";
    public static final String FEATURE_API = "getFeature";
    public static final String COLLECTIONS = "collections";
    public static final String TILEMATRIXSETS_API = "getTileMatrixSetsList";
    public static final String TILEMATRIXSET_API = "getTileMatrixSet";
    public static final String TILESETSLIST_API = ".collection.map.getTileSetsList";
    public static final String TILESET_API = ".collection.map.getTileSet";
    public static final String TILE_API = ".collection.map.getTile";

    public static final String ITEMS = "items";

    public static final String STAC = "stac";
    public static final String STAC_OPENAPI_SPEC = "/stac/api";
    public static final String PROCESSES_API = "getProcesses";
    public static final String PROCESS_API = "getProcessDescription";
    public static final String CONTENT_TYPE = "content-type";
    public static final String APPLICATION_JSON = "application/json";
}
