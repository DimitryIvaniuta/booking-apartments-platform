package com.github.dimitryivaniuta.booking.domain;

/**
 * Booking status used at application level.
 *
 * <p>Persisted as VARCHAR in DB (no DB-level enum/check constraint).</p>
 */
public enum BookingStatus {
  HOLD,
  CONFIRMED,
  CANCELLED,
  EXPIRED
}
