package com.github.dimitryivaniuta.booking.api.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Availability response.
 *
 * @param apartmentId apartment id
 * @param from start date (inclusive)
 * @param to end date (exclusive)
 * @param available whether available
 */
public record AvailabilityResponse(
    UUID apartmentId,
    LocalDate from,
    LocalDate to,
    boolean available
) {}
