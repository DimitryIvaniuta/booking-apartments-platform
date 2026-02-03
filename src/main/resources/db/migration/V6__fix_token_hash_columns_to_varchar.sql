-- token_hash should be varchar(64) to match JPA default mapping
ALTER TABLE email_verification_tokens
ALTER COLUMN token_hash TYPE varchar(64)
  USING btrim(token_hash)::varchar(64);

ALTER TABLE refresh_tokens
ALTER COLUMN token_hash TYPE varchar(64)
  USING btrim(token_hash)::varchar(64);