CREATE TABLE IF NOT EXISTS product
(
    id              UUID,
    name            VARCHAR(64) NOT NULL,
    description     TEXT,
    price           DECIMAL     NOT NULL CHECK (price > 0),
    PRIMARY KEY (id)
);