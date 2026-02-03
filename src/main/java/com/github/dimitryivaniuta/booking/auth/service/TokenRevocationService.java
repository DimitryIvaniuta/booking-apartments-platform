package com.github.dimitryivaniuta.booking.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Redis-backed token revocation.
 *
 * <p>Two mechanisms:
 * <ul>
 *   <li>JTI blacklist for single-token revoke (logout)</li>
 *   <li>valid-after timestamp per user for global invalidation (logout-all)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

  private final StringRedisTemplate redis;

  public void revokeJti(String jti, Instant expiresAt) {
    if (jti == null || expiresAt == null) return;
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    if (ttl.isNegative() || ttl.isZero()) return;
    redis.opsForValue().set(revokedJtiKey(jti), "1", ttl);
  }

  public void invalidateAllForUser(UUID userId) {
    redis.opsForValue().set(validAfterKey(userId), String.valueOf(Instant.now().getEpochSecond()));
  }

  public boolean isRevoked(Jwt jwt) {
    if (jwt == null) return true;
    String jti = jwt.getId();
    if (jti != null && Boolean.TRUE.equals(redis.hasKey(revokedJtiKey(jti)))) {
      return true;
    }
    UUID userId = UUID.fromString(jwt.getSubject());
    String va = redis.opsForValue().get(validAfterKey(userId));
    if (va != null) {
      try {
        long validAfter = Long.parseLong(va);
        Instant iat = jwt.getIssuedAt();
        if (iat != null && iat.getEpochSecond() < validAfter) {
          return true;
        }
      } catch (Exception ignored) {}
    }
    return false;
  }

  private static String revokedJtiKey(String jti) {
    return "auth:revoked:jti:" + jti;
  }

  private static String validAfterKey(UUID userId) {
    return "auth:valid-after:" + userId;
  }
}
