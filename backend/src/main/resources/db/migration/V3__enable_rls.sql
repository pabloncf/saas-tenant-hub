-- Enable Row Level Security on the tenant root table
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;

-- FORCE ensures the policy applies even to the table owner (saas_user).
-- Note: PostgreSQL superusers always bypass RLS regardless of FORCE.
-- The app connects as saas_app (non-superuser), so RLS is always active for app queries.
ALTER TABLE organizations FORCE ROW LEVEL SECURITY;

-- A single permissive policy covering all DML operations.
-- USING  → controls visibility for SELECT, UPDATE, DELETE
-- WITH CHECK → controls which rows can be written via INSERT, UPDATE
--
-- Guards against empty/unset tenant to prevent accidental full-table access.
CREATE POLICY tenant_isolation ON organizations
    AS PERMISSIVE
    FOR ALL
    TO saas_app
    USING (
        current_setting('app.current_tenant', true) IS NOT NULL
        AND current_setting('app.current_tenant', true) <> ''
        AND id = current_setting('app.current_tenant', true)::UUID
    )
    WITH CHECK (
        current_setting('app.current_tenant', true) IS NOT NULL
        AND current_setting('app.current_tenant', true) <> ''
        AND id = current_setting('app.current_tenant', true)::UUID
    );
