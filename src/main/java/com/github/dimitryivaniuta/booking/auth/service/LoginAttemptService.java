package com.github.dimitryivaniuta.booking.auth.service;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis-backed brute-force protection.
 *
 * <p>Tracks failed login attempts per email and temporarily locks accounts after repeated failures.</p>
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

  private final StringRedisTemplate redis;
  private final AuthProperties props;

  public boolean isLocked(String email) {
    return Boolean.TRUE.equals(redis.hasKey(lockKey(email)));
  }

  public Instant lockedUntil(String email) {
    String v = redis.opsForValue().get(lockKey(email));
    if (v == null) return null;
    try {
      return Instant.ofEpochSecond(Long.parseLong(v));
    } catch (Exception e) {
      return null;
    }
  }

  public void onSuccess(String email) {
    redis.delete(failKey(email));
    redis.delete(lockKey(email));
  }

  public void onFailure(String email) {
    String fk = failKey(email);
    Long count = redis.opsForValue().increment(fk);
    if (count != null && count == 1L) {
      redis.expire(fk, Duration.ofSeconds(props.lockoutWindowSeconds()));
    }
    if (count != null && count >= props.lockoutMaxFailures()) {
      // lock
      long until = Instant.now().plusSeconds(props.lockoutDurationSeconds()).getEpochSecond();
      String lk = lockKey(email);
      redis.opsForValue().set(lk, String.valueOf(until), Duration.ofSeconds(props.lockoutDurationSeconds()));
    }
  }

  private String failKey(String email) {
    return "auth:fail:" + normalize(email);
  }

  private String lockKey(String email) {
    return "auth:lock:" + normalize(email);
  }

  private static String normalize(String email) {
    return email == null ? "null" : email.trim().toLowerCase();
  }
}
