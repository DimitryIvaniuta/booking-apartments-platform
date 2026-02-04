package com.github.dimitryivaniuta.booking.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Booking response DTO.
 *
 * @param id booking id
 * @param apartmentId apartment id
 * @param userId user id
 * @param startDate check-in
 * @param endDate check-out
 * @param status status
 * @param expiresAt hold expiry (null for non-holds)
 * @param createdAt created at
 * @param updatedAt updated at
 */
public record BookingResponse(
    UUID id,
    UUID apartmentId,
    String userId,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt
) {}
