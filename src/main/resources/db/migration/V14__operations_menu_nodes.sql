-- Operations & Field domain: MODULE → GROUPs → ITEMs in the global menu catalogue.
-- These nodes are picked up automatically by TenantProvisioningService for new tenants.
-- Existing tenant ADMIN roles are updated by V15.

-- Top-level module
INSERT INTO menu_nodes (code, label, node_type, sort_order)
VALUES ('OPERATIONS', 'Operations', 'MODULE', 50);

-- Groups
INSERT INTO menu_nodes (code, label, node_type, parent_id, sort_order) VALUES
    ('OPS_MASTER_GRP',  'Masters',       'GROUP', (SELECT id FROM menu_nodes WHERE code = 'OPERATIONS'), 10),
    ('OPS_WORK_GRP',    'Work',          'GROUP', (SELECT id FROM menu_nodes WHERE code = 'OPERATIONS'), 20),
    ('OPS_CONFIG_GRP',  'Configuration', 'GROUP', (SELECT id FROM menu_nodes WHERE code = 'OPERATIONS'), 30);

-- Items
INSERT INTO menu_nodes (code, label, node_type, route, parent_id, sort_order) VALUES
    ('OPS_CLIENTS',     'Clients',       'ITEM', '/operations/clients',     (SELECT id FROM menu_nodes WHERE code = 'OPS_MASTER_GRP'), 10),
    ('OPS_VEHICLES',    'Vehicles',      'ITEM', '/operations/vehicles',    (SELECT id FROM menu_nodes WHERE code = 'OPS_MASTER_GRP'), 20),
    ('OPS_OSI',         'Service Orders','ITEM', '/operations/osi',         (SELECT id FROM menu_nodes WHERE code = 'OPS_WORK_GRP'),   10),
    ('OPS_EVENT_TYPES', 'Event Types',   'ITEM', '/operations/event-types', (SELECT id FROM menu_nodes WHERE code = 'OPS_CONFIG_GRP'), 10);
