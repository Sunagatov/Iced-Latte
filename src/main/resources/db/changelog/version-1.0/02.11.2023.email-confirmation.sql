--add email_confirmed and confirmation_token columns to user_details table
ALTER TABLE user_details
    ADD COLUMN email_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN confirmation_token VARCHAR;