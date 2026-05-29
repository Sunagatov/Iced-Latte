CREATE INDEX IF NOT EXISTS ix_inbox_events_consumer_event_type_poll
    ON inbox_events (consumer_name, event_type, status, next_attempt_at, created_at)
    WHERE status IN ('RECEIVED', 'FAILED_RETRYABLE');
