CREATE TABLE IF NOT EXISTS favorite
(
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    CONSTRAINT fk_user
        FOREIGN KEY (user_id)
            REFERENCES user_details (id)
            ON DELETE CASCADE
);