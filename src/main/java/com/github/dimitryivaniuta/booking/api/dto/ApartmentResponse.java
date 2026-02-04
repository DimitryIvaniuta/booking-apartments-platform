package com.github.dimitryivaniuta.booking.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Apartment response DTO.
 *
 * @param id id
 * @param name name
 * @param city city
 * @param capacity capacity
 * @param createdAt created at
 */
public record ApartmentResponse(
    UUID id,
    String name,
    String city,
    int capacity,
    Instant createdAt
) {}
