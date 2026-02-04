package com.github.dimitryivaniuta.booking.api;

import com.github.dimitryivaniuta.booking.api.dto.ApartmentResponse;
import com.github.dimitryivaniuta.booking.api.dto.PagedResponse;
import com.github.dimitryivaniuta.booking.service.AvailabilityService;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Availability search endpoints.
 */
@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

  private final AvailabilityService availabilityService;

  /**
   * Searches available apartments by city/capacity for the given date range.
   *
   * @param city city (case-insensitive)
   * @param capacity minimum capacity
   * @param from check-in (inclusive)
   * @param to check-out (exclusive)
   * @param page page index (0-based)
   * @param size page size
   * @return paged list of available apartments
   */
  @GetMapping("/search")
  public PagedResponse<ApartmentResponse> search(
      @RequestParam(value = "city", required = false) String city,
      @RequestParam(value = "capacity", required = false) Integer capacity,
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) int size
  ) {
    Page<ApartmentResponse> result = availabilityService.search(city, capacity, from, to, page, size);
    return new PagedResponse<>(
        result.getContent(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages()
    );
  }
}
