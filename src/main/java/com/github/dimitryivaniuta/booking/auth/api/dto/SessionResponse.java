package com.github.dimitryivaniuta.booking.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Refresh session representation. */
public record SessionResponse(
    UUID id,
    String deviceId,
    Instant createdAt,
    Instant lastUsedAt,
    Instant revokedAt
) {}
