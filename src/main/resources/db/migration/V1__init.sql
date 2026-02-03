-- Enable extensions required for GiST exclusion on (uuid, daterange)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS apartments (
  id uuid PRIMARY KEY,
  name varchar(200) NOT NULL,
  city varchar(120) NOT NULL,
  capacity int NOT NULL,
  created_at timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS bookings (
  id uuid PRIMARY KEY,
  apartment_id uuid NOT NULL REFERENCES apartments(id),
  user_id varchar(120) NOT NULL,
  start_date date NOT NULL,
  end_date date NOT NULL,
  status varchar(32) NOT NULL,
  expires_at timestamptz NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  -- Generated range used for exclusion constraint. '[)' makes end exclusive (check-out date).
  stay daterange GENERATED ALWAYS AS (daterange(start_date, end_date, '[)')) STORED
);

CREATE INDEX IF NOT EXISTS idx_bookings_apartment_dates ON bookings(apartment_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_expires_at ON bookings(expires_at) WHERE expires_at IS NOT NULL;

-- No-overlap guarantee for active bookings (not CANCELLED and not EXPIRED).
-- Concurrent overlapping inserts will fail with SQLSTATE 23P01 (exclusion_violation).
ALTER TABLE bookings
  ADD CONSTRAINT bookings_no_overlap
  EXCLUDE USING gist (
    apartment_id WITH =,
    stay WITH &&
  )
  WHERE (status <> 'CANCELLED' AND status <> 'EXPIRED');

CREATE TABLE IF NOT EXISTS outbox (
  id bigserial PRIMARY KEY,
  aggregate_type varchar(64) NOT NULL,
  aggregate_id uuid NOT NULL,
  event_type varchar(64) NOT NULL,
  payload jsonb NOT NULL,
  created_at timestamptz NOT NULL,
  published_at timestamptz NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished ON outbox(id) WHERE published_at IS NULL;
