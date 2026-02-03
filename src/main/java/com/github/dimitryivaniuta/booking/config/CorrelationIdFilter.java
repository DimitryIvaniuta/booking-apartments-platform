package com.github.dimitryivaniuta.booking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds a correlation id to each request for structured logs and error responses.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  /** Request/response header name. */
  public static final String HEADER = "X-Correlation-Id";

  /** MDC key. */
  public static final String MDC_KEY = "corrId";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String corrId = request.getHeader(HEADER);
    if (corrId == null || corrId.isBlank()) {
      corrId = UUID.randomUUID().toString();
    }

    MDC.put(MDC_KEY, corrId);
    response.setHeader(HEADER, corrId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
