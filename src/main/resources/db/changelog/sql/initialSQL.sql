CREATE TABLE IF NOT EXISTS product
(
    id          UUID,
    name        VARCHAR(64) NOT NULL,
    description TEXT,
    price       DECIMAL     NOT NULL CHECK (price > 0),
    currency    VARCHAR     NOT NULL,
    quantity    INT         NOT NULL CHECK (quantity >= 0),
    active      BOOLEAN     NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS address
(
    id      UUID,
    line    VARCHAR(75) NOT NULL,
    city    VARCHAR(75) NOT NULL,
    country VARCHAR(75) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS customer
(
    id         UUID,
    first_name VARCHAR(50) NOT NULL,
    last_name  VARCHAR(50) NOT NULL,
    email      VARCHAR(75) NOT NULL,
    address_id UUID,
    PRIMARY KEY (id),
    FOREIGN KEY (address_id) REFERENCES product (id)
);

CREATE TABLE IF NOT EXISTS notification
(
    id           UUID,
    message      TEXT,
    recipient_id UUID,
    PRIMARY KEY (id),
    FOREIGN KEY (recipient_id) REFERENCES customer (id)
);

CREATE TABLE IF NOT EXISTS review
(
    id          UUID,
    product_id  VARCHAR(100) NOT NULL,
    customer_id VARCHAR(100),
    text        VARCHAR(100),
    rating      BIGINT,
    date        DATE,
    PRIMARY KEY (id)
);