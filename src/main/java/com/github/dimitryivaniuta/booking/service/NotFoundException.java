package com.github.dimitryivaniuta.booking.service;

/**
 * Thrown when a requested entity does not exist.
 */
public class NotFoundException extends BookingException {

  /**
   * Creates a new exception.
   *
   * @param message message
   */
  public NotFoundException(String message) {
    super(message);
  }
}
