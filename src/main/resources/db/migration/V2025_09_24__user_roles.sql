CREATE TYPE marlin.user_authority_role AS ENUM ('ADMIN', 'USER');

ALTER TABLE marlin.user
    ADD COLUMN role marlin.user_authority_role NOT NULL DEFAULT 'USER';

CREATE TYPE marlin.user_activity_role AS ENUM ('HARBOR_MASTER', 'SWIMMER', 'SAILOR', 'FISHERMAN');

CREATE TYPE marlin.language AS ENUM ('EN', 'DE');

CREATE TYPE marlin.measurement_system AS ENUM ('METRIC', 'IMPERIAL');

CREATE TABLE marlin.user_profile
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT                      NOT NULL UNIQUE REFERENCES marlin.user (id) ON DELETE CASCADE,
    language           marlin.language             NOT NULL DEFAULT 'EN',
    role               marlin.user_activity_role[] NOT NULL,
    measurement_system marlin.measurement_system   NOT NULL DEFAULT 'METRIC',
    created_at         TIMESTAMP                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP
);

CREATE TRIGGER updated_at_sync
    BEFORE UPDATE
    ON marlin.user_profile
EXECUTE FUNCTION marlin.sync_updated_at_column();

ALTER TRIGGER updated_at_trap
    ON marlin.user
    RENAME TO updated_at_sync;

ALTER TRIGGER updated_at_trap
    ON marlin.email
    RENAME TO updated_at_sync;