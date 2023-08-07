CREATE TYPE RESERVATION_STATUS AS ENUM ('CREATED', 'CONFIRMED', 'CANCELLED');

CREATE TABLE IF NOT EXISTS reservation
(
    id                SERIAL PRIMARY KEY,
    reservation_id    UUID                     NOT NULL,
    product_id        UUID                     NOT NULL,
    reserved_quantity INT                      NOT NULL CHECK (reserved_quantity > 0),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status            RESERVATION_STATUS       NOT NULL,
    UNIQUE(reservation_id, product_id)
);
