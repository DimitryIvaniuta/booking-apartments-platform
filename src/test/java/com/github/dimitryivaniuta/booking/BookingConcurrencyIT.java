package com.github.dimitryivaniuta.booking;

import com.github.dimitryivaniuta.booking.api.dto.ApartmentCreateRequest;
import com.github.dimitryivaniuta.booking.api.dto.BookingHoldRequest;
import com.github.dimitryivaniuta.booking.api.dto.BookingResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that concurrent overlapping holds do not double-book an apartment.
 */
public class BookingConcurrencyIT extends AbstractIntegrationTest {

  @Autowired
  TestRestTemplate rest;

  @Test
  void concurrentHolds_onlyOneSucceeds() throws Exception {
    // Create apartment
    var adminH = TestAuth.loginHeaders(rest, "admin@local.test", "AdminPassword123!", "it-admin");
    ResponseEntity<String> a = rest.postForEntity("/api/apartments",
        new HttpEntity<>(new ApartmentCreateRequest("Loft", "Gdansk", 2), adminH),
        String.class);
    assertThat(a.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Extract apartmentId from JSON (simple parse to avoid extra deps)
    UUID apartmentId = UUID.fromString(a.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));

    LocalDate from = LocalDate.of(2026, 2, 1);
    LocalDate to = LocalDate.of(2026, 2, 5);

    var userH0 = TestAuth.loginHeaders(rest, "user1@local.test", "UserPassword123!", "it-user");
    String userAccess = userH0.getFirst(HttpHeaders.AUTHORIZATION);

    var pool = Executors.newFixedThreadPool(16);
    List<Callable<ResponseEntity<BookingResponse>>> tasks = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
      int idx = i;
      tasks.add(() -> {
        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.AUTHORIZATION, userAccess);
        h.add("Idempotency-Key", "key-" + idx);
        BookingHoldRequest req = new BookingHoldRequest(apartmentId, from, to);
        return rest.postForEntity("/api/bookings/hold", new HttpEntity<>(req, h), BookingResponse.class);
      });
    }

    List<Future<ResponseEntity<BookingResponse>>> futures = pool.invokeAll(tasks);
    pool.shutdown();

    long ok = 0;
    long conflict = 0;
    for (Future<ResponseEntity<BookingResponse>> f : futures) {
      ResponseEntity<BookingResponse> r = f.get();
      if (r.getStatusCode().is2xxSuccessful()) ok++;
      if (r.getStatusCode() == HttpStatus.CONFLICT) conflict++;
    }

    assertThat(ok).isEqualTo(1);
    assertThat(conflict).isEqualTo(24);
  }
}
