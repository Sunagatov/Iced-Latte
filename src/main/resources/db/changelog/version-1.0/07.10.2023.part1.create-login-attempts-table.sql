CREATE TABLE public.login_attempts
(
    id                  UUID        NOT NULL PRIMARY KEY,
    user_email          VARCHAR(55) NOT NULL UNIQUE,
    attempts            INTEGER     NOT NULL CHECK (attempts >= 0),
    is_user_locked      BOOLEAN     NOT NULL,
    expiration_datetime TIMESTAMPTZ,
    last_modified       TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_user_id
        FOREIGN KEY (user_email)
            REFERENCES user_details (email)
            ON DELETE CASCADE
);
