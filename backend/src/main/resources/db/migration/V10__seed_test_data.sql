-- Test data seed for demo/portfolio purposes.
-- All passwords are Admin@123 (BCrypt strength 12).
--
-- Org A — Acme Corp (PRO)   slug: acme
--   owner@acme.com   → OWNER
--   dev@acme.com     → MEMBER
--   viewer@acme.com  → VIEWER
--
-- Org B — Globex Inc (FREE) slug: globex
--   owner@globex.com → OWNER
--   admin@globex.com → ADMIN

DO $$
DECLARE
    -- Org A
    v_acme_id      UUID := '00000000-0000-0000-0000-000000000010';
    v_acme_owner   UUID := '00000000-0000-0000-0000-000000000011';
    v_acme_dev     UUID := '00000000-0000-0000-0000-000000000012';
    v_acme_viewer  UUID := '00000000-0000-0000-0000-000000000013';

    -- Org B
    v_globex_id    UUID := '00000000-0000-0000-0000-000000000020';
    v_globex_owner UUID := '00000000-0000-0000-0000-000000000021';
    v_globex_admin UUID := '00000000-0000-0000-0000-000000000022';

    -- Projects (Acme)
    v_proj_alpha   UUID := '00000000-0000-0000-0000-000000000031';
    v_proj_beta    UUID := '00000000-0000-0000-0000-000000000032';
    v_proj_gamma   UUID := '00000000-0000-0000-0000-000000000033';

    -- Projects (Globex)
    v_proj_delta   UUID := '00000000-0000-0000-0000-000000000041';

    HASH CONSTANT TEXT := '$2a$12$4UqNc7nc8aOHwQAyeF6GOeiAiCrdKfxoZU9BwJ4fnC32wLw.2kwFq';
BEGIN

    -- ── Organizations ────────────────────────────────────────────────────────────

    INSERT INTO organizations (id, name, slug, subscription_tier) VALUES
        (v_acme_id,   'Acme Corp',   'acme',   'PRO'),
        (v_globex_id, 'Globex Inc',  'globex', 'FREE')
    ON CONFLICT DO NOTHING;

    -- ── Users ────────────────────────────────────────────────────────────────────

    INSERT INTO users (id, email, password_hash, full_name) VALUES
        (v_acme_owner,   'owner@acme.com',   HASH, 'Alice Owner'),
        (v_acme_dev,     'dev@acme.com',     HASH, 'Bob Developer'),
        (v_acme_viewer,  'viewer@acme.com',  HASH, 'Carol Viewer'),
        (v_globex_owner, 'owner@globex.com', HASH, 'Dave Owner'),
        (v_globex_admin, 'admin@globex.com', HASH, 'Eve Admin')
    ON CONFLICT DO NOTHING;

    -- ── Members ──────────────────────────────────────────────────────────────────

    INSERT INTO organization_members (organization_id, user_id, role) VALUES
        (v_acme_id,   v_acme_owner,   'OWNER'),
        (v_acme_id,   v_acme_dev,     'MEMBER'),
        (v_acme_id,   v_acme_viewer,  'VIEWER'),
        (v_globex_id, v_globex_owner, 'OWNER'),
        (v_globex_id, v_globex_admin, 'ADMIN')
    ON CONFLICT DO NOTHING;

    -- ── Projects ─────────────────────────────────────────────────────────────────

    INSERT INTO projects (id, tenant_id, name, description, status, created_by, updated_by, created_at) VALUES
        (v_proj_alpha, v_acme_id, 'Alpha Platform',
         'Core SaaS platform rewrite — migrating monolith to microservices.',
         'ACTIVE', v_acme_owner, v_acme_owner, NOW() - INTERVAL '45 days'),

        (v_proj_beta, v_acme_id, 'Beta Mobile App',
         'React Native app for iOS and Android with offline support.',
         'ACTIVE', v_acme_dev, v_acme_dev, NOW() - INTERVAL '20 days'),

        (v_proj_gamma, v_acme_id, 'Gamma Analytics',
         'Internal BI dashboard for product and sales metrics.',
         'DRAFT', v_acme_owner, v_acme_owner, NOW() - INTERVAL '5 days'),

        (v_proj_delta, v_globex_id, 'Delta Portal',
         'Customer self-service portal with billing and support.',
         'ACTIVE', v_globex_owner, v_globex_owner, NOW() - INTERVAL '30 days')
    ON CONFLICT DO NOTHING;

    -- ── Activity logs ────────────────────────────────────────────────────────────

    INSERT INTO activity_logs (tenant_id, project_id, user_id, action, detail, created_at) VALUES
        (v_acme_id, v_proj_alpha, v_acme_owner,  'PROJECT_CREATED',  '{"name":"Alpha Platform"}',              NOW() - INTERVAL '45 days'),
        (v_acme_id, v_proj_alpha, v_acme_owner,  'MEMBER_INVITED',   '{"email":"dev@acme.com","role":"MEMBER"}',NOW() - INTERVAL '44 days'),
        (v_acme_id, v_proj_alpha, v_acme_dev,    'PROJECT_UPDATED',  '{"status":"ACTIVE"}',                    NOW() - INTERVAL '40 days'),
        (v_acme_id, v_proj_beta,  v_acme_dev,    'PROJECT_CREATED',  '{"name":"Beta Mobile App"}',             NOW() - INTERVAL '20 days'),
        (v_acme_id, v_proj_beta,  v_acme_dev,    'PROJECT_UPDATED',  '{"description":"added offline support"}',NOW() - INTERVAL '10 days'),
        (v_acme_id, v_proj_gamma, v_acme_owner,  'PROJECT_CREATED',  '{"name":"Gamma Analytics"}',             NOW() - INTERVAL '5 days'),
        (v_acme_id, NULL,         v_acme_owner,  'MEMBER_INVITED',   '{"email":"viewer@acme.com","role":"VIEWER"}', NOW() - INTERVAL '3 days'),
        (v_globex_id, v_proj_delta, v_globex_owner, 'PROJECT_CREATED','{"name":"Delta Portal"}',              NOW() - INTERVAL '30 days'),
        (v_globex_id, NULL,       v_globex_owner, 'MEMBER_INVITED',   '{"email":"admin@globex.com","role":"ADMIN"}', NOW() - INTERVAL '28 days'),
        (v_globex_id, v_proj_delta, v_globex_admin,'PROJECT_UPDATED', '{"status":"ACTIVE"}',                   NOW() - INTERVAL '25 days')
    ON CONFLICT DO NOTHING;

END
$$;
