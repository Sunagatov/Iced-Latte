CREATE TABLE product(
    id          UUID PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    description TEXT,
    price       NUMERIC     NOT NULL CHECK (price > 0),
    currency    VARCHAR     NOT NULL,
    quantity    INT         NOT NULL CHECK (quantity >= 0),
    active      BOOLEAN     NOT NULL
);