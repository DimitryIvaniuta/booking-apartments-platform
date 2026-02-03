package com.github.dimitryivaniuta.booking.ratelimit;

import com.github.dimitryivaniuta.booking.config.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Redis-backed API rate limiting filter.
 *
 * <p>Purpose: protect p95/p99 under bursty traffic by shedding load early.
 * We rate-limit per client (X-Client-Id if provided; otherwise remote IP).
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

  private final StringRedisTemplate redis;
  private final RateLimitProperties props;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!props.enabled()) {
      return true;
    }
    String uri = request.getRequestURI();
    if (uri == null) {
      return true;
    }
    // Never rate-limit health probes.
    if (uri.startsWith("/actuator")) {
      return true;
    }
    List<String> prefixes = props.pathPrefixes() == null ? List.of("/api") : props.pathPrefixes();
    return prefixes.stream().noneMatch(uri::startsWith);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String clientId = null;
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    if (a != null && a.getPrincipal() instanceof Jwt jwt) {
      clientId = "u:" + jwt.getSubject();
    }
    if (clientId == null) {
      clientId = request.getHeader("X-Client-Id");
    }
    if (clientId == null || clientId.isBlank()) {
      clientId = request.getRemoteAddr();
    }
    clientId = sanitize(clientId);

    int window = Math.max(1, props.windowSeconds());
    int limit = Math.max(1, props.maxRequests());
    long bucket = Instant.now().getEpochSecond() / window;

    String key = "rl:" + clientId + ":" + bucket;
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, Duration.ofSeconds(window + 1L));
    }

    if (count != null && count > limit) {
      String corrId = MDC.get(CorrelationIdFilter.MDC_KEY);
      response.setStatus(429);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());

      // Provide a small hint for backoff.
      response.setHeader("Retry-After", String.valueOf(window));
      if (corrId != null) {
        response.setHeader(CorrelationIdFilter.HEADER, corrId);
      }

      String json = "{" +
          "\"timestamp\":\"" + Instant.now().toString() + "\"," +
          "\"status\":429," +
          "\"error\":\"TOO_MANY_REQUESTS\"," +
          "\"message\":\"Too many requests. Please slow down and retry.\"," +
          "\"correlationId\":\"" + (corrId == null ? "" : corrId) + "\"" +
          "}";

      response.getWriter().write(json);
      log.warn("Rate limited request: clientId={} uri={} count={}", clientId, request.getRequestURI(), count);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private static String sanitize(String raw) {
    // Keep keys safe and bounded.
    String s = raw.replaceAll("[^a-zA-Z0-9:._-]", "_");
    return s.length() > 64 ? s.substring(0, 64) : s;
  }
}
