package com.github.dimitryivaniuta.booking;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.redis.testcontainers.RedisContainer;

/**
 * Base class for integration tests backed by Testcontainers.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("booking")
          .withUsername("booking")
          .withPassword("booking");

  protected static final RedisContainer REDIS =
      new RedisContainer(DockerImageName.parse("redis:7"));

  protected static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @BeforeAll
  static void startContainers() {
    POSTGRES.start();
    REDIS.start();
    KAFKA.start();
  }

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);

    r.add("spring.data.redis.host", REDIS::getHost);
    r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

    r.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

    // speed up polling for tests
    r.add("booking.outbox.poll.fixed-delay-ms", () -> "200");
    r.add("booking.holds.expiry-scan-ms", () -> "200");
    r.add("booking.holds.default-minutes", () -> "1");
  }
}
