package com.github.dimitryivaniuta.booking.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility methods for extracting application principal data from JWT.
 */
public final class SecurityUtils {

  private SecurityUtils() {}

  /**
   * Extracts {@link AuthenticatedUser} from JWT.
   *
   * <p>Access tokens are issued with:
   * <ul>
   *   <li>subject (sub) = user UUID</li>
   *   <li>email claim</li>
   *   <li>roles claim = array of role names</li>
   * </ul>
   *
   * @param jwt validated JWT
   * @return principal
   */
  public static AuthenticatedUser fromJwt(Jwt jwt) {
    if (jwt == null) {
      throw new IllegalArgumentException("jwt is required");
    }
    UUID userId = UUID.fromString(jwt.getSubject());
    String email = jwt.getClaimAsString("email");

    Set<String> roles = new HashSet<>();
    Object raw = jwt.getClaims().get("roles");
    if (raw instanceof List<?> list) {
      for (Object o : list) {
        if (o != null) {
          roles.add(o.toString());
        }
      }
    }
    return new AuthenticatedUser(userId, email, roles);
  }
}
