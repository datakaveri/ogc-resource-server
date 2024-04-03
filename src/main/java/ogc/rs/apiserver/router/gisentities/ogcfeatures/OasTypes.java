package ogc.rs.apiserver.router.gisentities.ogcfeatures;

/**
 * Enum to represent the valid types in OpenAPI.
 *
 */
public enum OasTypes {
  ARRAY, NUMBER, INTEGER, BOOLEAN, STRING, OBJECT;

  /**
   * Get the closest matching OpenAPI type given a postgres type.
   * 
   * @param postgresType
   * @return
   */
  public static OasTypes getOasTypeFromPostgresType(String postgresType) {
    postgresType = postgresType.toLowerCase();

    if (postgresType.matches("boolean")) {
      return BOOLEAN;
    }

    else if (postgresType.matches("smallint|integer|bigint|smallserial|serial|bigserial")) {
      return INTEGER;
    }

    else if (postgresType.matches("decimal|numeric|real|double precision")) {
      return NUMBER;
    }

    else if (postgresType.matches("array")) {
      return ARRAY;
    }

    else if (postgresType.matches("json|jsonb")) {
      return OBJECT;
    }

    // any other type is considered as a string
    else {
      return STRING;
    }
  }
}
