ALTER TABLE public.favorite_list
    ADD CONSTRAINT uq_favorite_list_user_id UNIQUE (user_id);
