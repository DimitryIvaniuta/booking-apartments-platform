package com.github.dimitryivaniuta.booking.ratelimit;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate limiting configuration.
 *
 * <p>We implement a Redis-backed fixed-window limiter using atomic INCR + EXPIRE. This is
 * simple, fast, and works across multiple service instances.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
    boolean enabled,
    int maxRequests,
    int windowSeconds,
    List<String> pathPrefixes
) {
  /** Default values used when properties are absent. */
  public RateLimitProperties() {
    this(true, 200, 60, List.of("/api"));
  }
}
