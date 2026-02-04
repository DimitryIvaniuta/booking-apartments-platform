package com.github.dimitryivaniuta.booking.service;

/**
 * Thrown when a request cannot be fulfilled due to a conflict (e.g., double booking).
 */
public class ConflictException extends BookingException {

  /**
   * Creates a new exception.
   *
   * @param message message
   */
  public ConflictException(String message) {
    super(message);
  }

  /**
   * Creates a new exception.
   *
   * @param message message
   * @param cause cause
   */
  public ConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
