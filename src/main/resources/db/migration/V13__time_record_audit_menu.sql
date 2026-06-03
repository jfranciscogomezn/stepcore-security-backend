-- Time record audit history screen for time administrators.
INSERT INTO menu_nodes (code, label, node_type, route, parent_id, sort_order)
VALUES (
    'TIME_RECORD_AUDIT',
    'Time Record Audit',
    'ITEM',
    '/admin/time/audit',
    (SELECT id FROM menu_nodes WHERE code = 'TIME_ADMIN_GRP'),
    20
);

INSERT INTO role_menu_nodes (role_id, menu_node_id)
SELECT DISTINCT rmn.role_id, mn.id
FROM role_menu_nodes rmn
JOIN menu_nodes existing ON existing.id = rmn.menu_node_id AND existing.code = 'TIME_RECORDS_ADMIN'
JOIN menu_nodes mn ON mn.code = 'TIME_RECORD_AUDIT'
ON CONFLICT DO NOTHING;
