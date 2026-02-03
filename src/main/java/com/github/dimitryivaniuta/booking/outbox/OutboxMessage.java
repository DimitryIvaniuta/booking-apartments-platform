package com.github.dimitryivaniuta.booking.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transactional outbox message.
 *
 * <p>Written in the same DB transaction as the business update, then published asynchronously to Kafka.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "outbox")
public class OutboxMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "topic", nullable = false, length = 128)
  private String topic;

  @Column(name = "aggregate_type", nullable = false, length = 64)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  /**
   * Creates a new outbox message.
   *
   * @param topic kafka topic
   * @param aggregateType aggregate type
   * @param aggregateId aggregate id
   * @param eventType event type
   * @param payload json payload
   */
  public OutboxMessage(String topic, String aggregateType, UUID aggregateId, String eventType, String payload) {
    this.topic = topic;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    this.createdAt = Instant.now();
  }
}
