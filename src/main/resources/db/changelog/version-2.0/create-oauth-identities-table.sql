-- liquibase formatted sql

-- changeset zufar:create-oauth-identities-table
CREATE TABLE IF NOT EXISTS oauth_identities
(
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES user_details (id) ON DELETE CASCADE,
    provider         VARCHAR(32)  NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email            VARCHAR(254) NOT NULL,
    created_at       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by       UUID,
    updated_by       UUID,
    CONSTRAINT oauth_identities_provider_subject_unique UNIQUE (provider, provider_subject)
);

CREATE INDEX IF NOT EXISTS oauth_identities_user_id_idx
    ON oauth_identities (user_id);
