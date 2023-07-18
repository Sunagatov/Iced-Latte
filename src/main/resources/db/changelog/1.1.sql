--changeset Alex Zarubin:1
CREATE TABLE reservation (
                             reservation_id UUID NOT NULL,
                             product_id UUID NOT NULL,
                             reserved_quantity INT NOT NULL CHECK(reserved_amount > 0),
                             created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             reservation_status VARCHAR(32) NOT NULL,
                             PRIMARY KEY (reservation_id)
);
CREATE UNIQUE INDEX uniq_reservation ON reservation (reservation_id, product_id);