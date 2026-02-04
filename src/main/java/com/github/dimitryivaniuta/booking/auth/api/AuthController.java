package com.github.dimitryivaniuta.booking.auth.api;

import com.github.dimitryivaniuta.booking.auth.api.dto.LoginRequest;
import com.github.dimitryivaniuta.booking.auth.api.dto.LogoutRequest;
import com.github.dimitryivaniuta.booking.auth.api.dto.RefreshRequest;
import com.github.dimitryivaniuta.booking.auth.api.dto.RegisterRequest;
import com.github.dimitryivaniuta.booking.auth.api.dto.SessionResponse;
import com.github.dimitryivaniuta.booking.auth.api.dto.TokenResponse;
import com.github.dimitryivaniuta.booking.auth.service.AuthService;
import com.github.dimitryivaniuta.booking.security.SecurityUtils;
import com.github.dimitryivaniuta.booking.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UUID register(@Valid @RequestBody RegisterRequest req) {
    return authService.register(req.email(), req.password());
  }

  @GetMapping("/verify")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void verify(@RequestParam("token") String token) {
    authService.verifyEmail(token);
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest req) {
    var pair = authService.login(req.email(), req.password(), req.deviceId());
    return new TokenResponse(pair.accessToken(), pair.refreshToken(), pair.accessTokenExpiresInSeconds());
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
    var pair = authService.refresh(req.refreshToken());
    return new TokenResponse(pair.accessToken(), pair.refreshToken(), pair.accessTokenExpiresInSeconds());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) LogoutRequest req) {
    String refreshToken = req == null ? null : req.refreshToken();
    authService.logout(jwt.getId(), jwt.getExpiresAt(), refreshToken);
  }

  @PostMapping("/logout-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logoutAll(@AuthenticationPrincipal Jwt jwt) {
    AuthenticatedUser u = SecurityUtils.fromJwt(jwt);
    authService.logoutAll(u.userId());
  }

  @GetMapping("/sessions")
  public List<SessionResponse> sessions(@AuthenticationPrincipal Jwt jwt) {
    AuthenticatedUser u = SecurityUtils.fromJwt(jwt);
    return authService.sessions(u.userId()).stream()
        .map(f -> new SessionResponse(f.getId(), f.getDeviceId(), f.getCreatedAt(), f.getLastUsedAt(), f.getRevokedAt()))
        .toList();
  }

  @PostMapping("/sessions/{id}/revoke")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    AuthenticatedUser u = SecurityUtils.fromJwt(jwt);
    authService.revokeSession(u.userId(), id);
  }
}
