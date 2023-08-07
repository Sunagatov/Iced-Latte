CREATE TABLE IF NOT EXISTS product
(
    id              UUID,
    name            VARCHAR(64) NOT NULL,
    description     TEXT,
    price           DECIMAL     NOT NULL CHECK (price > 0),
    currency        VARCHAR     NOT NULL,
    old_quantity    INT         DEFAULT 0,
    quantity        INT         NOT NULL CHECK (quantity >= 0),
    active          BOOLEAN     NOT NULL,
    PRIMARY KEY (id)
);