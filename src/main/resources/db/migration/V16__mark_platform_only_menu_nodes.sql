-- Mark menu nodes that belong exclusively to the provider/platform plane.
-- These nodes must never appear in the role-permissions catalogue for tenant roles,
-- and cannot be assigned to any role outside the platform tenant.

ALTER TABLE menu_nodes ADD COLUMN platform_only BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE menu_nodes SET platform_only = TRUE
WHERE code IN ('PLATFORM', 'PLATFORM_ADMIN_GRP', 'PLATFORM_TENANTS');
