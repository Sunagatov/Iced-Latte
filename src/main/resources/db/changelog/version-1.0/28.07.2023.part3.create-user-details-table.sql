CREATE TABLE user_details
(
    id         UUID         NOT NULL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    user_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    address_id UUID,
    CONSTRAINT fk_address
        FOREIGN KEY (address_id)
            REFERENCES address (id)
            ON DELETE CASCADE
);
