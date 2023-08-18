CREATE TABLE IF NOT EXISTS city_dictionary
(
    id              UUID              PRIMARY KEY,
    country         VARCHAR(64)       NOT NULL,
    city            VARCHAR(64)       NOT NULL
);

COMMENT ON TABLE city_dictionary                IS 'Справочник городов';

COMMENT ON COLUMN city_dictionary.id            IS 'Уникальный идентификатор города';
COMMENT ON COLUMN city_dictionary.country       IS 'Страна, в которой находится город';
COMMENT ON COLUMN city_dictionary.city          IS 'Город';