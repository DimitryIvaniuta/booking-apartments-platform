-- Finalize bookings.user_id migration to UUID and add FK.

-- Best-effort migration: for any existing bookings without user_id_uuid, map to a default seeded user.
UPDATE bookings
SET user_id_uuid = COALESCE(user_id_uuid, '22222222-2222-2222-2222-222222222222'::uuid)
WHERE user_id_uuid IS NULL;

-- Drop legacy column and rename
ALTER TABLE bookings
  DROP COLUMN IF EXISTS user_id;

ALTER TABLE bookings
  RENAME COLUMN user_id_uuid TO user_id;

ALTER TABLE bookings
  ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE bookings
  ADD CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id);
