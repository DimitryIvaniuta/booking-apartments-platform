package com.github.dimitryivaniuta.booking.api;

import com.github.dimitryivaniuta.booking.api.dto.ApartmentCreateRequest;
import com.github.dimitryivaniuta.booking.api.dto.ApartmentResponse;
import com.github.dimitryivaniuta.booking.api.dto.AvailabilityResponse;
import com.github.dimitryivaniuta.booking.domain.Apartment;
import com.github.dimitryivaniuta.booking.service.ApartmentService;
import com.github.dimitryivaniuta.booking.service.BookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Apartment API endpoints.
 */
@RestController
@RequestMapping("/api/apartments")
@RequiredArgsConstructor
public class ApartmentController {

  private final ApartmentService apartmentService;
  private final BookingService bookingService;

  /**
   * Creates a new apartment.
   *
   * @param req request
   * @return created apartment
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApartmentResponse create(@Valid @RequestBody ApartmentCreateRequest req) {
    Apartment a = apartmentService.create(req);
    return ApiMapper.toResponse(a);
  }

  /**
   * Gets an apartment.
   *
   * @param id apartment id
   * @return apartment
   */
  @GetMapping("/{id}")
  public ApartmentResponse get(@PathVariable UUID id) {
    return ApiMapper.toResponse(apartmentService.get(id));
  }

  /**
   * Checks availability for the given date range.
   *
   * @param id apartment id
   * @param from from
   * @param to to
   * @return availability
   */
  @GetMapping("/{id}/availability")
  public AvailabilityResponse availability(
      @PathVariable UUID id,
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
  ) {
    boolean available = bookingService.isAvailable(id, from, to);
    return new AvailabilityResponse(id, from, to, available);
  }
}
