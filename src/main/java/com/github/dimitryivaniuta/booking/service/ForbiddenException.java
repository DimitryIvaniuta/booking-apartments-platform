package com.github.dimitryivaniuta.booking.service;

/**
 * Raised when the authenticated user is not allowed to perform an operation.
 */
public class ForbiddenException extends BookingException {

  public ForbiddenException(String message) {
    super(message);
  }

  public ForbiddenException(String message, Throwable cause) {
    super(message, cause);
  }
}
