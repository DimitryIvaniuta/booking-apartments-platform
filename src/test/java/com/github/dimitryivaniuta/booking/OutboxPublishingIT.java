package com.github.dimitryivaniuta.booking;

import com.github.dimitryivaniuta.booking.api.dto.ApartmentCreateRequest;
import com.github.dimitryivaniuta.booking.api.dto.BookingHoldRequest;
import com.github.dimitryivaniuta.booking.outbox.OutboxRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures the polling outbox publisher marks messages as published after Kafka publish.
 */
public class OutboxPublishingIT extends AbstractIntegrationTest {

  @Autowired TestRestTemplate rest;
  @Autowired OutboxRepository outboxRepository;

  @Test
  void outboxMessages_getPublished() {
    var adminH = TestAuth.loginHeaders(rest, "admin@local.test", "AdminPassword123!", "it-admin");
    ResponseEntity<String> a = rest.postForEntity("/api/apartments",
        new HttpEntity<>(new ApartmentCreateRequest("Penthouse", "Gdansk", 4), adminH),
        String.class);

    UUID apartmentId = UUID.fromString(a.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));

    BookingHoldRequest req = new BookingHoldRequest(apartmentId,
        LocalDate.of(2026, 2, 20),
        LocalDate.of(2026, 2, 22));

    var userH = TestAuth.loginHeaders(rest, "user1@local.test", "UserPassword123!", "it-user");
    userH.add("Idempotency-Key", "idem-publish-1");
    rest.postForEntity("/api/bookings/hold", new HttpEntity<>(req, userH), String.class);

    Awaitility.await().atMost(ofSeconds(15)).untilAsserted(() -> {
      long unpublished = outboxRepository.findNextBatch(1000).size();
      assertThat(unpublished).isEqualTo(0);
    });
  }
}
