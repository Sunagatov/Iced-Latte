-- liquibase formatted sql

-- changeset zufar:add-product-image-table runOnChange:true
CREATE TABLE IF NOT EXISTS product_image (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    url        VARCHAR(2048) NOT NULL,
    position   SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_product_image_product_id
    ON product_image (product_id);