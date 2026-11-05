-- liquibase formatted sql

-- changeset codex:add-operation-quotas
CREATE TABLE daily_operation_quota (
    scope VARCHAR(128) PRIMARY KEY,
    usage_date DATE NOT NULL,
    used BIGINT NOT NULL CHECK (used >= 0)
);

-- rollback DROP TABLE daily_operation_quota;

-- changeset codex:add-avatar-source-cleanup-marker
ALTER TABLE user_avatar_upload ADD COLUMN source_cleanup_scheduled BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_avatar_source_cleanup ON user_avatar_upload (expires_at) WHERE source_cleanup_scheduled = FALSE;

-- rollback DROP INDEX idx_avatar_source_cleanup;
-- rollback ALTER TABLE user_avatar_upload DROP COLUMN source_cleanup_scheduled;
