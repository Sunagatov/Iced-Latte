CREATE TYPE USER_AUTHORITY AS ENUM ('USER');

CREATE TABLE IF NOT EXISTS user_granted_authority
(
    id        UUID PRIMARY KEY,
    user_id   UUID           NOT NULL,
    authority USER_AUTHORITY NOT NULL
)