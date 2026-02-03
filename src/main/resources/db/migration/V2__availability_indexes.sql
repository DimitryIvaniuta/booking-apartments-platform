-- Indexes to make availability search fast (city/capacity filters).
-- We query using lower(city) for case-insensitive matching.
CREATE INDEX IF NOT EXISTS idx_apartments_city_capacity ON apartments (lower(city), capacity);
