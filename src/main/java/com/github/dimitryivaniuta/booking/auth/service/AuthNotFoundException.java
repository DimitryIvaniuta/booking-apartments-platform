package com.github.dimitryivaniuta.booking.auth.service;

/** Raised when requested auth resource is not found. */
public class AuthNotFoundException extends AuthException {
  public AuthNotFoundException(String message) { super(message); }
}
