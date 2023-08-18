CREATE TYPE RESERVATION_STATUS AS ENUM ('CREATED', 'CONFIRMED', 'CANCELLED');

CREATE TABLE IF NOT EXISTS user_reservation_history
(
    reservation_id       UUID                          PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at           TIMESTAMP without TIME ZONE   NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    status               RESERVATION_STATUS            NOT NULL DEFAULT 'CREATED',
    user_id              UUID                          NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_details(id)
);

CREATE UNIQUE INDEX user_reservation_history_user_id_status_idx ON user_reservation_history (user_id, status)
WHERE status = 'CREATED';

COMMENT ON TABLE user_reservation_history                   IS 'Таблица описывает историю бронирований пользователя';

COMMENT ON COLUMN user_reservation_history.reservation_id   IS 'Идентификатор бронирования';
COMMENT ON COLUMN user_reservation_history.created_at       IS 'Дата создания бронирования';
COMMENT ON COLUMN user_reservation_history.status           IS 'Статус бронирования';
COMMENT ON COLUMN user_reservation_history.user_id          IS 'Идентификатор пользователя';
