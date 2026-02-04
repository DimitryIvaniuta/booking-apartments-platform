package com.github.dimitryivaniuta.booking.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Role entity.
 *
 * <p>Stored as a row to allow changes without DB constraints on the booking domain tables.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false, unique = true, length = 64)
  private String name;

  public Role(UUID id, String name) {
    this.id = id;
    this.name = name;
  }
}
