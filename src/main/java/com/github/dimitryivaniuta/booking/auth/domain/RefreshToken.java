package com.github.dimitryivaniuta.booking.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rotating refresh token record.
 *
 * <p>Only the SHA-256 hash of the token is stored in DB. The raw token is returned once at issuance/rotation.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "family_id", nullable = false)
  private UUID familyId;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "replaced_by")
  private UUID replacedBy;

  public static RefreshToken issue(UUID familyId, String tokenHash, Instant expiresAt) {
    RefreshToken t = new RefreshToken();
    t.id = UUID.randomUUID();
    t.familyId = familyId;
    t.tokenHash = tokenHash;
    t.createdAt = Instant.now();
    t.expiresAt = expiresAt;
    return t;
  }

  public boolean isExpired(Instant now) {
    return expiresAt.isBefore(now);
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public void revoke() {
    this.revokedAt = Instant.now();
  }

  public void markReplacedBy(UUID newId) {
    this.replacedBy = newId;
  }
}
