package com.github.dimitryivaniuta.booking.auth.service;

/** Raised when the authenticated user is not allowed. */
public class AuthForbiddenException extends AuthException {
  public AuthForbiddenException(String message) { super(message); }
}
