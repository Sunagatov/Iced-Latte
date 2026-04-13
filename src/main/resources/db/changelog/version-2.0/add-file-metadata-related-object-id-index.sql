CREATE INDEX IF NOT EXISTS idx_file_metadata_related_object_id
    ON public.file_metadata (related_object_id);
