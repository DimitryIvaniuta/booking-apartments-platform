package com.github.dimitryivaniuta.booking.auth.service;

import com.github.dimitryivaniuta.booking.auth.domain.RefreshToken;
import com.github.dimitryivaniuta.booking.auth.domain.RefreshTokenFamily;
import com.github.dimitryivaniuta.booking.auth.domain.UserAccount;
import com.github.dimitryivaniuta.booking.auth.repo.RefreshTokenFamilyRepository;
import com.github.dimitryivaniuta.booking.auth.repo.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh token issuance and rotation with family reuse detection.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenFamilyRepository familyRepo;
  private final RefreshTokenRepository tokenRepo;
  private final TokenRevocationService revocationService;
  private final AuthProperties props;

  private final SecureRandom random = new SecureRandom();

  /** Result of issuing/rotating refresh token. */
  public record Issued(RefreshTokenFamily family, String refreshTokenRaw, RefreshToken tokenEntity) {}

  /**
   * Issues a new refresh token (creates or reuses family for the device).
   */
  @Transactional
  public Issued issue(UserAccount user, String deviceId) {
    RefreshTokenFamily family = familyRepo.findByUserIdAndDeviceId(user.getId(), deviceId)
        .orElseGet(() -> familyRepo.save(RefreshTokenFamily.create(user.getId(), deviceId)));

    if (family.isRevoked()) {
      // re-open by creating a new family
      family = familyRepo.save(RefreshTokenFamily.create(user.getId(), deviceId));
    }

    String raw = newToken();
    String hash = TokenHashing.sha256Hex(raw);
    RefreshToken token = RefreshToken.issue(family.getId(), hash, Instant.now().plus(props.refreshTtl()));
    tokenRepo.save(token);

    family.touch();
    familyRepo.save(family);

    return new Issued(family, raw, token);
  }

  /**
   * Rotates a refresh token.
   *
   * <p>If a token is reused (already replaced), we treat it as compromise:
   * revoke the whole family and invalidate all access tokens for the user.</p>
   */
  @Transactional
  public Issued rotate(String refreshTokenRaw) {
    String hash = TokenHashing.sha256Hex(refreshTokenRaw);
    RefreshToken existing = tokenRepo.findByTokenHash(hash)
        .orElseThrow(() -> new AuthUnauthorizedException("Invalid refresh token."));

    RefreshTokenFamily family = familyRepo.findById(existing.getFamilyId())
        .orElseThrow(() -> new AuthUnauthorizedException("Invalid refresh token family."));

    if (family.isRevoked()) {
      throw new AuthUnauthorizedException("Refresh session revoked.");
    }

    Instant now = Instant.now();
    if (existing.isExpired(now) || existing.isRevoked()) {
      throw new AuthUnauthorizedException("Refresh token expired or revoked.");
    }

    if (existing.getReplacedBy() != null) {
      // Reuse detected -> compromise
      family.revoke();
      familyRepo.save(family);
      revocationService.invalidateAllForUser(family.getUserId());
      throw new AuthUnauthorizedException("Refresh token reuse detected. Session revoked.");
    }

    String rawNew = newToken();
    String hashNew = TokenHashing.sha256Hex(rawNew);
    RefreshToken newToken = RefreshToken.issue(family.getId(), hashNew, now.plus(props.refreshTtl()));
    tokenRepo.save(newToken);

    existing.markReplacedBy(newToken.getId());
    tokenRepo.save(existing);

    family.touch();
    familyRepo.save(family);

    return new Issued(family, rawNew, newToken);
  }

  @Transactional(readOnly = true)
  public List<RefreshTokenFamily> listFamilies(UUID userId) {
    return familyRepo.findAllByUserId(userId);
  }

  @Transactional
  public void revokeFamily(UUID userId, UUID familyId) {
    RefreshTokenFamily f = familyRepo.findById(familyId)
        .orElseThrow(() -> new AuthNotFoundException("Session not found."));
    if (!f.getUserId().equals(userId)) {
      throw new AuthForbiddenException("Not allowed.");
    }
    f.revoke();
    familyRepo.save(f);
  }

  @Transactional
  public void revokeAll(UUID userId) {
    for (RefreshTokenFamily f : familyRepo.findAllByUserId(userId)) {
      if (!f.isRevoked()) {
        f.revoke();
        familyRepo.save(f);
      }
    }
  }


  /**
   * Revokes a refresh token and its family (best-effort). Used for logout.
   *
   * @param refreshTokenRaw raw refresh token
   */
  @Transactional
  public void revokeByTokenRaw(String refreshTokenRaw) {
    if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
      return;
    }
    String hash = TokenHashing.sha256Hex(refreshTokenRaw);
    tokenRepo.findByTokenHash(hash).ifPresent(t -> {
      t.revoke();
      tokenRepo.save(t);
      familyRepo.findById(t.getFamilyId()).ifPresent(f -> {
        f.revoke();
        familyRepo.save(f);
      });
    });
  }

  private String newToken() {
    byte[] buf = new byte[32];
    random.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }
}
