package com.github.dimitryivaniuta.booking;

import com.github.dimitryivaniuta.booking.api.dto.ApartmentCreateRequest;
import com.github.dimitryivaniuta.booking.domain.BookingStatus;
import com.github.dimitryivaniuta.booking.repo.ApartmentRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Verifies that availability search uses Redis cache for repeat requests.
 */
public class AvailabilityCachingIT extends AbstractIntegrationTest {

  @Autowired
  TestRestTemplate rest;

  @SpyBean
  ApartmentRepository apartmentRepository;

  @Test
  void secondSearchHitsCache_repoCalledOnce() {
    // One apartment in the city.
    var adminH = TestAuth.loginHeaders(rest, "admin@local.test", "AdminPassword123!", "it-admin");
    ResponseEntity<String> a = rest.postForEntity("/api/apartments",
        new org.springframework.http.HttpEntity<>(new ApartmentCreateRequest("Cache Loft", "Gdansk", 2), adminH),
        String.class);
    assertThat(a.getStatusCode()).isEqualTo(HttpStatus.OK);
    UUID apartmentId = UUID.fromString(a.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));

    String url = "/api/availability/search?city=Gdansk&capacity=2&from=2026-02-01&to=2026-02-05&page=0&size=20";

    ResponseEntity<String> r1 = rest.getForEntity(url, String.class);
    ResponseEntity<String> r2 = rest.getForEntity(url, String.class);

    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(r1.getBody()).contains(apartmentId.toString());
    assertThat(r2.getBody()).contains(apartmentId.toString());

    // Repository method should be invoked once thanks to cache.
    Mockito.verify(apartmentRepository, Mockito.times(1)).searchAvailable(
        eq("Gdansk"),
        eq(2),
        eq(LocalDate.of(2026, 2, 1)),
        eq(LocalDate.of(2026, 2, 5)),
        eq(BookingStatus.CANCELLED),
        eq(BookingStatus.EXPIRED),
        any()
    );
  }
}
