package com.github.dimitryivaniuta.booking.auth.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication-related configuration.
 *
 * @param accessTokenTtlMinutes access token TTL
 * @param refreshTokenTtlDays refresh token TTL
 * @param verificationTokenTtlHours email verification token TTL
 * @param lockoutMaxFailures max failures before lockout
 * @param lockoutWindowSeconds rolling window for failures
 * @param lockoutDurationSeconds lock duration
 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
    int accessTokenTtlMinutes,
    int refreshTokenTtlDays,
    int verificationTokenTtlHours,
    int lockoutMaxFailures,
    int lockoutWindowSeconds,
    int lockoutDurationSeconds,
    String outboxTopic
) {

  public Duration accessTtl() {
    return Duration.ofMinutes(Math.max(1, accessTokenTtlMinutes));
  }

  public Duration refreshTtl() {
    return Duration.ofDays(Math.max(1, refreshTokenTtlDays));
  }

  public Duration verificationTtl() {
    return Duration.ofHours(Math.max(1, verificationTokenTtlHours));
  }
}
