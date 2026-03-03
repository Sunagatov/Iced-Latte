CREATE TABLE public.delivery_address
(
    id         UUID         NOT NULL PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES user_details(id) ON DELETE CASCADE,
    label      VARCHAR(64)  NOT NULL,
    line       VARCHAR(256) NOT NULL,
    city       VARCHAR(128) NOT NULL,
    country    VARCHAR(128) NOT NULL,
    postcode   VARCHAR(16)  NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_delivery_address_user_id ON public.delivery_address(user_id);
