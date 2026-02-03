package com.github.dimitryivaniuta.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Booking entity.
 *
 * <p>Date semantics: {@code startDate} is check-in (inclusive), {@code endDate} is check-out (exclusive).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bookings")
public class Booking {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "apartment_id", nullable = false)
  private UUID apartmentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  /**
   * Status is stored as VARCHAR; JPA enum mapping uses the enum name.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private BookingStatus status;

  /**
   * For HOLD bookings: when the hold expires. NULL for CONFIRMED/CANCELLED.
   */
  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * Create a new booking hold.
   *
   * @param apartmentId apartment id
   * @param userId user id (UUID)
   * @param startDate check-in (inclusive)
   * @param endDate check-out (exclusive)
   * @param expiresAt expiry timestamp
   */
  public static Booking newHold(UUID apartmentId, UUID userId, LocalDate startDate, LocalDate endDate, Instant expiresAt) {
    Booking b = new Booking();
    b.id = UUID.randomUUID();
    b.apartmentId = apartmentId;
    b.userId = userId;
    b.startDate = startDate;
    b.endDate = endDate;
    b.status = BookingStatus.HOLD;
    b.expiresAt = expiresAt;
    b.createdAt = Instant.now();
    b.updatedAt = b.createdAt;
    return b;
  }

  /** Marks the hold as confirmed. */
  public void confirm() {
    this.status = BookingStatus.CONFIRMED;
    this.expiresAt = null;
    this.updatedAt = Instant.now();
  }

  /** Cancels the booking. */
  public void cancel() {
    this.status = BookingStatus.CANCELLED;
    this.expiresAt = null;
    this.updatedAt = Instant.now();
  }

  /** Expires the hold. */
  public void expire() {
    this.status = BookingStatus.EXPIRED;
    this.updatedAt = Instant.now();
  }

  /**
   * @param now current time
   * @return true if hold expired
   */
  public boolean isHoldExpired(Instant now) {
    return this.status == BookingStatus.HOLD && this.expiresAt != null && this.expiresAt.isBefore(now);
  }
}
