package com.github.dimitryivaniuta.booking.auth.service;

/** Base class for authentication/authorization domain errors. */
public class AuthException extends RuntimeException {
  public AuthException(String message) { super(message); }
  public AuthException(String message, Throwable cause) { super(message, cause); }
}
