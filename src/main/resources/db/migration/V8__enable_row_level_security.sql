-- Layer-2 tenant isolation: PostgreSQL Row-Level Security (authoritative).
-- The application connects as a dedicated NON-owner role (no BYPASSRLS) so policies always
-- apply, even to find-by-id paths the Hibernate filter cannot cover. Migrations and seeding
-- keep running as the owner role (gmm_user), which bypasses RLS.

-- 1) Dedicated runtime role -------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'stepcore_app') THEN
        CREATE ROLE stepcore_app LOGIN PASSWORD 'stepcore_app_pass';
    END IF;
END $$;

GRANT USAGE ON SCHEMA public TO stepcore_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO stepcore_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO stepcore_app;

-- Future objects created by the owner (later migrations) inherit the same grants.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO stepcore_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO stepcore_app;

-- 2) Enable RLS + tenant policies on tenant-owned tables --------------------
-- current_setting('app.current_tenant', true) returns NULL when unset (missing_ok),
-- so an unset context matches no rows (deny by default).

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_users ON users
    USING (tenant_id = current_setting('app.current_tenant', true)::bigint)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::bigint);

ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_roles ON roles
    USING (tenant_id = current_setting('app.current_tenant', true)::bigint)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::bigint);

ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_audit ON audit_logs
    USING (tenant_id = current_setting('app.current_tenant', true)::bigint)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::bigint);

-- role_menu_options has no own tenant_id; it is scoped through its role's tenant.
ALTER TABLE role_menu_options ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_role_menu ON role_menu_options
    USING (role_id IN (SELECT id FROM roles WHERE tenant_id = current_setting('app.current_tenant', true)::bigint))
    WITH CHECK (role_id IN (SELECT id FROM roles WHERE tenant_id = current_setting('app.current_tenant', true)::bigint));
