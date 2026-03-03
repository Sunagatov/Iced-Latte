CREATE TABLE IF NOT EXISTS public.user_granted_authority
(
    id        UUID PRIMARY KEY,
    user_id   UUID           NOT NULL,
    authority VARCHAR(32) NOT NULL
)