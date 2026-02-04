package com.github.dimitryivaniuta.booking.auth.service;

import com.github.dimitryivaniuta.booking.auth.domain.Role;
import com.github.dimitryivaniuta.booking.auth.domain.UserAccount;
import com.github.dimitryivaniuta.booking.auth.repo.RoleRepository;
import com.github.dimitryivaniuta.booking.auth.repo.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service facade.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserAccountRepository userRepo;
  private final RoleRepository roleRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final EmailVerificationService verificationService;
  private final LoginAttemptService loginAttemptService;
  private final TokenRevocationService revocationService;

  /** Result of login/refresh operations. */
  public record TokenPair(String accessToken, String refreshToken, long accessTokenExpiresInSeconds) {}

  /**
   * Registers a user (unverified).
   *
   * <p>In production you would normally allow public registration, but often you restrict it to admins.
   * This demo exposes the endpoint publicly.</p>
   */
  @Transactional
  public UUID register(String email, String password) {
    if (email == null || email.isBlank()) {
      throw new AuthBadRequestException("email is required");
    }
    email = email.trim().toLowerCase();
    if (userRepo.existsByEmailIgnoreCase(email)) {
      throw new AuthBadRequestException("Email already registered.");
    }

    List<String> pwdErrors = PasswordPolicy.validate(email, password);
    if (!pwdErrors.isEmpty()) {
      throw new AuthBadRequestException(String.join(" ", pwdErrors));
    }

    String hash = passwordEncoder.encode(password);
    UserAccount user = UserAccount.newUnverified(email, hash);

    Role userRole = roleRepo.findByName("USER")
        .orElseThrow(() -> new IllegalStateException("Missing role USER. Run Flyway seeds."));
    user.getRoles().add(userRole);

    userRepo.save(user);

    // Issue verification token & outbox event
    verificationService.issueFor(user);

    return user.getId();
  }

  /**
   * Logs in with email/password.
   */
  @Transactional
  public TokenPair login(String email, String password, String deviceId) {
    if (email == null || email.isBlank() || password == null) {
      throw new AuthBadRequestException("email and password are required");
    }

    final String normalizedEmail = email.trim().toLowerCase();

    if (loginAttemptService.isLocked(normalizedEmail)) {
      Instant until = loginAttemptService.lockedUntil(normalizedEmail);
      throw new AuthLockedException("Account locked. Retry after: " + (until == null ? "later" : until.toString()));
    }

    UserAccount user = userRepo.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> {
              loginAttemptService.onFailure(normalizedEmail);
              return new AuthUnauthorizedException("Invalid credentials.");
            });

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      loginAttemptService.onFailure(normalizedEmail);
      throw new AuthUnauthorizedException("Invalid credentials.");
    }

    loginAttemptService.onSuccess(normalizedEmail);

    if (!user.isEnabled() || !user.isEmailVerified()) {
      throw new AuthForbiddenException("Email not verified. Please verify your email first.");
    }

    Jwt jwt = jwtService.issueAccessToken(user);
    var issued = refreshTokenService.issue(user, deviceId);
    return new TokenPair(jwt.getTokenValue(), issued.refreshTokenRaw(), jwt.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
  }

  /**
   * Refreshes tokens using rotating refresh token.
   */
  @Transactional
  public TokenPair refresh(String refreshTokenRaw) {
    var rotated = refreshTokenService.rotate(refreshTokenRaw);
    UserAccount user = userRepo.findById(rotated.family().getUserId())
        .orElseThrow(() -> new AuthUnauthorizedException("User not found."));
    Jwt jwt = jwtService.issueAccessToken(user);
    return new TokenPair(jwt.getTokenValue(), rotated.refreshTokenRaw(), jwt.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
  }

  /** Verifies email using token. */
  public void verifyEmail(String token) {
    verificationService.verify(token);
  }

  /**
   * Logout: revoke current access token and optionally revoke refresh token family.
   */
  public void logout(String accessJti, Instant accessExp, String refreshTokenRaw) {
    revocationService.revokeJti(accessJti, accessExp);
    refreshTokenService.revokeByTokenRaw(refreshTokenRaw);
  }

  /**
   * Logout all sessions for a user: revoke refresh families + invalidate access tokens.
   */
  public void logoutAll(UUID userId) {
    refreshTokenService.revokeAll(userId);
    revocationService.invalidateAllForUser(userId);
  }

  public List<com.github.dimitryivaniuta.booking.auth.domain.RefreshTokenFamily> sessions(UUID userId) {
    return refreshTokenService.listFamilies(userId);
  }

  public void revokeSession(UUID userId, UUID familyId) {
    refreshTokenService.revokeFamily(userId, familyId);
  }
}
