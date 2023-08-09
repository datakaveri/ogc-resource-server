package ogc.rs.apiserver.util;

import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;

public class OgcException extends ServiceException {
  private static final long serialVersionUID = 1L;
  public static final int OGC_FUTURE_ERROR = 1338;

  private final int statusCode;
  private final String code;
  private final String description;
  public OgcException(int status, String code, String description) {
    super(OGC_FUTURE_ERROR, description);
    this.code = code;
    this.description = description;
    this.statusCode = status;
  }

  public JsonObject getJson() {
    return new JsonObject().put("code", this.code).put("description", this.description);
  }
  public int getStatusCode() {
    return this.statusCode;
  }
}
