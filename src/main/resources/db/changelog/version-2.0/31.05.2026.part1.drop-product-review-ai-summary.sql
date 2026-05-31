-- liquibase formatted sql

-- changeset zufar:drop-product-review-ai-summary
ALTER TABLE product_reviews
    DROP COLUMN IF EXISTS ai_summary;
