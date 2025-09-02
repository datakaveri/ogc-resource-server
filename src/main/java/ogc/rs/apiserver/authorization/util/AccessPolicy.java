package ogc.rs.apiserver.authorization.util;

public enum AccessPolicy {
  PRIVATE("private"),
  OPEN("open"),
  RESTRICTED("restricted");

  private final String value;

  AccessPolicy(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static AccessPolicy fromValue(String value) {
    for (AccessPolicy policy : AccessPolicy.values()) {
      if (policy.value.equalsIgnoreCase(value)) {
        return policy;
      }
    }
    throw new IllegalArgumentException("Unknown value: " + value);
  }

  public boolean equalsPolicy(AccessPolicy other) {
    return other != null && this.value.equalsIgnoreCase(other.value);
  }
}
