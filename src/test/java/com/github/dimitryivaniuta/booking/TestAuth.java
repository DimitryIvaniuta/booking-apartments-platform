package com.github.dimitryivaniuta.booking;

import com.github.dimitryivaniuta.booking.auth.api.dto.LoginRequest;
import com.github.dimitryivaniuta.booking.auth.api.dto.TokenResponse;
import java.util.UUID;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Test helper for obtaining authenticated headers.
 */
public final class TestAuth {

  private TestAuth() {}

  public static HttpHeaders loginHeaders(TestRestTemplate rest, String email, String password, String deviceId) {
    ResponseEntity<TokenResponse> r = rest.postForEntity(
        "/api/auth/login",
        new LoginRequest(email, password, deviceId),
        TokenResponse.class
    );
    if (!r.getStatusCode().is2xxSuccessful() || r.getBody() == null) {
      throw new IllegalStateException("Login failed for " + email + " status=" + r.getStatusCode());
    }
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(r.getBody().accessToken());
    return h;
  }
}
