package com.github.dimitryivaniuta.booking.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a booking hold.
 *
 * <p>User identity is derived from the authenticated JWT (no userId field here).</p>
 *
 * @param apartmentId apartment id
 * @param startDate check-in (inclusive)
 * @param endDate check-out (exclusive)
 */
public record BookingHoldRequest(
    @NotNull UUID apartmentId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate
) {}
