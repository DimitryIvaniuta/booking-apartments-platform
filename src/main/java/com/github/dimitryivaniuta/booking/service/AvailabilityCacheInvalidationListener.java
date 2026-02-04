package com.github.dimitryivaniuta.booking.service;

import com.github.dimitryivaniuta.booking.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Invalidates cached availability search results after booking writes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvailabilityCacheInvalidationListener {

  private final CacheManager cacheManager;

  /**
   * Clears availability cache after the booking transaction commits.
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onBookingChanged(BookingDataChangedEvent event) {
    var cache = cacheManager.getCache(CacheConfig.AVAILABILITY_SEARCH_CACHE);
    if (cache != null) {
      cache.clear();
      log.debug("Cleared cache: {}", CacheConfig.AVAILABILITY_SEARCH_CACHE);
    }
  }
}
