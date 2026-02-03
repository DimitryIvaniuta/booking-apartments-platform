package com.github.dimitryivaniuta.booking.repo;

import com.github.dimitryivaniuta.booking.domain.Booking;
import com.github.dimitryivaniuta.booking.domain.BookingStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@link Booking}.
 */
public interface BookingRepository extends JpaRepository<Booking, UUID> {

  /**
   * Checks if there is any non-cancelled/non-expired booking that overlaps with the given period.
   *
   * @param apartmentId apartment id
   * @param from start (inclusive)
   * @param to end (exclusive)
   * @return true if overlapping bookings exist
   */
  @Query("""
      select (count(b) > 0) from Booking b
      where b.apartmentId = :apartmentId
        and b.status <> com.github.dimitryivaniuta.booking.domain.BookingStatus.CANCELLED
        and b.status <> com.github.dimitryivaniuta.booking.domain.BookingStatus.EXPIRED
        and b.startDate < :to
        and b.endDate > :from
      """)
  boolean existsOverlap(@Param("apartmentId") UUID apartmentId,
                        @Param("from") LocalDate from,
                        @Param("to") LocalDate to);

  /**
   * Finds expired holds at a given instant.
   *
   * @param now current time
   * @return bookings
   */
  @Query("""
      select b from Booking b
      where b.status = :status and b.expiresAt is not null and b.expiresAt < :now
      """)
  List<Booking> findExpiredHolds(@Param("status") BookingStatus status, @Param("now") Instant now);

  /**
   * Loads a booking by id and status (useful for state checks).
   *
   * @param id booking id
   * @param status status
   * @return booking
   */
  Optional<Booking> findByIdAndStatus(UUID id, BookingStatus status);

  /**
   * Bulk update to expire holds.
   *
   * @param now current time
   * @return number of rows updated
   */
  @Transactional
  @Modifying
  @Query("""
      update Booking b
      set b.status = com.github.dimitryivaniuta.booking.domain.BookingStatus.EXPIRED,
          b.updatedAt = :now
      where b.status = com.github.dimitryivaniuta.booking.domain.BookingStatus.HOLD
        and b.expiresAt is not null
        and b.expiresAt < :now
      """)
  int expireHolds(@Param("now") Instant now);
}
