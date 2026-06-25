-- liquibase formatted sql

-- changeset codex:create-user-avatar-upload-table
CREATE TABLE IF NOT EXISTS user_avatar_upload (
    id                       UUID PRIMARY KEY,
    user_id                  UUID NOT NULL REFERENCES user_details (id) ON DELETE CASCADE,
    status                   VARCHAR(32) NOT NULL,
    original_bucket          VARCHAR(128) NOT NULL,
    original_key             VARCHAR(512) NOT NULL,
    processed_bucket         VARCHAR(128),
    processed_key            VARCHAR(512),
    content_type             VARCHAR(64) NOT NULL,
    original_size_bytes      BIGINT,
    processed_size_bytes     BIGINT,
    image_width              INTEGER,
    image_height             INTEGER,
    sha256                   VARCHAR(128),
    client_idempotency_key   VARCHAR(100),
    failure_code             VARCHAR(64),
    failure_message          VARCHAR(512),
    active                   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ NOT NULL,
    uploaded_at              TIMESTAMPTZ,
    processed_at             TIMESTAMPTZ,
    activated_at             TIMESTAMPTZ,
    superseded_at            TIMESTAMPTZ,
    expires_at               TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_user_avatar_upload_expires_after_created CHECK (expires_at > created_at),
    CONSTRAINT chk_user_avatar_upload_status CHECK (
        status IN (
            'PENDING_UPLOAD',
            'UPLOADED',
            'PROCESSING',
            'READY',
            'FAILED',
            'EXPIRED',
            'SUPERSEDED'
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_user_avatar_upload_user_created_at
    ON user_avatar_upload (user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_avatar_upload_original_key
    ON user_avatar_upload (original_bucket, original_key);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_avatar_upload_processed_key
    ON user_avatar_upload (processed_bucket, processed_key)
    WHERE processed_bucket IS NOT NULL AND processed_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_avatar_upload_user_active
    ON user_avatar_upload (user_id)
    WHERE active = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_avatar_upload_user_idempotency
    ON user_avatar_upload (user_id, client_idempotency_key)
    WHERE client_idempotency_key IS NOT NULL;
