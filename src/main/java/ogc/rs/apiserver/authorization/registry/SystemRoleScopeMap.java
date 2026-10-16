package ogc.rs.apiserver.authorization.registry;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import ogc.rs.apiserver.authorization.model.DxRole;
import ogc.rs.apiserver.authorization.model.Scopes;

/**
 * Authoritative role → scope bundle for the five system roles, kept in sync with {@code
 * org.cdpg.dx.auth.authorization.registry.SystemRoleScopeMap} in dx-common. Code-level constant,
 * never in the database.
 */
public final class SystemRoleScopeMap {

  /** Principal claim holding the pre-computed effective scopes. */
  public static final String CLAIM_SCOPES = "scopes";

  private static final Map<DxRole, Set<String>> MAP;

  static {
    EnumMap<DxRole, Set<String>> m = new EnumMap<>(DxRole.class);
    m.put(DxRole.CONSUMER, Set.of(Scopes.DATA_ACCESS));
    m.put(DxRole.PROVIDER, Set.of(Scopes.OWN_ASSET_MANAGEMENT));
    m.put(
        DxRole.ORG_ADMIN,
        Set.of(
            Scopes.ORG_USER_MANAGEMENT,
            Scopes.ORG_ASSET_MANAGEMENT,
            Scopes.ORG_ASSET_PUBLISH,
            Scopes.OWN_ASSET_MANAGEMENT,
            Scopes.ORG_PUBLISHER_MANAGEMENT));
    m.put(
        DxRole.COS_ADMIN,
        Set.of(
            Scopes.ORG_MANAGEMENT,
            Scopes.ASSET_PUBLISH,
            Scopes.ASSET_MANAGEMENT,
            Scopes.USER_MANAGEMENT,
            Scopes.PUBLISHER_MANAGEMENT,
            Scopes.ROLE_MANAGEMENT,
            Scopes.CREDIT_MANAGEMENT,
            Scopes.COMPUTE_MANAGEMENT));
    m.put(DxRole.COMPUTE, Set.of(Scopes.COMPUTE_MANAGEMENT));
    MAP = Map.copyOf(m);
  }

  private SystemRoleScopeMap() {}

  /** Returns the scopes granted by this role. Empty set if the role is unmapped (defensive). */
  public static Set<String> getScopes(DxRole role) {
    return MAP.getOrDefault(role, Set.of());
  }

  /**
   * Computes the effective scopes from the principal's {@code realm_access.roles} and stores them
   * in the principal under {@value #CLAIM_SCOPES}. Mutates the principal in place so the Vert.x
   * {@code User} attributes (e.g. {@code exp}) are preserved.
   */
  public static void enrichWithScopes(JsonObject principal) {
    JsonArray roles =
        principal
            .getJsonObject("realm_access", new JsonObject())
            .getJsonArray("roles", new JsonArray());

    Set<String> scopeSet = new HashSet<>();
    for (Object r : roles) {
      DxRole.fromString(String.valueOf(r)).ifPresent(role -> scopeSet.addAll(getScopes(role)));
    }

    JsonArray scopesArr = new JsonArray();
    scopeSet.forEach(scopesArr::add);
    principal.put(CLAIM_SCOPES, scopesArr);
  }
}
