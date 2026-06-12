-- Idempotency store for Stripe webhook events.
-- No RLS — this is a global system table; saas_user (admin) writes via the webhook path.
CREATE TABLE stripe_events (
    id           VARCHAR(255) PRIMARY KEY,   -- Stripe evt_xxx ID
    type         VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

GRANT SELECT, INSERT ON stripe_events TO saas_app;
