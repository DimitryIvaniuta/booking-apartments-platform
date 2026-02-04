package com.github.dimitryivaniuta.booking.auth.service;

/** Raised for invalid client input. */
public class AuthBadRequestException extends AuthException {
  public AuthBadRequestException(String message) { super(message); }
}
