package com.github.dimitryivaniuta.booking.repo;

import com.github.dimitryivaniuta.booking.domain.Apartment;
import com.github.dimitryivaniuta.booking.domain.BookingStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository for {@link Apartment}.
 */
public interface ApartmentRepository extends JpaRepository<Apartment, UUID> {

  /**
   * Searches apartments by city/capacity and filters out those that have overlapping active bookings.
   *
   * <p>Overlap condition matches the DB exclusion constraint semantics: two intervals overlap if
   * {@code startDate < to && endDate > from} (with end being exclusive).
   *
   * @param city city (case-insensitive); nullable
   * @param minCapacity minimum capacity; nullable
   * @param from check-in (inclusive)
   * @param to check-out (exclusive)
   * @param pageable paging
   * @return page of apartments available in the given window
   */
  @Query("""
      select a from Apartment a
      where (:city is null or lower(a.city) = lower(:city))
        and (:minCapacity is null or a.capacity >= :minCapacity)
        and not exists (
          select 1 from Booking b
          where b.apartmentId = a.id
            and b.status not in (:cancelled, :expired)
            and b.startDate < :to
            and b.endDate > :from
        )
      """)
  Page<Apartment> searchAvailable(
      @Param("city") String city,
      @Param("minCapacity") Integer minCapacity,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      @Param("cancelled") BookingStatus cancelled,
      @Param("expired") BookingStatus expired,
      Pageable pageable
  );
}
