-- Seed roles, users and sample apartments.

-- Roles
INSERT INTO roles (id, name) VALUES
  ('44444444-4444-4444-4444-444444444444', 'USER'),
  ('55555555-5555-5555-5555-555555555555', 'ADMIN')
ON CONFLICT (name) DO NOTHING;

-- Users (bcrypt cost=12)
-- admin@local.test / AdminPassword123!
-- user1@local.test / UserPassword123!
-- user2@local.test / UserPassword123!
INSERT INTO users (id, email, password_hash, enabled, email_verified, created_at, updated_at) VALUES
  ('11111111-1111-1111-1111-111111111111', 'admin@local.test', '$2b$12$fEuRGMHjgdU9jJinDIaSwuZVfzKRJTMajLj0zWOCYMehCsOkMB.56', true, true, now(), now()),
  ('22222222-2222-2222-2222-222222222222', 'user1@local.test',  '$2b$12$SnAlLUP7ALwMHjLikc112e08o7fRGxGFnk7iQMqVqR1ws997PwUnm', true, true, now(), now()),
  ('33333333-3333-3333-3333-333333333333', 'user2@local.test',  '$2b$12$SnAlLUP7ALwMHjLikc112e08o7fRGxGFnk7iQMqVqR1ws997PwUnm', true, true, now(), now())
ON CONFLICT (email) DO NOTHING;

-- Role mapping
INSERT INTO user_roles (user_id, role_id) VALUES
  ('11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555555555'),
  ('11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444'),
  ('22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444'),
  ('33333333-3333-3333-3333-333333333333', '44444444-4444-4444-4444-444444444444')
ON CONFLICT DO NOTHING;

-- Apartments
INSERT INTO apartments (id, name, city, capacity, created_at) VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Old Town Loft', 'Gdansk', 2, now()),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Family Flat', 'Gdansk', 4, now()),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'City Studio', 'Warsaw', 2, now()),
  ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Business Suite', 'Warsaw', 3, now()),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Seaside Apartment', 'Sopot', 5, now())
ON CONFLICT (id) DO NOTHING;
