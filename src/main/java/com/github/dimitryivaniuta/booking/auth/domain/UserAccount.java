package com.github.dimitryivaniuta.booking.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Application user account.
 *
 * <p>Passwords are stored as BCrypt hashes. Email verification is required before enabling the account.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserAccount {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "email", nullable = false, length = 200, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 200)
  private String passwordHash;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<Role> roles = new HashSet<>();

  public static UserAccount newUnverified(String email, String passwordHash) {
    UserAccount u = new UserAccount();
    u.id = UUID.randomUUID();
    u.email = email.toLowerCase();
    u.passwordHash = passwordHash;
    u.enabled = false;
    u.emailVerified = false;
    u.createdAt = Instant.now();
    u.updatedAt = u.createdAt;
    return u;
  }

  public void markVerifiedAndEnabled() {
    this.emailVerified = true;
    this.enabled = true;
    this.updatedAt = Instant.now();
  }
}
