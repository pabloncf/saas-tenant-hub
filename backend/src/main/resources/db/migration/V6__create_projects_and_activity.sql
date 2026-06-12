CREATE TABLE projects (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED')),
    created_by  UUID         NOT NULL REFERENCES users(id),
    updated_by  UUID         NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON projects
    AS PERMISSIVE FOR ALL TO saas_app
    USING (
        current_setting('app.current_tenant', true) IS NOT NULL
        AND current_setting('app.current_tenant', true) <> ''
        AND tenant_id = current_setting('app.current_tenant', true)::UUID
    )
    WITH CHECK (
        current_setting('app.current_tenant', true) IS NOT NULL
        AND current_setting('app.current_tenant', true) <> ''
        AND tenant_id = current_setting('app.current_tenant', true)::UUID
    );

GRANT SELECT, INSERT, UPDATE, DELETE ON projects TO saas_app;

-- Activity log is append-only: saas_app has no UPDATE or DELETE privileges.
CREATE TABLE activity_logs (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id  UUID         REFERENCES projects(id) ON DELETE SET NULL,
    user_id     UUID         NOT NULL REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,
    detail      TEXT,        -- JSON stored as text
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE activity_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_logs FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON activity_logs
    AS PERMISSIVE FOR ALL TO saas_app
    USING (
        current_setting('app.current_tenant', true) IS NOT NULL
        AND current_setting('app.current_tenant', true) <> ''
        AND tenant_id = current_setting('app.current_tenant', true)::UUID
    )
    WITH CHECK (
        current_setting('app.current_tenant', true) IS NOT NULL
        AND current_setting('app.current_tenant', true) <> ''
        AND tenant_id = current_setting('app.current_tenant', true)::UUID
    );

-- Append-only: SELECT + INSERT only
GRANT SELECT, INSERT ON activity_logs TO saas_app;
