CREATE TABLE shipping
(
    id                         BIGSERIAL PRIMARY KEY,
    shipping_user_email        VARCHAR(255) NOT NULL,
    shipping_user_first_name   VARCHAR(255) NOT NULL,
    shipping_user_last_name    VARCHAR(255) NOT NULL,
    shipping_user_phone_number VARCHAR(50)  NOT NULL,
    shipping_method            VARCHAR(255) NOT NULL,
    user_id                    UUID,
    country                    VARCHAR(255) NOT NULL,
    address_line               VARCHAR(255) NOT NULL,
    city                       VARCHAR(255) NOT NULL,
    zip_code                   VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_shipping_user FOREIGN KEY (user_id) REFERENCES user_details (id)
);

CREATE INDEX idx_shipping_user_id ON shipping (user_id);
