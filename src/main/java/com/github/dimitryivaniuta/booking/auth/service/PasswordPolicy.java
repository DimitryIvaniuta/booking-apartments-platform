package com.github.dimitryivaniuta.booking.auth.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Password policy validator (production-grade baseline).
 *
 * <p>Rules:
 * <ul>
 *   <li>min 12 chars</li>
 *   <li>upper + lower + digit + special</li>
 *   <li>no whitespace</li>
 *   <li>must not contain email local-part segment (>=3 chars)</li>
 * </ul>
 */
public final class PasswordPolicy {

  private PasswordPolicy() {}

  public static List<String> validate(String email, String password) {
    List<String> errors = new ArrayList<>();
    if (password == null) {
      errors.add("Password is required.");
      return errors;
    }
    if (password.length() < 12) errors.add("Password must be at least 12 characters.");
    if (password.chars().anyMatch(Character::isWhitespace)) errors.add("Password must not contain whitespace.");
    if (!password.chars().anyMatch(Character::isUpperCase)) errors.add("Password must contain an uppercase letter.");
    if (!password.chars().anyMatch(Character::isLowerCase)) errors.add("Password must contain a lowercase letter.");
    if (!password.chars().anyMatch(Character::isDigit)) errors.add("Password must contain a digit.");
    boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
    if (!hasSpecial) errors.add("Password must contain a special character.");

    if (email != null) {
      String local = email.split("@")[0].toLowerCase();
      if (local.length() >= 3 && password.toLowerCase().contains(local)) {
        errors.add("Password must not contain parts of your email.");
      }
    }
    return errors;
  }
}
