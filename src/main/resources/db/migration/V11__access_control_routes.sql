-- Access Control hub routes and catalogue screen under Security administration.

UPDATE menu_nodes SET route = '/admin/access/roles' WHERE code = 'ROLE_MANAGEMENT';
UPDATE menu_nodes SET route = '/admin/access/users' WHERE code = 'USER_MANAGEMENT';

INSERT INTO menu_nodes (code, label, node_type, route, parent_id, sort_order)
VALUES (
    'ACCESS_CONTROL',
    'Access Control',
    'ITEM',
    '/admin/access',
    (SELECT id FROM menu_nodes WHERE code = 'SECURITY_ADMIN'),
    5
);

INSERT INTO menu_nodes (code, label, node_type, route, parent_id, sort_order)
VALUES (
    'MENU_CATALOGUE',
    'Menu Catalogue',
    'ITEM',
    '/admin/access/menu',
    (SELECT id FROM menu_nodes WHERE code = 'SECURITY_ADMIN'),
    8
);

INSERT INTO role_menu_nodes (role_id, menu_node_id)
SELECT r.id, mn.id
FROM roles r
JOIN menu_nodes mn ON mn.code IN ('ACCESS_CONTROL', 'MENU_CATALOGUE')
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_menu_nodes (role_id, menu_node_id)
SELECT r.id, mn.id
FROM roles r
JOIN menu_nodes mn ON mn.code = 'ACCESS_CONTROL'
WHERE r.name = 'PLATFORM_ADMIN'
ON CONFLICT DO NOTHING;
