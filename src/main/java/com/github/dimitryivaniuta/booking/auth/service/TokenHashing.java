package com.github.dimitryivaniuta.booking.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Token hashing utilities.
 *
 * <p>We store only SHA-256 hashes of opaque tokens to reduce blast radius if DB is compromised.</p>
 */
public final class TokenHashing {

  private TokenHashing() {}

  /** Returns SHA-256 hex of input token. */
  public static String sha256Hex(String token) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
