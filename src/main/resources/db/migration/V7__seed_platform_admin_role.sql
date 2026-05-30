-- Platform administration (provider plane): the reserved PLATFORM_ADMIN role lives in the
-- platform tenant and is never assignable inside a regular tenant. The matching platform
-- admin user is created by DataSeeder (it needs a BCrypt-encoded password).
INSERT INTO roles (name, description, tenant_id)
VALUES (
    'PLATFORM_ADMIN',
    'SaaS provider operator: manages tenants (create, plan, suspend).',
    1
)
ON CONFLICT (tenant_id, name) DO NOTHING;
