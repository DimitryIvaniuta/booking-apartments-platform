package com.github.dimitryivaniuta.booking.security;

import java.util.Set;
import java.util.UUID;

/**
 * Authenticated user principal extracted from a validated JWT.
 *
 * @param userId user id
 * @param email email
 * @param roles role names (e.g. USER, ADMIN)
 */
public record AuthenticatedUser(UUID userId, String email, Set<String> roles) {

  /** @return true if user has ADMIN role. */
  public boolean isAdmin() {
    return roles != null && roles.contains("ADMIN");
  }
}
