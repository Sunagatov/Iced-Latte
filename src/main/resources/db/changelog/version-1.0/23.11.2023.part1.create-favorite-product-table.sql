CREATE TABLE IF NOT EXISTS public.favorite_list
(
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT current_timestamp,

    CONSTRAINT fk_favorite_user
        FOREIGN KEY (user_id)
            REFERENCES user_details (id)
            ON DELETE CASCADE
);