-- Assign all four operations menu items to every existing role named 'ADMIN'
-- so that tenant admins on existing installations gain access immediately.
-- New tenants are handled by TenantProvisioningService (assigns all ITEM nodes).
INSERT INTO role_menu_nodes (role_id, menu_node_id)
SELECT r.id, mn.id
FROM roles r
JOIN menu_nodes mn ON mn.code IN ('OPS_CLIENTS', 'OPS_VEHICLES', 'OPS_OSI', 'OPS_EVENT_TYPES')
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
