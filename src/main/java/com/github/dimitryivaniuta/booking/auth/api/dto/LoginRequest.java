package com.github.dimitryivaniuta.booking.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Login request. */
public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    String deviceId
) {}
