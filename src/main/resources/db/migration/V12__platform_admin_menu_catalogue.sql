-- Allow platform operators to open the menu catalogue administration screen.
INSERT INTO role_menu_nodes (role_id, menu_node_id)
SELECT r.id, mn.id
FROM roles r
JOIN menu_nodes mn ON mn.code = 'MENU_CATALOGUE'
WHERE r.name = 'PLATFORM_ADMIN'
ON CONFLICT DO NOTHING;
