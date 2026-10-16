package ogc.rs.apiserver.delegation;

import io.vertx.core.Future;
import io.vertx.ext.auth.User;

/**
 * Resolves a {@code did} delegation into the delegator's user, the same way dx-common's
 * {@code AuthenticationHandler} does in dx-dataplane-rs.
 */
public interface DelegationResolver {

  /**
   * @param delegatorSub value of the {@code did} header
   * @param delegatee the authenticated caller (verified JWT)
   * @return the delegator as a Vert.x {@link User}: {@code sub} = delegator, {@code scopes} =
   *     delegated scopes, {@code delegatee_sub} = caller. Fails with a 403 {@code OgcException}
   *     when there is no active delegation.
   */
  Future<User> resolve(String delegatorSub, User delegatee);
}
