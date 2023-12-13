CREATE TABLE IF NOT EXISTS avatar_info
(
    id              UUID NOT NULL PRIMARY KEY,
    bucket_name     VARCHAR(255),
    file_name       VARCHAR(255),
    avatar_url      TEXT,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
