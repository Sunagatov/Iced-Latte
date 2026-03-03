CREATE TABLE IF NOT EXISTS public.orders
(
    id                          UUID        PRIMARY KEY,
    user_id                     UUID        NOT NULL,
    session_id                  VARCHAR(255) NOT NULL,
    created_at                  TIMESTAMPTZ DEFAULT current_timestamp,
    status                      VARCHAR(55) NOT NULL,
    items_quantity              INT         NOT NULL CHECK (items_quantity >= 0),
    address_id                  UUID        NOT NULL,
    items_total_price           DECIMAL     NOT NULL CHECK (items_total_price > 0),
    recipient_name              VARCHAR(128) NOT NULL DEFAULT '',
    recipient_surname           VARCHAR(128) NOT NULL DEFAULT '',
    recipient_phone             VARCHAR(32),

    CONSTRAINT fk_address
        FOREIGN KEY (address_id)
            REFERENCES address (id)
            ON DELETE CASCADE
)