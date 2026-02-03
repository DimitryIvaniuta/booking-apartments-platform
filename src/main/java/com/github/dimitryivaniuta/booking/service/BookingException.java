package com.github.dimitryivaniuta.booking.service;

/**
 * Base exception for booking domain errors.
 */
public class BookingException extends RuntimeException {

  /**
   * Creates a new exception.
   *
   * @param message message
   */
  public BookingException(String message) {
    super(message);
  }

  /**
   * Creates a new exception.
   *
   * @param message message
   * @param cause cause
   */
  public BookingException(String message, Throwable cause) {
    super(message, cause);
  }
}
