package com.github.dimitryivaniuta.booking.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Refresh request. */
public record RefreshRequest(@NotBlank String refreshToken) {}
