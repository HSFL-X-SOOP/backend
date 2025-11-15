CREATE TYPE marlin.payment_status AS ENUM ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'REQUIRES_ACTION');

CREATE TABLE IF NOT EXISTS marlin.payment
(
    id                       UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id                  BIGINT                NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,
    stripe_payment_intent_id TEXT                  NOT NULL UNIQUE,
    amount                   BIGINT                NOT NULL,
    currency                 TEXT                  NOT NULL,
    status                   marlin.payment_status NOT NULL,
    metadata                 JSONB,
    created_at               TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at               TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_payments_user_id ON marlin.payment (user_id);
CREATE INDEX idx_payments_stripe_id ON marlin.payment (stripe_payment_intent_id);
CREATE INDEX idx_payments_status ON marlin.payment (status);
CREATE INDEX idx_payments_created_at ON marlin.payment (created_at DESC);

COMMENT ON TABLE marlin.payment IS 'Stores payment transactions processed through Stripe';
COMMENT ON COLUMN marlin.payment.amount IS 'Amount in smallest currency unit (e.g., cents)';
COMMENT ON COLUMN marlin.payment.metadata IS 'Additional metadata from Stripe webhook events';

CREATE TRIGGER updated_at_sync
    BEFORE UPDATE
    ON marlin.payment
EXECUTE FUNCTION marlin.sync_updated_at_column();