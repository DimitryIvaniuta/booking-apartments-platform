package com.github.dimitryivaniuta.booking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka topic configuration.
 *
 * <p>Auto-creates the main booking events topic when Kafka broker supports topic auto-creation.
 * In production, you typically create topics via IaC, but this helps local dev.</p>
 */
@Configuration
public class KafkaTopicConfig {

  /**
   * Creates the booking events topic.
   *
   * @param topic topic name
   * @return topic definition
   */
  @Bean
  public NewTopic bookingEventsTopic(@Value("${booking.outbox.topic:booking-events}") String topic) {
    return new NewTopic(topic, 6, (short) 1);
  }

  /**
   * Creates the auth events topic.
   *
   * @param topic topic name
   * @return topic definition
   */
  @Bean
  public NewTopic authEventsTopic(@Value("${auth.outbox.topic:auth-events}") String topic) {
    return new NewTopic(topic, 3, (short) 1);
  }

}
