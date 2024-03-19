package ogc.rs.apiserver.router.gisentities.ogcfeatures;

/**
 * Enum to represent the valid types in OpenAPI.
 *
 */
public enum OasTypes {
  ARRAY, NUMBER, BOOLEAN, STRING, OBJECT;

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

    else if (postgresType.matches(
        "smallint|integer|bigint|decimal|numeric|real|double precision|smallserial|serial|bigserial")) {
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
