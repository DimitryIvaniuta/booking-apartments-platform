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
 * Refresh token family (device/session).
 *
 * <p>Multi-device support: a user can have multiple families (e.g., laptop, phone).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_token_families")
public class RefreshTokenFamily {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "device_id", length = 128)
  private String deviceId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public static RefreshTokenFamily create(UUID userId, String deviceId) {
    RefreshTokenFamily f = new RefreshTokenFamily();
    f.id = UUID.randomUUID();
    f.userId = userId;
    f.deviceId = deviceId;
    f.createdAt = Instant.now();
    return f;
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public void touch() {
    this.lastUsedAt = Instant.now();
  }

  public void revoke() {
    this.revokedAt = Instant.now();
  }
}
