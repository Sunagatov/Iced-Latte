CREATE TABLE IF NOT EXISTS store
(
    id                UUID              PRIMARY KEY,
    name              VARCHAR(64)       NOT NULL,
    description       TEXT              NOT NULL,
    city_id           UUID              NOT NULL,
    street            VARCHAR(64)       NOT NULL,
    house             VARCHAR(32)       NOT NULL,
    time_zone         VARCHAR(64)       NOT NULL,
    latitude          FLOAT             NOT NULL,
    longitude         FLOAT             NOT NULL,
    darkstore         BOOLEAN           NOT NULL,
    support_pickup    BOOLEAN           NOT NULL,
    support_delivery  BOOLEAN           NOT NULL,
    FOREIGN KEY (city_id) REFERENCES city_dictionary(id)
);

COMMENT ON TABLE store                    IS 'Таблица описывает магазин';

COMMENT ON COLUMN store.id                IS 'Уникальный идентификатор магазина';
COMMENT ON COLUMN store.name              IS 'Название магазина';
COMMENT ON COLUMN store.description       IS 'Подробное описание магазина';
COMMENT ON COLUMN store.street            IS 'Улица нахождения магазина';
COMMENT ON COLUMN store.house             IS 'Дом нахождения магазина';
COMMENT ON COLUMN store.time_zone         IS 'Таймзона нахождения магазина';
COMMENT ON COLUMN store.latitude          IS 'Широта нахождения магазина (координата y)';
COMMENT ON COLUMN store.longitude         IS 'Долгота нахождения магазина (координата x)';
COMMENT ON COLUMN store.darkstore         IS 'Признак того, что магазин только для онлайн заказов (без оффлайн покупателей)';
COMMENT ON COLUMN store.support_pickup    IS 'Признак того, что магазин предлагает опцию самовывоза';
COMMENT ON COLUMN store.support_delivery  IS 'Признак того, что магазин предлагает опцию доставки';