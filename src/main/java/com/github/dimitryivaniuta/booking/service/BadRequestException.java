package com.github.dimitryivaniuta.booking.service;

/**
 * Thrown when the client sends invalid request data.
 */
public class BadRequestException extends BookingException {

  /**
   * Creates a new exception.
   *
   * @param message message
   */
  public BadRequestException(String message) {
    super(message);
  }
}
