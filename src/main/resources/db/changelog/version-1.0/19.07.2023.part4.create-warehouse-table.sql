CREATE TABLE IF NOT EXISTS warehouse
(
    item_id              UUID              PRIMARY KEY,
    store_id             UUID              NOT NULL,
    product_id           UUID              NOT NULL,
    quantity             INT               NOT NULL CHECK (quantity >= 0),
    tmp_quantity         INT               NOT NULL DEFAULT 0,
    FOREIGN KEY (store_id) REFERENCES store(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    UNIQUE(store_id, product_id)
);

COMMENT ON TABLE warehouse                      IS 'Таблица описывает склад продуктов';

COMMENT ON COLUMN warehouse.item_id             IS 'Уникальный идентификатор продукта на складе для каждой пары (store_id, product_id)';
COMMENT ON COLUMN warehouse.store_id            IS 'Идентификатор магазина';
COMMENT ON COLUMN warehouse.product_id          IS 'Идентификатор продукта';
COMMENT ON COLUMN warehouse.quantity            IS 'Количества данного продукта в данном магазине';
COMMENT ON COLUMN warehouse.tmp_quantity        IS 'Служебная колонка для обеспечения бронирования продуктов без блокировок и уменьшения количества запросов';
