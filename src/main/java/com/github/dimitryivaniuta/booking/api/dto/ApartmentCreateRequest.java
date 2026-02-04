package com.github.dimitryivaniuta.booking.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO to create an apartment.
 *
 * @param name apartment name
 * @param city city
 * @param capacity max guests
 */
public record ApartmentCreateRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 120) String city,
    @Min(1) @Max(50) int capacity
) {}
