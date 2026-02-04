package com.github.dimitryivaniuta.booking.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Example consumer for booking events.
 *
 * <p>In a real system, this could be notifications, billing, analytics, etc.</p>
 */
@Slf4j
@Component
public class BookingEventsConsumer {

  /**
   * Consumes booking events (demo).
   *
   * @param payload JSON payload
   */
  @KafkaListener(topics = "${booking.outbox.topic:booking-events}", groupId = "booking-apartments")
  public void onEvent(String payload) {
    log.info("booking-event={}", payload);
  }
}
