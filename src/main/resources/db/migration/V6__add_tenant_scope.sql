-- Retrofit tenant scoping onto Phase-1 tables.
-- Strategy: add nullable tenant_id, backfill existing rows to the legacy tenant,
-- then enforce NOT NULL + FK + index, and replace global unique keys with per-tenant ones.
-- Note: role_menu_options is intentionally NOT given its own tenant_id; it is scoped
-- through its role_id FK (preserving the JPA @ManyToMany mapping). Row-Level Security
-- for that table (added in a later migration) derives the tenant via roles.tenant_id.

DO $$
DECLARE
    legacy_tenant CONSTANT BIGINT := 2;
BEGIN
    -- roles ----------------------------------------------------------------
    ALTER TABLE roles ADD COLUMN tenant_id BIGINT;
    UPDATE roles SET tenant_id = legacy_tenant;
    ALTER TABLE roles ALTER COLUMN tenant_id SET NOT NULL;
    ALTER TABLE roles ADD CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
    ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_key;
    ALTER TABLE roles ADD CONSTRAINT uq_roles_tenant_name UNIQUE (tenant_id, name);
    CREATE INDEX idx_roles_tenant ON roles(tenant_id);

    -- users ----------------------------------------------------------------
    ALTER TABLE users ADD COLUMN tenant_id BIGINT;
    UPDATE users SET tenant_id = legacy_tenant;
    ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
    ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
    ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
    ALTER TABLE users ADD CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email);
    CREATE INDEX idx_users_tenant ON users(tenant_id);

    -- audit_logs -----------------------------------------------------------
    ALTER TABLE audit_logs ADD COLUMN tenant_id BIGINT;
    UPDATE audit_logs SET tenant_id = legacy_tenant;
    ALTER TABLE audit_logs ALTER COLUMN tenant_id SET NOT NULL;
    ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
    CREATE INDEX idx_audit_tenant ON audit_logs(tenant_id);
END $$;
