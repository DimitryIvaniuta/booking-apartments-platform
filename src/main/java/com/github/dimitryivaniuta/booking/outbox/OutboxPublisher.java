package com.github.dimitryivaniuta.booking.outbox;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polling publisher for transactional outbox messages.
 *
 * <p>Publishing is done outside of the DB transaction that wrote the business update.
 * After a successful publish, the outbox row is marked as published.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

  private final OutboxRepository outboxRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${booking.outbox.poll.batch-size:100}")
  private int batchSize;

  @Value("${booking.outbox.poll.fixed-delay-ms:500}")
  private long fixedDelayMs;

  @Value("${booking.outbox.topic:booking-events}")
  private String defaultTopic;

  /**
   * Polls and publishes outbox messages.
   */
  @Scheduled(fixedDelayString = "${booking.outbox.poll.fixed-delay-ms:500}")
  public void pollAndPublish() {
    List<OutboxMessage> batch = outboxRepository.findNextBatch(batchSize);
    if (batch.isEmpty()) {
      return;
    }

    for (OutboxMessage msg : batch) {
      try {
        // key = aggregateId (keeps ordering per booking)
        String t = (msg.getTopic() == null || msg.getTopic().isBlank()) ? defaultTopic : msg.getTopic();
        kafkaTemplate.send(t, msg.getAggregateId().toString(), msg.getPayload()).get();
        outboxRepository.markPublished(msg.getId(), Instant.now());
      } catch (Exception e) {
        // Stop on first failure to avoid hot-looping and to keep ordering stable.
        log.warn("Failed to publish outbox message id={} (will retry). cause={}", msg.getId(), e.toString());
        return;
      }
    }
  }
}
