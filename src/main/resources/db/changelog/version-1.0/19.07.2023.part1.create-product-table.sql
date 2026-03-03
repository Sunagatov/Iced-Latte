CREATE TABLE IF NOT EXISTS public.product (
    id             UUID,
    name           VARCHAR(64) NOT NULL,
    description    TEXT,
    price          DECIMAL     NOT NULL CHECK (price > 0),
    quantity       INT         NOT NULL CHECK (quantity >= 0),
    active         BOOLEAN     NOT NULL,
    average_rating DECIMAL CHECK (average_rating >= 0 AND average_rating < 6) DEFAULT 0,
    reviews_count  INT CHECK (reviews_count >= 0)                             DEFAULT 0,
    brand_name      VARCHAR(64) NOT NULL,
    seller_name     VARCHAR(64) NOT NULL,
    origin_country  VARCHAR(128) NOT NULL,
    weight          INT NOT NULL CHECK (weight > 0),
    size_length     INT NOT NULL CHECK (size_length > 0),
    size_width      INT NOT NULL CHECK (size_width > 0),
    size_height     INT NOT NULL CHECK (size_height > 0),
    sold_products_count INT NOT NULL CHECK (sold_products_count > 0),
    discount        INT NOT NULL CHECK (discount > 0),
    date_added      TIMESTAMPTZ NOT NULL CHECK (date_added <= CURRENT_TIMESTAMP),
    popularity_score INT NOT NULL CHECK (popularity_score > 0),
    PRIMARY KEY (id)
);


