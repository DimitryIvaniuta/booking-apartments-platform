package com.github.dimitryivaniuta.booking.auth.service;

/** Raised when authentication is required or token is invalid. */
public class AuthUnauthorizedException extends AuthException {
  public AuthUnauthorizedException(String message) { super(message); }
}
