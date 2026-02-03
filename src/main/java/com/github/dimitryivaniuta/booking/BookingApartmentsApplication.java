package com.github.dimitryivaniuta.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.github.dimitryivaniuta.booking.auth.service.AuthProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Booking Apartments application.
 *
 * <p>This service provides concurrency-safe booking holds/confirmations using
 * PostgreSQL exclusion constraints, Redis idempotency, and Kafka events via a transactional outbox.</p>
 */
@EnableScheduling
@EnableConfigurationProperties(AuthProperties.class)
@SpringBootApplication
public class BookingApartmentsApplication {

  /**
   * Application entrypoint.
   *
   * @param args CLI args
   */
  public static void main(String[] args) {
    SpringApplication.run(BookingApartmentsApplication.class, args);
  }
}
