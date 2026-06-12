-- Default admin user for development/demo purposes.
-- Organization: "Demo Organization" (slug: demo)
-- Email:        admin@demo.com
-- Password:     Admin@123  (BCrypt strength 12)
-- Role:         OWNER

DO $$
DECLARE
    v_org_id  UUID := '00000000-0000-0000-0000-000000000001';
    v_user_id UUID := '00000000-0000-0000-0000-000000000002';
BEGIN
    INSERT INTO organizations (id, name, slug, subscription_tier)
    VALUES (v_org_id, 'Demo Organization', 'demo', 'FREE')
    ON CONFLICT DO NOTHING;

    INSERT INTO users (id, email, password_hash, full_name)
    VALUES (
        v_user_id,
        'admin@demo.com',
        '$2a$12$4UqNc7nc8aOHwQAyeF6GOeiAiCrdKfxoZU9BwJ4fnC32wLw.2kwFq',
        'Admin'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO organization_members (organization_id, user_id, role)
    VALUES (v_org_id, v_user_id, 'OWNER')
    ON CONFLICT DO NOTHING;
END
$$;
