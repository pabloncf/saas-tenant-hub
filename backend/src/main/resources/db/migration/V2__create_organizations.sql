-- Create the application role used for all app queries (non-superuser → subject to RLS)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'saas_app') THEN
        CREATE ROLE saas_app WITH LOGIN PASSWORD 'saas_app_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE saas_dashboard TO saas_app;
GRANT USAGE ON SCHEMA public TO saas_app;

-- Grant privileges on all future tables/sequences created by saas_user in this schema
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO saas_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO saas_app;

-- Tenant root table
CREATE TABLE organizations (
    id         UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Explicit grant because ALTER DEFAULT PRIVILEGES only covers future tables
GRANT SELECT, INSERT, UPDATE, DELETE ON organizations TO saas_app;
