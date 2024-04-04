package ogc.rs.apiserver.util;

import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;

/**
 * This class is used to represent exceptions that occur during the execution of a process.
 */
public class ProcessException extends ServiceException {

  public static final int OGC_FUTURE_ERROR = 1339;
  private static final long serialVersionUID = 1L;
  private final int statusCode;
  private final String code;
  private final String description;

  /**
   * Creates a new ProcessException with the specified status code, code, and description.
   *
   * @param status      The status code for this exception.
   * @param code        The code for this exception.
   * @param description The description for this exception.
   */
  public ProcessException(int status, String code, String description) {
    super(OGC_FUTURE_ERROR, description);
    this.statusCode = status;
    this.code = code;
    this.description = description;
  }

  public JsonObject getJson() {
    return new JsonObject().put("type", this.code).put("title", this.code)
      .put("detail", this.description);
  }

  public int getStatusCode() {
    return this.statusCode;
  }

}

