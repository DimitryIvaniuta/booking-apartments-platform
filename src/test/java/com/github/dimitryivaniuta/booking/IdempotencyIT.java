package com.github.dimitryivaniuta.booking;

import com.github.dimitryivaniuta.booking.api.dto.ApartmentCreateRequest;
import com.github.dimitryivaniuta.booking.api.dto.BookingHoldRequest;
import com.github.dimitryivaniuta.booking.api.dto.BookingResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that Idempotency-Key returns the same booking on retries.
 */
public class IdempotencyIT extends AbstractIntegrationTest {

  @Autowired
  TestRestTemplate rest;

  @Test
  void idempotencyKey_returnsSameBooking() {
    var adminH = TestAuth.loginHeaders(rest, "admin@local.test", "AdminPassword123!", "it-admin");
    ResponseEntity<String> a = rest.postForEntity("/api/apartments",
        new HttpEntity<>(new ApartmentCreateRequest("Studio", "Gdansk", 2), adminH),
        String.class);

    UUID apartmentId = UUID.fromString(a.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));

    BookingHoldRequest req = new BookingHoldRequest(apartmentId,
        LocalDate.of(2026, 2, 10),
        LocalDate.of(2026, 2, 12));

    var userH = TestAuth.loginHeaders(rest, "user1@local.test", "UserPassword123!", "it-user");
    userH.add("Idempotency-Key", "idem-1");

    ResponseEntity<BookingResponse> r1 = rest.postForEntity("/api/bookings/hold", new HttpEntity<>(req, userH), BookingResponse.class);
    ResponseEntity<BookingResponse> r2 = rest.postForEntity("/api/bookings/hold", new HttpEntity<>(req, userH), BookingResponse.class);

    assertThat(r1.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(r2.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(r1.getBody().id()).isEqualTo(r2.getBody().id());
  }
}
