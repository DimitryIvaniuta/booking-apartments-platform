package com.github.dimitryivaniuta.booking.auth.security;

import com.github.dimitryivaniuta.booking.auth.service.TokenRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces access-token revocation for already authenticated requests.
 *
 * <p>Runs after JWT authentication has populated the {@link SecurityContextHolder}.</p>
 */
@Component
@RequiredArgsConstructor
public class RevokedTokenFilter extends OncePerRequestFilter {

  /** Must match your logging pattern %X{corrId}. */
  private static final String MDC_CORR_ID_KEY = "corrId";

  private final TokenRevocationService revocationService;

  @Override
  protected void doFilterInternal(
          HttpServletRequest request,
          HttpServletResponse response,
          FilterChain filterChain
  ) throws ServletException, IOException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      if (revocationService.isRevoked(jwt)) {
        if (response.isCommitted()) return;

        String corrId = MDC.get(MDC_CORR_ID_KEY);

        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        String json = """
            {
              "timestamp":"%s",
              "status":401,
              "error":"UNAUTHORIZED",
              "message":"Token revoked",
              "correlationId":"%s"
            }
            """.formatted(Instant.now(), corrId == null ? "" : corrId);

        response.getWriter().write(json);
        response.flushBuffer();
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
