ALTER TABLE marlin.user
    ALTER COLUMN password DROP NOT NULL,
    ADD COLUMN verified BOOLEAN;

CREATE TYPE marlin.email_type AS ENUM ('WELCOME', 'EMAIL_VERIFICATION', 'MAGIC_LINK', 'TOO_MANY_FAILED_LOGIN_ATTEMPTS');

CREATE TABLE marlin.email
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT            NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,
    type       marlin.email_type NOT NULL,
    sent_at    TIMESTAMP,
    error      TEXT,
    created_at TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TRIGGER updated_at_trap
    BEFORE UPDATE
    ON marlin.email
EXECUTE FUNCTION marlin.sync_updated_at_column();

CREATE TABLE marlin.failed_login_attempt
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT    NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,
    ip_address   TEXT      NOT NULL,
    attempted_at TIMESTAMP NOT NULL
);

CREATE TABLE marlin.login_blacklist
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT    NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,
    ip_address    TEXT      NOT NULL,
    country       TEXT,
    city          TEXT,
    region        TEXT,
    blocked_at    TIMESTAMP NOT NULL,
    blocked_until TIMESTAMP NOT NULL
);