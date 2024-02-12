CREATE TABLE IF NOT EXISTS reviews
(
    id                          UUID        PRIMARY KEY,
    user_id                     UUID        NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL,
    text                        VARCHAR(255) NOT NULL,
    rate                        INT         NOT NULL CHECK (rate >= 1 and rate <= 5)
)
