CREATE TABLE IF NOT EXISTS product
(
    id          UUID,
    name        VARCHAR(64) NOT NULL,
    description TEXT,
    price       DECIMAL     NOT NULL CHECK (price > 0),
    quantity    INT         NOT NULL CHECK (quantity >= 0),
    active      BOOLEAN     NOT NULL,
    PRIMARY KEY (id),
    file_metadata_id  UUID,

        CONSTRAINT fk_file_metadata
        FOREIGN KEY (file_metadata_id)
            REFERENCES file_metadata (id)
            ON DELETE CASCADE
);