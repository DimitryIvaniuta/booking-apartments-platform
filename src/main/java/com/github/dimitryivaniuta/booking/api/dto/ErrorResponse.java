package com.github.dimitryivaniuta.booking.api.dto;

import java.time.Instant;

/**
 * Error response returned by the API.
 *
 * @param timestamp time
 * @param status HTTP status
 * @param error short error
 * @param message details
 * @param correlationId correlation id for troubleshooting
 */
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String correlationId
) {}
