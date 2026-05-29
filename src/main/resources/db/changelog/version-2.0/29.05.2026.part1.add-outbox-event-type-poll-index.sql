CREATE INDEX IF NOT EXISTS ix_outbox_events_event_type_poll
    ON outbox_events (event_type, status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'FAILED_RETRYABLE');
