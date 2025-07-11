package ogc.rs.database.util;

import io.vertx.pgclient.PgException;
import ogc.rs.apiserver.util.OgcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class to handle Multicorn script errors embedded in a {@link PgException} that is thrown when
 * interacting with PostgreSQL.
 */
public class MulticornErrorHandler {

  private static final Logger LOGGER = LogManager.getLogger(MulticornErrorHandler.class);
  /**
   * Multicorn request error hint string - if the error is due to something wrong with the query
   * string.
   */
  public static final String MULTICORN_REQUEST_ERROR_PG_HINT = "MULTICORN_REQUEST_ERR";
  /**
   * Multicorn API error hint string - if the error is due to something wrong with the upstream API.
   */
  public static final String MULTICORN_API_ERROR_PG_HINT = "MULTICORN_API_ERR";

  public static final String MULTICORN_API_ERROR_RESP_CODE = "Failed to fetch data";
  public static final String MULTICORN_API_ERROR_RESP_DESCRIPTION =
      "Something went wrong when getting data from the data source";

  /**
   * Check if exception is a {@link PgException} and is thrown from a Multicorn script. Based on the
   * PostgreSQL error hint ({@link PgException#getHint()}), we can check if the error is indeed
   * thrown from Multicorn and handle it accordingly by throwing an {@link OgcException}. </br>
   * </br>
   * If the hint is {@link MulticornErrorHandler#MULTICORN_REQUEST_ERROR_PG_HINT}, it means that the
   * OGC API resulted in an SQL query that cannot be processed by the Multicorn script. </br>
   * </br>
   * If the hint is {@link MulticornErrorHandler#MULTICORN_API_ERROR_PG_HINT}, it means that the
   * upstream API failed to respond to the script in an expected manner.
   * 
   * @param error the Throwable
   * @return the original Throwable or an {@link OgcException} if it's a Multicorn script error
   */
  public static Throwable handle(Throwable error) {

    if (!(error instanceof PgException)) {
      return error;
    }

    PgException pgExp = (PgException) error;

    if (pgExp.getHint().equals(MULTICORN_REQUEST_ERROR_PG_HINT)) {
      /*
       * We can send back the PgException errorMessage in the API error response as it should have
       * the actual cause of the issue thrown by the Multicorn script.
       */
      return new OgcException(400, "Bad Request", pgExp.getErrorMessage());

    } else if (pgExp.getHint().equals(MULTICORN_API_ERROR_PG_HINT)) {
      /*
       * We don't send back the PgException errorMessage as it will have details of why the upstream
       * API failed. The error logger above will help to debug the issue.
       */
      LOGGER.error("Exception from Multicorn due to upstream API: {} {}", pgExp.getErrorMessage(),
          pgExp.getHint());

      return new OgcException(403, MULTICORN_API_ERROR_PG_HINT,
          MULTICORN_API_ERROR_RESP_DESCRIPTION);

    } else { // ordinary PostgreSQL exception, let it fail normally.
      return error;
    }
  }
}
