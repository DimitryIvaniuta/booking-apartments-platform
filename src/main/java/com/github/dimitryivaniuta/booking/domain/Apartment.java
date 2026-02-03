package com.github.dimitryivaniuta.booking.domain;

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
 * Apartment aggregate root.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "apartments")
public class Apartment {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "city", nullable = false, length = 120)
  private String city;

  @Column(name = "capacity", nullable = false)
  private int capacity;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /**
   * Creates a new apartment instance.
   *
   * @param name name
   * @param city city
   * @param capacity capacity
   */
  public Apartment(String name, String city, int capacity) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.city = city;
    this.capacity = capacity;
    this.createdAt = Instant.now();
  }
}
