CREATE TABLE IF NOT EXISTS file_metadata
(
    id              UUID NOT NULL PRIMARY KEY,
    bucket_name     VARCHAR(255),
    file_name       VARCHAR(255),
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
