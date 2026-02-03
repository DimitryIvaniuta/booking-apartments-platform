package com.github.dimitryivaniuta.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.booking.api.dto.BookingHoldRequest;
import com.github.dimitryivaniuta.booking.domain.Booking;
import com.github.dimitryivaniuta.booking.domain.BookingStatus;
import com.github.dimitryivaniuta.booking.outbox.OutboxMessage;
import com.github.dimitryivaniuta.booking.outbox.OutboxRepository;
import com.github.dimitryivaniuta.booking.repo.ApartmentRepository;
import com.github.dimitryivaniuta.booking.repo.BookingRepository;
import com.github.dimitryivaniuta.booking.security.AuthenticatedUser;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for bookings.
 *
 * <p><b>Correctness:</b> double-booking is prevented by a PostgreSQL exclusion constraint
 * on (apartment_id, stay daterange) for non-cancelled/non-expired bookings (see Flyway migrations).</p>
 *
 * <p><b>p95/p99:</b> the write path is a short transaction (insert booking + insert outbox row),
 * with Kafka publish deferred to the outbox poller. Idempotency is backed by Redis to prevent duplicate work.</p>
 */
@Service
@RequiredArgsConstructor
public class BookingService {

  private static final String IDEMP_IN_PROGRESS = "IN_PROGRESS";

  private final ApartmentRepository apartmentRepository;
  private final BookingRepository bookingRepository;
  private final OutboxRepository outboxRepository;
  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher events;

  @Value("${booking.holds.default-minutes:15}")
  private int defaultHoldMinutes;

  @Value("${booking.idempotency.ttl-hours:24}")
  private int idempotencyTtlHours;

  /**
   * Checks if the apartment is available for the requested date range.
   *
   * @param apartmentId apartment id
   * @param from check-in (inclusive)
   * @param to check-out (exclusive)
   * @return true if available
   */
  @Transactional(readOnly = true)
  public boolean isAvailable(UUID apartmentId, LocalDate from, LocalDate to) {
    validateDates(from, to);
    requireApartmentExists(apartmentId);
    return !bookingRepository.existsOverlap(apartmentId, from, to);
  }

  /**
   * Creates a booking hold, concurrency-safe.
   *
   * <p>If multiple clients try to hold overlapping dates at the same time,
   * only one insert will succeed; the others fail with SQLSTATE 23P01 (exclusion violation),
   * mapped to {@code 409 CONFLICT}.</p>
   *
   * <p>Idempotency is scoped by user to prevent cross-user key collisions.</p>
   *
   * @param user authenticated user
   * @param req hold request
   * @param idempotencyKey optional idempotency key
   * @return created booking
   */
  @Transactional
  public Booking createHold(AuthenticatedUser user, BookingHoldRequest req, String idempotencyKey) {
    validateDates(req.startDate(), req.endDate());
    requireApartmentExists(req.apartmentId());

    UUID existing = idempotencyKey != null ? tryGetIdempotentResult(user.userId(), idempotencyKey) : null;
    if (existing != null) {
      return bookingRepository.findById(existing)
          .orElseThrow(() -> new NotFoundException("Idempotency mapping points to missing booking: " + existing));
    }

    Instant expiresAt = Instant.now().plus(Duration.ofMinutes(defaultHoldMinutes));
    Booking booking = Booking.newHold(req.apartmentId(), user.userId(), req.startDate(), req.endDate(), expiresAt);

    try {
      Booking saved = bookingRepository.saveAndFlush(booking);
      publishOutbox(saved, "BookingHeld", Map.of(
          "bookingId", saved.getId(),
          "apartmentId", saved.getApartmentId(),
          "userId", saved.getUserId().toString(),
          "startDate", saved.getStartDate().toString(),
          "endDate", saved.getEndDate().toString(),
          "status", saved.getStatus().name(),
          "expiresAt", saved.getExpiresAt().toString()
      ));
      if (idempotencyKey != null) {
        finalizeIdempotencyKey(user.userId(), idempotencyKey, saved.getId());
      }
      events.publishEvent(new BookingDataChangedEvent());
      return saved;
    } catch (DataIntegrityViolationException e) {
      if (SqlStateUtil.isPgExclusionViolation(e)) {
        throw new ConflictException("Apartment is already booked for the requested dates.", e);
      }
      throw e;
    } finally {
      if (idempotencyKey != null) {
        releaseInProgressIfNeeded(user.userId(), idempotencyKey);
      }
    }
  }

  /**
   * Confirms a hold booking (owner-or-admin).
   *
   * @param user authenticated user
   * @param bookingId booking id
   * @param paymentRef payment reference (demo)
   * @return updated booking
   */
  @Transactional
  public Booking confirm(AuthenticatedUser user, UUID bookingId, String paymentRef) {
    Booking b = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
    ensureOwnerOrAdmin(user, b);

    if (b.getStatus() != BookingStatus.HOLD) {
      throw new ConflictException("Only HOLD booking can be confirmed. Current status: " + b.getStatus());
    }
    if (b.isHoldExpired(Instant.now())) {
      b.expire();
      bookingRepository.save(b);
      throw new ConflictException("Hold already expired.");
    }
    b.confirm();
    Booking saved = bookingRepository.save(b);
    publishOutbox(saved, "BookingConfirmed", Map.of(
        "bookingId", saved.getId(),
        "paymentRef", paymentRef,
        "status", saved.getStatus().name()
    ));
    events.publishEvent(new BookingDataChangedEvent());
    return saved;
  }

  /**
   * Cancels a booking (owner-or-admin).
   *
   * @param user authenticated user
   * @param bookingId booking id
   * @return updated booking
   */
  @Transactional
  public Booking cancel(AuthenticatedUser user, UUID bookingId) {
    Booking b = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
    ensureOwnerOrAdmin(user, b);

    if (b.getStatus() == BookingStatus.CANCELLED) {
      return b;
    }
    b.cancel();
    Booking saved = bookingRepository.save(b);
    publishOutbox(saved, "BookingCancelled", Map.of(
        "bookingId", saved.getId(),
        "status", saved.getStatus().name()
    ));
    events.publishEvent(new BookingDataChangedEvent());
    return saved;
  }

  /**
   * Expires holds that passed their expiry time.
   *
   * <p>Used by a scheduled job. It does not require user authentication.</p>
   *
   * @return number of expired holds
   */
  @Transactional
  public int expireHolds() {
    Instant now = Instant.now();
    int updated = bookingRepository.expireHolds(now);
    if (updated > 0) {
      publishOutbox(UUID.randomUUID(), "BookingHoldsExpired", Map.of("count", updated, "at", now.toString()));
      events.publishEvent(new BookingDataChangedEvent());
    }
    return updated;
  }

  /**
   * Loads a booking by id (owner-or-admin).
   *
   * @param user authenticated user
   * @param id id
   * @return booking
   */
  @Transactional(readOnly = true)
  public Booking get(AuthenticatedUser user, UUID id) {
    Booking b = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found: " + id));
    ensureOwnerOrAdmin(user, b);
    return b;
  }

  private void ensureOwnerOrAdmin(AuthenticatedUser user, Booking booking) {
    if (user == null) {
      throw new ForbiddenException("Not authenticated.");
    }
    if (user.isAdmin()) {
      return;
    }
    if (!booking.getUserId().equals(user.userId())) {
      throw new ForbiddenException("You are not allowed to access this booking.");
    }
  }

  private void validateDates(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      throw new BadRequestException("startDate and endDate are required.");
    }
    if (!start.isBefore(end)) {
      throw new BadRequestException("startDate must be before endDate (endDate is exclusive checkout date).");
    }
  }

  private void requireApartmentExists(UUID apartmentId) {
    if (apartmentId == null) {
      throw new BadRequestException("apartmentId is required.");
    }
    if (!apartmentRepository.existsById(apartmentId)) {
      throw new NotFoundException("Apartment not found: " + apartmentId);
    }
  }

  private void publishOutbox(Booking booking, String eventType, Map<String, Object> payload) {
    publishOutbox(booking.getId(), eventType, payload);
  }

  private void publishOutbox(UUID aggregateId, String eventType, Map<String, Object> payload) {
    try {
      String json = objectMapper.writeValueAsString(payload);
      outboxRepository.save(new OutboxMessage("booking-events", "Booking", aggregateId, eventType, json));
    } catch (Exception e) {
      throw new BookingException("Failed to serialize outbox payload.", e);
    }
  }

  private String redisKey(UUID userId, String idempotencyKey) {
    return "idemp:hold:" + userId + ":" + idempotencyKey;
  }

  private UUID tryGetIdempotentResult(UUID userId, String idempotencyKey) {
    String key = redisKey(userId, idempotencyKey);
    String existing = redis.opsForValue().get(key);
    if (existing == null) {
      boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, IDEMP_IN_PROGRESS, Duration.ofSeconds(30)));
      if (!locked) {
        existing = redis.opsForValue().get(key);
      } else {
        return null;
      }
    }
    if (IDEMP_IN_PROGRESS.equals(existing)) {
      throw new ConflictException("Request with the same Idempotency-Key is already in progress.");
    }
    try {
      return UUID.fromString(existing);
    } catch (Exception e) {
      throw new ConflictException("Invalid idempotency value stored in Redis for key: " + idempotencyKey);
    }
  }

  private void finalizeIdempotencyKey(UUID userId, String idempotencyKey, UUID bookingId) {
    redis.opsForValue().set(redisKey(userId, idempotencyKey), bookingId.toString(), Duration.ofHours(idempotencyTtlHours));
  }

  private void releaseInProgressIfNeeded(UUID userId, String idempotencyKey) {
    String key = redisKey(userId, idempotencyKey);
    String existing = redis.opsForValue().get(key);
    if (IDEMP_IN_PROGRESS.equals(existing)) {
      redis.delete(key);
    }
  }
}
