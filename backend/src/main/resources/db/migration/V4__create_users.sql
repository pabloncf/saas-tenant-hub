-- Users table has no RLS: a user may belong to multiple organizations,
-- and auth operations (login, refresh) must read across tenant boundaries.
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

GRANT SELECT, INSERT, UPDATE, DELETE ON users TO saas_app;

-- Pivot between users and organizations, carries the role within that org.
CREATE TABLE organization_members (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('OWNER','ADMIN','MEMBER','VIEWER')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, user_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON organization_members TO saas_app;

-- RLS: a member may only see/modify records within their own organization.
ALTER TABLE organization_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_members FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON organization_members
    AS PERMISSIVE FOR ALL TO saas_app
    USING (
        current_setting('app.current_tenant', true) IS NOT NULL
        AND current_setting('app.current_tenant', true) <> ''
        AND organization_id = current_setting('app.current_tenant', true)::UUID
    )
    WITH CHECK (
        current_setting('app.current_tenant', true) IS NOT NULL
        AND current_setting('app.current_tenant', true) <> ''
        AND organization_id = current_setting('app.current_tenant', true)::UUID
    );
