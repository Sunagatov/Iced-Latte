-- liquibase formatted sql

-- changeset zufar:add-auth-sessions-previous-token-hash
ALTER TABLE auth_sessions
    ADD COLUMN IF NOT EXISTS previous_token_hash VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_previous_token_hash
    ON auth_sessions (previous_token_hash)
    WHERE previous_token_hash IS NOT NULL;
