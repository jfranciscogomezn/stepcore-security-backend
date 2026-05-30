-- Multi-tenancy (Pool model): global tenants catalogue.
-- gen_random_uuid() requires pgcrypto.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE tenants (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    plan        VARCHAR(20)  NOT NULL DEFAULT 'STANDARD',
    max_users   INT          NOT NULL DEFAULT 50,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_platform BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Reserved tenants with fixed identifiers:
--  * platform: the SaaS provider plane (PLATFORM_ADMIN users), runs outside tenant scope.
--  * legacy:   holds all pre-existing Phase-1 data so current behaviour is preserved.
INSERT INTO tenants (id, name, slug, plan, max_users, status, is_platform) VALUES
    ('00000000-0000-0000-0000-000000000000', 'Platform', 'platform', 'PREMIUM', 100, 'ACTIVE', TRUE),
    ('00000000-0000-0000-0000-000000000001', 'Legacy',   'legacy',   'PREMIUM', 100, 'ACTIVE', FALSE);
