CREATE TABLE public.user_details
(
    id                      UUID         NOT NULL PRIMARY KEY,
    first_name              VARCHAR(128)  NOT NULL,
    last_name               VARCHAR(128)  NOT NULL,
    birth_date              DATE,
    phone_number            VARCHAR(25),
    stripe_customer_token   VARCHAR(64) UNIQUE,
    email                   VARCHAR(55)  NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    address_id              UUID,
    account_non_expired     BOOLEAN      NOT NULL,
    account_non_locked      BOOLEAN      NOT NULL,
    credentials_non_expired BOOLEAN      NOT NULL,
    enabled                 BOOLEAN      NOT NULL,

    CONSTRAINT fk_address
        FOREIGN KEY (address_id)
            REFERENCES address (id)
            ON DELETE CASCADE
);
