package com.github.dimitryivaniuta.booking.outbox;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for outbox messages.
 */
public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

  /**
   * Loads the next batch of unpublished messages.
   *
   * @param limit batch size
   * @return outbox messages
   */
  @Query(value = "select * from outbox where published_at is null order by id asc limit :limit", nativeQuery = true)
  List<OutboxMessage> findNextBatch(@Param("limit") int limit);

  /**
   * Marks one message as published.
   *
   * <p>Runs in its own transaction to avoid holding the original polling transaction while sending to Kafka.</p>
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(value = "update outbox set published_at = :publishedAt where id = :id and published_at is null", nativeQuery = true)
  int markPublished(@Param("id") long id, @Param("publishedAt") Instant publishedAt);
}
