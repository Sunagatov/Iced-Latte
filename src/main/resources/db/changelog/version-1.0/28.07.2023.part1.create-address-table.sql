CREATE TABLE address
(
    id       UUID        NOT NULL PRIMARY KEY,
    line     VARCHAR(55) NOT NULL,
    city     VARCHAR(55) NOT NULL,
    country  VARCHAR(55) NOT NULL,
    postcode VARCHAR(55) NOT NULL
);
