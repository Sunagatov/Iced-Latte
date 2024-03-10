CREATE TABLE IF NOT EXISTS orders
(
    id                          UUID        PRIMARY KEY,
    user_id                     UUID        NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL,
    order_status                VARCHAR(55) NOT NULL,
    items_quantity              INT         NOT NULL CHECK (items_quantity >= 0),
    delivery_cost               DECIMAL     NOT NULL CHECK (delivery_cost > 0),
    tax_cost                    DECIMAL     NOT NULL CHECK (tax_cost > -1),
    delivery_info               VARCHAR(55) NOT NULL,
    recipient_name              VARCHAR(55) NOT NULL,
    recipient_surname           VARCHAR(55) NOT NULL,
    email                       VARCHAR(55) NOT NULL,
    phone_number                VARCHAR(25)
)
