CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL,
    topic VARCHAR(255) NOT NULL,
    partition_key VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 10,
    next_attempt_at TIMESTAMPTZ,
    locked_by VARCHAR(100),
    locked_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    kafka_partition INTEGER,
    kafka_offset BIGINT,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT outbox_events_status_check CHECK (status IN (
        'PENDING',
        'IN_PROGRESS',
        'PUBLISHED',
        'FAILED_RETRYABLE',
        'FAILED_PERMANENT',
        'CANCELLED'
    )),
    CONSTRAINT outbox_events_attempt_count_check CHECK (attempt_count >= 0),
    CONSTRAINT outbox_events_max_attempts_check CHECK (max_attempts > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_outbox_events_event_id
    ON outbox_events (event_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_outbox_events_aggregate_event
    ON outbox_events (aggregate_type, aggregate_id, event_type, event_version);

CREATE INDEX IF NOT EXISTS ix_outbox_events_poll
    ON outbox_events (status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'FAILED_RETRYABLE');

CREATE INDEX IF NOT EXISTS ix_outbox_events_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

CREATE INDEX IF NOT EXISTS ix_outbox_events_topic_partition_key
    ON outbox_events (topic, partition_key);

CREATE TABLE IF NOT EXISTS inbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL,
    topic VARCHAR(255) NOT NULL,
    partition_key VARCHAR(255) NOT NULL,
    kafka_partition INTEGER NOT NULL,
    kafka_offset BIGINT NOT NULL,
    consumer_name VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 10,
    next_attempt_at TIMESTAMPTZ,
    locked_by VARCHAR(100),
    locked_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT inbox_events_status_check CHECK (status IN (
        'RECEIVED',
        'IN_PROGRESS',
        'PROCESSED',
        'FAILED_RETRYABLE',
        'FAILED_PERMANENT',
        'IGNORED'
    )),
    CONSTRAINT inbox_events_attempt_count_check CHECK (attempt_count >= 0),
    CONSTRAINT inbox_events_max_attempts_check CHECK (max_attempts > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_inbox_events_event_consumer
    ON inbox_events (event_id, consumer_name);

CREATE UNIQUE INDEX IF NOT EXISTS ux_inbox_events_kafka_offset_consumer
    ON inbox_events (topic, kafka_partition, kafka_offset, consumer_name);

CREATE INDEX IF NOT EXISTS ix_inbox_events_poll
    ON inbox_events (status, next_attempt_at, created_at)
    WHERE status IN ('RECEIVED', 'FAILED_RETRYABLE');

CREATE INDEX IF NOT EXISTS ix_inbox_events_topic_partition_key
    ON inbox_events (topic, partition_key);
