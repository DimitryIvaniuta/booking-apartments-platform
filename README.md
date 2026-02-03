# Booking Apartments — Production-grade concurrency-safe booking service

Spring Boot 3.5.10 + Java 21 reference implementation for apartment bookings with **hard guarantees against double-booking** under high concurrency, and practical guidance to keep **p95/p99** latency stable.

## Key production choices

### 1) Correctness under concurrency (no double-booking)
- One request wins; the rest fail with SQLSTATE **23P01** (`exclusion_violation`).
- Application maps it to HTTP **409 Conflict**.

This avoids fragile “check-then-insert” logic and avoids relying on app-level locks only.

### 2) Tail latency (p95/p99) guardrails
The booking write path is a short transaction: **single insert to `bookings` + single insert to `outbox`**. Event publishing happens asynchronously.

Operational guardrails:
- Indexes + GiST exclusion index (fast conflict checks)
- Keep transactions short; no remote calls inside TX
- HikariCP tuned for your DB limits (avoid queueing at pool)
- Micrometer metrics via Actuator for latency + error rates

### 3) Reliable event publishing (Kafka) via Transactional Outbox
We implement the **Transactional Outbox pattern**: write business row + outbox row in the same DB transaction, then a background publisher polls and publishes to Kafka.

### 4) Rate limiting (protect p95/p99)
Requests are rate limited **per client** using Redis (`INCR` + `EXPIRE`).

- Client identity: `X-Client-Id` header if present, otherwise remote IP
- Default: 200 requests / 60 seconds for all `/api/**` endpoints

Configure via:
```yaml
app:
  rate-limit:
    enabled: true
    max-requests: 200
    window-seconds: 60
    path-prefixes: ["/api"]
```

### 5) Availability search + Redis read-side caching
The availability search endpoint is the hottest read path in booking systems, so it is:

1. Implemented as an efficient DB query (`NOT EXISTS` overlap check)
2. Backed by an index on `(lower(city), capacity)`
3. Cached in Redis for a short TTL (30s)

On any booking write (hold/confirm/cancel/expiry), availability cache is invalidated **after the transaction commits**.

## Architecture (high level)

- **PostgreSQL**: source of truth + overlap protection
- **Redis**:
  - idempotency keys to prevent duplicate creates
  - optional cache hooks (kept minimal)
- **Kafka**: `booking-events` topic for downstream systems (notifications, billing, analytics, etc.)


## Authentication (JWT + refresh tokens) + security enhancements

This project includes a production-grade baseline auth stack:

- **JWT access tokens** (RS256) + Spring Security resource server
- **Rotating refresh tokens** with **multi-device families**
- **Token revocation**:
  - single-token logout via JTI blacklist (Redis)
  - logout-all via per-user "valid-after" timestamp (Redis)
- **Account lockout** after repeated failed logins (Redis counter)
- **Password strength validation**
- **Email verification** flow via **Transactional Outbox + Kafka event** (`auth-events`)

### Seeded accounts
Flyway seeds users for local/dev/testing:

- Admin: `admin@local.test` / `AdminPassword123!`
- User: `user1@local.test` / `UserPassword123!`
- User: `user2@local.test` / `UserPassword123!`

### Auth endpoints
- `POST /api/auth/register`
- `GET /api/auth/verify?token=...`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all`
- `GET /api/auth/sessions`
- `POST /api/auth/sessions/{id}/revoke`

### Authorization rules
- `POST /api/apartments/**` requires **ADMIN**
- `/api/bookings/**` requires authentication; bookings are **owner-or-admin** readable/cancellable/confirmable.


## Running locally

### Prerequisites
- Java 21
- Docker (for Postgres/Redis/Kafka)
- Internet access for Gradle distribution download (wrapper will fetch it)

### Start infra
```bash
docker compose up -d
```

### Run app
```bash
./gradlew bootRun
```

App: `http://localhost:8080`

Actuator: `http://localhost:8080/actuator`

## API (main endpoints)

- `POST /api/apartments` — create apartment
- `GET /api/apartments/{id}` — get apartment
- `GET /api/apartments/{id}/availability?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/availability/search?city=Gdansk&capacity=2&from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20`
- `POST /api/bookings/hold` — create hold (concurrency safe)
- `POST /api/bookings/{id}/confirm`
- `POST /api/bookings/{id}/cancel`

### Idempotency
Send `Idempotency-Key` header to `POST /api/bookings/hold`.
If the same key is reused, the server returns the original booking.

## Postman
Import:
- `postman/BookingApartments.postman_collection.json`
- `postman/BookingApartments.local.postman_environment.json`

## Tests
Integration tests use **Testcontainers** (Postgres + Redis + Kafka).

```bash
./gradlew test
```

## Notes on date semantics
`startDate` is **check-in**, `endDate` is **check-out** (exclusive).  
So booking `[2026-02-01, 2026-02-05)` and `[2026-02-05, 2026-02-07)` do **not** overlap.

## License
MIT
