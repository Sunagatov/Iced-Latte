CREATE TABLE address
(
    id      UUID         NOT NULL PRIMARY KEY,
    line    VARCHAR(255) NOT NULL,
    city    VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL
);
