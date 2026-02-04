package com.github.dimitryivaniuta.booking.service;

import com.github.dimitryivaniuta.booking.api.ApiMapper;
import com.github.dimitryivaniuta.booking.api.dto.ApartmentResponse;
import com.github.dimitryivaniuta.booking.config.CacheConfig;
import com.github.dimitryivaniuta.booking.domain.BookingStatus;
import com.github.dimitryivaniuta.booking.repo.ApartmentRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side service for availability search.
 *
 * <p>We cache the search result for a short TTL because this endpoint is typically the hottest read
 * path in booking systems.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

  private final ApartmentRepository apartmentRepository;

  /**
   * Searches available apartments by city/capacity for the given date range.
   *
   * @param city city (case-insensitive), optional
   * @param minCapacity minimum capacity, optional
   * @param from check-in (inclusive)
   * @param to check-out (exclusive)
   * @param page page index (0-based)
   * @param size page size
   * @return page of apartments
   */
  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = CacheConfig.AVAILABILITY_SEARCH_CACHE,
      key = "T(String).valueOf(#city).toLowerCase() + '|' + T(String).valueOf(#minCapacity) + '|' + #from + '|' + #to + '|' + #page + '|' + #size"
  )
  public Page<ApartmentResponse> search(String city, Integer minCapacity, LocalDate from, LocalDate to, int page, int size) {
    validateDates(from, to);
    int safePage = Math.max(0, page);
    int safeSize = Math.min(Math.max(1, size), 200);
    Pageable pageable = PageRequest.of(safePage, safeSize);

    Page<com.github.dimitryivaniuta.booking.domain.Apartment> result = apartmentRepository.searchAvailable(
        city,
        minCapacity,
        from,
        to,
        BookingStatus.CANCELLED,
        BookingStatus.EXPIRED,
        pageable
    );

    return result.map(ApiMapper::toResponse);
  }

  private void validateDates(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      throw new BadRequestException("from and to are required.");
    }
    if (!start.isBefore(end)) {
      throw new BadRequestException("from must be before to (to is exclusive checkout date).");
    }
  }
}
