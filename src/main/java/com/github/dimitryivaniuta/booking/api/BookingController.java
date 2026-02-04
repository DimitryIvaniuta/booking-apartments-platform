package com.github.dimitryivaniuta.booking.api;

import com.github.dimitryivaniuta.booking.api.dto.BookingHoldRequest;
import com.github.dimitryivaniuta.booking.api.dto.BookingResponse;
import com.github.dimitryivaniuta.booking.api.dto.ConfirmBookingRequest;
import com.github.dimitryivaniuta.booking.domain.Booking;
import com.github.dimitryivaniuta.booking.service.BookingService;
import com.github.dimitryivaniuta.booking.security.SecurityUtils;
import com.github.dimitryivaniuta.booking.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booking API endpoints.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;

  /**
   * Creates a booking hold.
   *
   * @param req request
   * @param idempotencyKey idempotency key (optional but recommended)
   * @return created booking
   */
  @PostMapping("/hold")
  public BookingResponse hold(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody BookingHoldRequest req,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
  ) {
    AuthenticatedUser user = SecurityUtils.fromJwt(jwt);
    Booking b = bookingService.createHold(user, req, idempotencyKey);
    return ApiMapper.toResponse(b);
  }

  /**
   * Confirms a booking hold.
   *
   * @param id booking id
   * @param req confirm request
   * @return booking
   */
  @PostMapping("/{id}/confirm")
  public BookingResponse confirm(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody ConfirmBookingRequest req
  ) {
    AuthenticatedUser user = SecurityUtils.fromJwt(jwt);
    return ApiMapper.toResponse(bookingService.confirm(user, id, req.paymentRef()));
  }

  /**
   * Cancels a booking.
   *
   * @param id booking id
   * @return booking
   */
  @PostMapping("/{id}/cancel")
  public BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    AuthenticatedUser user = SecurityUtils.fromJwt(jwt);
    return ApiMapper.toResponse(bookingService.cancel(user, id));
  }

  /**
   * Gets a booking.
   *
   * @param id booking id
   * @return booking
   */
  @GetMapping("/{id}")
  public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    AuthenticatedUser user = SecurityUtils.fromJwt(jwt);
    return ApiMapper.toResponse(bookingService.get(user, id));
  }
}
