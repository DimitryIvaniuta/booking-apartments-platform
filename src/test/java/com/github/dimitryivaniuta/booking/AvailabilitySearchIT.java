package com.github.dimitryivaniuta.booking;

import com.github.dimitryivaniuta.booking.api.dto.ApartmentCreateRequest;
import com.github.dimitryivaniuta.booking.api.dto.BookingHoldRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for availability search endpoint.
 */
public class AvailabilitySearchIT extends AbstractIntegrationTest {

  @Autowired
  TestRestTemplate rest;

  @Test
  void searchByCityAndCapacity_filtersOutBookedApartments() {
    UUID a1 = createApartment("Sea View", "Gdansk", 2);
    UUID a2 = createApartment("Big Loft", "Gdansk", 5);
    createApartment("Krakow Flat", "Krakow", 3);

    // Book a2 for the window, it should not appear in search results.
    LocalDate from = LocalDate.of(2026, 2, 1);
    LocalDate to = LocalDate.of(2026, 2, 5);
    hold(a2, from, to, "hold-a2");

    String url = "/api/availability/search?city=Gdansk&capacity=2&from=2026-02-01&to=2026-02-05&page=0&size=50";
    ResponseEntity<String> resp = rest.getForEntity(url, String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Simple assertions without a JSON mapper dependency.
    String body = resp.getBody();
    assertThat(body).contains(a1.toString());
    assertThat(body).doesNotContain(a2.toString());
    assertThat(body).doesNotContain("Krakow");
  }

  private UUID createApartment(String name, String city, int capacity) {
    var adminH = TestAuth.loginHeaders(rest, "admin@local.test", "AdminPassword123!", "it-admin");
    ResponseEntity<String> a = rest.postForEntity("/api/apartments",
        new HttpEntity<>(new ApartmentCreateRequest(name, city, capacity), adminH),
        String.class);
    assertThat(a.getStatusCode()).isEqualTo(HttpStatus.OK);
    return UUID.fromString(a.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
  }

  private void hold(UUID apartmentId, LocalDate from, LocalDate to, String idempotencyKey) {
    BookingHoldRequest req = new BookingHoldRequest(apartmentId, from, to);
    var userH = TestAuth.loginHeaders(rest, "user1@local.test", "UserPassword123!", "it-user");
    userH.add("Idempotency-Key", idempotencyKey);
    ResponseEntity<String> r = rest.postForEntity("/api/bookings/hold", new HttpEntity<>(req, userH), String.class);
    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
  }
}
