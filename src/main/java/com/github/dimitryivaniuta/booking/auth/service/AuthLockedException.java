package com.github.dimitryivaniuta.booking.auth.service;

/** Raised when account is temporarily locked due to repeated failed logins. */
public class AuthLockedException extends AuthException {
  public AuthLockedException(String message) { super(message); }
}
