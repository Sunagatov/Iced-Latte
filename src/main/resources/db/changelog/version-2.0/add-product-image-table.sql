CREATE TABLE product_image (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID         NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    url        VARCHAR(2048) NOT NULL,
    position   SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_image_product_id ON product_image (product_id);
