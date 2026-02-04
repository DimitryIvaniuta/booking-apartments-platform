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
 * Email verification token.
 *
 * <p>Only SHA-256 hash of the token is stored in DB.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  public static EmailVerificationToken issue(UUID userId, String tokenHash, Instant expiresAt) {
    EmailVerificationToken t = new EmailVerificationToken();
    t.id = UUID.randomUUID();
    t.userId = userId;
    t.tokenHash = tokenHash;
    t.createdAt = Instant.now();
    t.expiresAt = expiresAt;
    return t;
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public void markUsed() {
    this.usedAt = Instant.now();
  }
}
