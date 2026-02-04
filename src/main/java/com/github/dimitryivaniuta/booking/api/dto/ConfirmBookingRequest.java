package com.github.dimitryivaniuta.booking.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Confirm booking request DTO.
 *
 * @param paymentRef external payment reference (for demo only)
 */
public record ConfirmBookingRequest(@NotBlank String paymentRef) {}
