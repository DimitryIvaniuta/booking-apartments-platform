package com.github.dimitryivaniuta.booking;

import com.github.dimitryivaniuta.booking.api.dto.ApartmentCreateRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates Redis-backed rate limiting filter.
 */
@TestPropertySource(properties = {
    "app.rate-limit.enabled=true",
    "app.rate-limit.max-requests=3",
    "app.rate-limit.window-seconds=60",
    "app.rate-limit.path-prefixes[0]=/api"
})
public class RateLimitingIT extends AbstractIntegrationTest {

  @Autowired
  TestRestTemplate rest;

  @Test
  void afterLimit_excessRequestsReturn429() {
    var adminH = TestAuth.loginHeaders(rest, "admin@local.test", "AdminPassword123!", "it-admin");
    ResponseEntity<String> created = rest.postForEntity("/api/apartments",
        new HttpEntity<>(new ApartmentCreateRequest("RL Loft", "Gdansk", 2), adminH),
        String.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    UUID apartmentId = UUID.fromString(created.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));

    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Client-Id", "client-rl-1");
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    // 3 allowed
    for (int i = 0; i < 3; i++) {
      ResponseEntity<String> r = rest.exchange("/api/apartments/" + apartmentId, HttpMethod.GET, entity, String.class);
      assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // 4th blocked
    ResponseEntity<String> blocked = rest.exchange("/api/apartments/" + apartmentId, HttpMethod.GET, entity, String.class);
    assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(blocked.getBody()).contains("TOO_MANY_REQUESTS");
  }
}
