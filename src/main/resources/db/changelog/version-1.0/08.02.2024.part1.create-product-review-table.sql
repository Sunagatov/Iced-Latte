CREATE TABLE IF NOT EXISTS product_reviews
(
    id                          UUID        PRIMARY KEY,
    product_id                  UUID        NOT NULL,
    user_id                     UUID        NOT NULL,
    created_at                  TIMESTAMPTZ DEFAULT current_timestamp,
    text                        VARCHAR(1500) NOT NULL,

    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
	        REFERENCES user_details(id)
	        ON DELETE CASCADE,

    CONSTRAINT fk_product
        FOREIGN KEY(product_id)
	        REFERENCES product(id)
	        ON DELETE CASCADE
)
