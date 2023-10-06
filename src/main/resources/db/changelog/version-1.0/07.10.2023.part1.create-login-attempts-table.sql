CREATE TABLE user_attempts
(
    id                  UUID        NOT NULL PRIMARY KEY,
    user_id             UUID        NOT NULL,
    attempts            INTEGER     NOT NULL CHECK (attempts >= 0),
    is_user_locked      BOOLEAN     NOT NULL,
    expiration_datetime TIMESTAMPTZ CHECK (last_modified <= NOW()),
    lastModified        TIMESTAMPTZ NOT NULL CHECK (last_modified <= NOW()),
    CONSTRAINT fk_user_id
        FOREIGN KEY (username)
            REFERENCES user_details (id)
            ON DELETE CASCADE
);
