-- liquibase formatted sql

-- changeset zufar:add-auth-sessions-table
CREATE TABLE IF NOT EXISTS auth_sessions (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES user_details (id) ON DELETE CASCADE,
    refresh_token_hash  VARCHAR(128) NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    last_used_at        TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    user_agent          VARCHAR(256),
    ip_address          VARCHAR(64),
    compromised         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_user_id
    ON auth_sessions (user_id);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_refresh_token_hash
    ON auth_sessions (refresh_token_hash);
