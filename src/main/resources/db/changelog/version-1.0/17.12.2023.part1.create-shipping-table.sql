CREATE TABLE shipping
(
    shipping_id   SERIAL PRIMARY KEY,
    shipping_name VARCHAR(255),
    user_id       BIGINT,
    address_id    UUID,
    CONSTRAINT fk_shipping_user FOREIGN KEY (user_id) REFERENCES user_details (id),
    CONSTRAINT fk_shipping_address FOREIGN KEY (address_id) REFERENCES address (id)
);
