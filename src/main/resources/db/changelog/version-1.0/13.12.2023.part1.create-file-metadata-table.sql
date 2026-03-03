create TABLE IF NOT EXISTS public.file_metadata
(
    id                UUID NOT NULL PRIMARY KEY,
    related_object_id UUID NOT NULL,
    bucket_name       VARCHAR(255),
    file_name         VARCHAR(255),
    created_at        TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
