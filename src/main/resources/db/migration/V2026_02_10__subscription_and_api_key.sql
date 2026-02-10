CREATE TYPE marlin.subscription_type AS ENUM ('APP_NOTIFICATION', 'API_ACCESS');
CREATE TYPE marlin.subscription_status AS ENUM (
    'ACTIVE', 'TRIALING', 'PAST_DUE', 'CANCELED', 'UNPAID',
    'INCOMPLETE', 'INCOMPLETE_EXPIRED', 'PAUSED'
    );

CREATE TABLE marlin.subscription
(
    id                     UUID PRIMARY KEY                    DEFAULT gen_random_uuid(),
    user_id                BIGINT                     NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,
    stripe_customer_id     TEXT                       NOT NULL,
    stripe_subscription_id TEXT                       NOT NULL UNIQUE,
    stripe_price_id        TEXT                       NOT NULL,
    subscription_type      marlin.subscription_type   NOT NULL,
    status                 marlin.subscription_status NOT NULL,
    current_period_start   TIMESTAMPTZ,
    current_period_end     TIMESTAMPTZ,
    cancel_at_period_end   BOOLEAN                    NOT NULL DEFAULT FALSE,
    trial_start            TIMESTAMPTZ,
    trial_end              TIMESTAMPTZ,
    canceled_at            TIMESTAMPTZ,
    created_at             TIMESTAMPTZ                NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ                NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscription_user_id ON marlin.subscription (user_id);
CREATE INDEX idx_subscription_stripe_sub_id ON marlin.subscription (stripe_subscription_id);
CREATE INDEX idx_subscription_status ON marlin.subscription (status);

CREATE UNIQUE INDEX idx_unique_active_sub ON marlin.subscription (user_id, subscription_type)
    WHERE status IN ('ACTIVE', 'TRIALING', 'PAST_DUE');

CREATE TRIGGER updated_at_sync
    BEFORE UPDATE
    ON marlin.subscription
EXECUTE FUNCTION marlin.sync_updated_at_column();

CREATE TABLE marlin.api_key
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id      BIGINT      NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,
    key_prefix   VARCHAR(12) NOT NULL,
    key_hash     TEXT        NOT NULL,
    name         TEXT,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at   TIMESTAMPTZ
);

CREATE INDEX idx_api_key_user_id ON marlin.api_key (user_id);
CREATE INDEX idx_api_key_prefix ON marlin.api_key (key_prefix);

CREATE UNIQUE INDEX idx_unique_active_api_key ON marlin.api_key (user_id) WHERE is_active = TRUE;

ALTER TABLE marlin.user
    ADD COLUMN stripe_customer_id TEXT UNIQUE;

CREATE TRIGGER audit
    AFTER INSERT OR UPDATE OR DELETE
    ON marlin.subscription
    FOR EACH ROW
EXECUTE FUNCTION audit.audit();

CREATE TRIGGER audit
    AFTER INSERT OR UPDATE OR DELETE
    ON marlin.api_key
    FOR EACH ROW
EXECUTE FUNCTION audit.audit();
