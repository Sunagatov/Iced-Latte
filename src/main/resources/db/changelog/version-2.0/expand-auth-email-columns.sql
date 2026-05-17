-- liquibase formatted sql

-- changeset zufar:expand-auth-email-columns
ALTER TABLE user_details
    ALTER COLUMN email TYPE VARCHAR(254);

ALTER TABLE login_attempts
    ALTER COLUMN user_email TYPE VARCHAR(254);

ALTER TABLE oauth_identities
    ALTER COLUMN email TYPE VARCHAR(254);
