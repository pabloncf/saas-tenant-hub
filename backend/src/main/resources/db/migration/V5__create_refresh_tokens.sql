-- Refresh tokens: only the SHA-256 hash is stored, never the raw value.
-- No RLS: looked up by token hash before tenant context is known.
CREATE TABLE refresh_tokens (
    id              UUID     PRIMARY KEY DEFAULT uuid_generate_v4(),
    token_hash      CHAR(64) NOT NULL UNIQUE,   -- SHA-256 hex digest
    user_id         UUID     NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    organization_id UUID     NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Partial index: speeds up the common case of validating an active token.
CREATE INDEX idx_refresh_tokens_active ON refresh_tokens(user_id, organization_id)
    WHERE NOT revoked;

GRANT SELECT, INSERT, UPDATE, DELETE ON refresh_tokens TO saas_app;
