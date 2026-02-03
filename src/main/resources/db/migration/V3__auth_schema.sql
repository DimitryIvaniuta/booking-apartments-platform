-- Auth schema + outbox topic + booking user migration prep

-- Outbox: add topic (allows multiple logical topics from same outbox table).
ALTER TABLE outbox
  ADD COLUMN IF NOT EXISTS topic varchar(128);

UPDATE outbox SET topic = 'booking-events' WHERE topic IS NULL;
ALTER TABLE outbox ALTER COLUMN topic SET NOT NULL;

-- Users & roles
CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY,
  email varchar(200) NOT NULL UNIQUE,
  password_hash varchar(200) NOT NULL,
  enabled boolean NOT NULL,
  email_verified boolean NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
  id uuid PRIMARY KEY,
  name varchar(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_roles (
  user_id uuid NOT NULL REFERENCES users(id),
  role_id uuid NOT NULL REFERENCES roles(id),
  PRIMARY KEY (user_id, role_id)
);

-- Refresh token families (multi-device sessions)
CREATE TABLE IF NOT EXISTS refresh_token_families (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  device_id varchar(128) NULL,
  created_at timestamptz NOT NULL,
  last_used_at timestamptz NULL,
  revoked_at timestamptz NULL
);

CREATE INDEX IF NOT EXISTS idx_refresh_families_user ON refresh_token_families(user_id);

-- Refresh tokens (rotating, hashed)
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id uuid PRIMARY KEY,
  family_id uuid NOT NULL REFERENCES refresh_token_families(id),
  token_hash char(64) NOT NULL UNIQUE,
  created_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz NULL,
  replaced_by uuid NULL
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family ON refresh_tokens(family_id);

-- Email verification tokens (hashed)
CREATE TABLE IF NOT EXISTS email_verification_tokens (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  token_hash char(64) NOT NULL UNIQUE,
  created_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  used_at timestamptz NULL
);

CREATE INDEX IF NOT EXISTS idx_email_verif_user ON email_verification_tokens(user_id);

-- Prepare booking table migration: add new UUID column for user_id.
ALTER TABLE bookings
  ADD COLUMN IF NOT EXISTS user_id_uuid uuid NULL;
