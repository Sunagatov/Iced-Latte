DELETE FROM public.user_granted_authority uga
WHERE NOT EXISTS (
    SELECT 1
    FROM public.user_details u
    WHERE u.id = uga.user_id
);

DELETE FROM public.user_granted_authority uga
USING public.user_granted_authority duplicate
WHERE uga.user_id = duplicate.user_id
  AND uga.authority = duplicate.authority
  AND uga.id > duplicate.id;

ALTER TABLE public.user_granted_authority
    ADD CONSTRAINT fk_user_granted_authority_user
        FOREIGN KEY (user_id)
            REFERENCES public.user_details (id)
            ON DELETE CASCADE;

ALTER TABLE public.user_granted_authority
    ADD CONSTRAINT uk_user_granted_authority_user_authority
        UNIQUE (user_id, authority);
