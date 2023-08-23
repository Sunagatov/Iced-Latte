CREATE TABLE IF NOT EXISTS reservation
(
    id                          SERIAL                   PRIMARY KEY,
    reservation_id              UUID                     NOT NULL,
    warehouse_item_id           UUID                     NOT NULL,
    reserved_quantity           INT                      NOT NULL CHECK (reserved_quantity > 0),
    FOREIGN KEY (reservation_id) REFERENCES user_reservation_history(reservation_id),
    FOREIGN KEY (warehouse_item_id) REFERENCES warehouse(item_id),
    UNIQUE(reservation_id, warehouse_item_id)
);
