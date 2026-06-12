ALTER TABLE organizations
    ADD COLUMN subscription_tier      VARCHAR(20)  NOT NULL DEFAULT 'FREE'
                    CHECK (subscription_tier IN ('FREE', 'PRO', 'ENTERPRISE')),
    ADD COLUMN stripe_customer_id     VARCHAR(255),
    ADD COLUMN stripe_subscription_id VARCHAR(255);

CREATE UNIQUE INDEX idx_organizations_stripe_customer
    ON organizations(stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;
