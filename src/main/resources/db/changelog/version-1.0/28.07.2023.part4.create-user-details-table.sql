CREATE TABLE user_details
(
    id                      UUID        NOT NULL PRIMARY KEY,
    first_name              VARCHAR(55) NOT NULL,
    last_name               VARCHAR(55) NOT NULL,
    user_name               VARCHAR(55) NOT NULL,
    email                   VARCHAR(55) NOT NULL,
    password                VARCHAR(55) NOT NULL,
    address_id              UUID,
    account_non_expired     BOOLEAN     NOT NULL,
    account_non_locked      BOOLEAN     NOT NULL,
    credentials_non_expired BOOLEAN     NOT NULL,
    enabled                 BOOLEAN     NOT NULL,
    CONSTRAINT fk_address
        FOREIGN KEY (address_id)
            REFERENCES address (id)
            ON DELETE CASCADE
);
