package ogc.rs.common;

import ogc.rs.apiserver.util.ProcessException;

public class Constants {

  /* service proxy addresses */
  public static final String AUTH_SERVICE_ADDRESS = "ogc.rs.authentication.service";

    public static final String DATABASE_SERVICE_ADDRESS = "ogc.rs.database.service";
    public static final String METERING_SERVICE_ADDRESS = "ogc.rs.metering.service";
    public static final String PROCESSING_SERVICE_ADDRESS = "ogc.rs.processes.service";
    public static final String JOBS_SERVICE_ADDRESS = "ogc.rs.jobs.service";
  public static final String DEFAULT_SERVER_CRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    public static final String UUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4  }-[0-9a-f]{12}$";

  public static final String CATALOGUE_SERVICE_ADDRESS = "ogc.rs.catalog.service";
  public static final String EPOCH_TIME = "epochTime";
  public static final String ISO_TIME = "isoTime";
  public static final String RESPONSE_SIZE = "response_size";
  public static final String PROVIDER_ID = "providerId";
  public static final String ID = "id";
  public static final String API = "api";
  public static final String ITEM_TYPE = "itemType";
  public static final String API_ENDPOINT = "apiEndpoint";
  public static final String RESOURCE_GROUP = "resourceGroup";
  public static final String USER_ID = "userid";
  public static final String DELEGATOR_ID = "delegatorId";
  public static final String ROLE = "role";
  public static final String DRL = "drl";
  public static final String DID = "did";
  public static final String EVENT = "event";
  public static final String TYPE_KEY = "type";
  public static final String REQUEST_JSON = "request_json";
  public static String CAT_SEARCH_PATH = "/search";
  public static final ProcessException processException404 =
    new ProcessException(404, "Not Found", "Resource Not Found");
  public static final ProcessException processException500 =
    new ProcessException(500, "Internal Server Error", "Internal Server Error");

}
