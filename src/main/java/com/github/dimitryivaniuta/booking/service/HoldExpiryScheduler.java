package com.github.dimitryivaniuta.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically expires booking holds that have passed their expiry time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryScheduler {

  private final BookingService bookingService;

  /**
   * Expires holds.
   */
  @Scheduled(fixedDelayString = "${booking.holds.expiry-scan-ms:10000}")
  public void expireHolds() {
    int expired = bookingService.expireHolds();
    if (expired > 0) {
      log.info("expiredHolds={}", expired);
    }
  }
}
