-- Modular menu: hierarchical catalogue (MODULE → GROUP → ITEM) and role assignments at leaves.

CREATE TABLE menu_nodes (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(100) NOT NULL UNIQUE,
    label      VARCHAR(150) NOT NULL,
    node_type  VARCHAR(20)  NOT NULL CHECK (node_type IN ('MODULE', 'GROUP', 'ITEM')),
    route      VARCHAR(200),
    icon       VARCHAR(50),
    parent_id  BIGINT       REFERENCES menu_nodes(id) ON DELETE CASCADE,
    sort_order INT          NOT NULL DEFAULT 0,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT menu_nodes_item_route_chk CHECK (
        (node_type = 'ITEM' AND route IS NOT NULL)
        OR (node_type <> 'ITEM' AND route IS NULL)
    ),
    CONSTRAINT menu_nodes_module_root_chk CHECK (
        (node_type = 'MODULE' AND parent_id IS NULL)
        OR (node_type <> 'MODULE')
    )
);

CREATE INDEX idx_menu_nodes_parent_sort ON menu_nodes (parent_id, sort_order);

-- Top-level modules
INSERT INTO menu_nodes (code, label, node_type, sort_order) VALUES
    ('SECURITY',      'Security',      'MODULE', 10),
    ('PAYROLL',       'Payroll',       'MODULE', 20),
    ('TIME_TRACKING', 'Time Tracking', 'MODULE', 30),
    ('ACCOUNT',       'Account',       'MODULE', 90),
    ('PLATFORM',      'Platform',      'MODULE', 100);

-- Groups
INSERT INTO menu_nodes (code, label, node_type, parent_id, sort_order) VALUES
    ('SECURITY_ADMIN',     'Administration', 'GROUP', (SELECT id FROM menu_nodes WHERE code = 'SECURITY'), 10),
    ('PAYROLL_CONFIG_GRP', 'Configuration',  'GROUP', (SELECT id FROM menu_nodes WHERE code = 'PAYROLL'), 10),
    ('PAYROLL_OPS_GRP',    'Operations',     'GROUP', (SELECT id FROM menu_nodes WHERE code = 'PAYROLL'), 20),
    ('TIME_ADMIN_GRP',     'Administration', 'GROUP', (SELECT id FROM menu_nodes WHERE code = 'TIME_TRACKING'), 10),
    ('TIME_SELF_GRP',      'Self-service',   'GROUP', (SELECT id FROM menu_nodes WHERE code = 'TIME_TRACKING'), 20),
    ('PLATFORM_ADMIN_GRP', 'Administration', 'GROUP', (SELECT id FROM menu_nodes WHERE code = 'PLATFORM'), 10);

-- Items (preserve legacy leaf codes and routes from menu_options)
INSERT INTO menu_nodes (code, label, node_type, route, parent_id, sort_order) VALUES
    ('ROLE_MANAGEMENT',    'Role Management',        'ITEM', '/admin/roles',     (SELECT id FROM menu_nodes WHERE code = 'SECURITY_ADMIN'), 10),
    ('USER_MANAGEMENT',    'User Management',        'ITEM', '/admin/users',     (SELECT id FROM menu_nodes WHERE code = 'SECURITY_ADMIN'), 20),
    ('PAYROLL_CONFIG',     'Payroll Configuration',  'ITEM', '/admin/config',    (SELECT id FROM menu_nodes WHERE code = 'PAYROLL_CONFIG_GRP'), 10),
    ('EMPLOYEE_CONFIG',    'Employee Configuration', 'ITEM', '/admin/employees', (SELECT id FROM menu_nodes WHERE code = 'PAYROLL_CONFIG_GRP'), 20),
    ('REPORTS',            'Reports',                'ITEM', '/reports',         (SELECT id FROM menu_nodes WHERE code = 'PAYROLL_OPS_GRP'), 10),
    ('TIME_RECORDS_ADMIN', 'Time Records (Admin)',   'ITEM', '/admin/time',      (SELECT id FROM menu_nodes WHERE code = 'TIME_ADMIN_GRP'), 10),
    ('MY_TIME',            'My Time Records',        'ITEM', '/my/time',         (SELECT id FROM menu_nodes WHERE code = 'TIME_SELF_GRP'), 10),
    ('MY_PROFILE',         'My Profile',             'ITEM', '/my/profile',      (SELECT id FROM menu_nodes WHERE code = 'ACCOUNT'), 10),
    ('PLATFORM_TENANTS',   'Tenants',                'ITEM', '/platform/tenants', (SELECT id FROM menu_nodes WHERE code = 'PLATFORM_ADMIN_GRP'), 10);

CREATE TABLE role_menu_nodes (
    role_id      BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    menu_node_id BIGINT NOT NULL REFERENCES menu_nodes(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, menu_node_id)
);

-- Migrate existing role assignments (ITEM nodes only, matched by code)
INSERT INTO role_menu_nodes (role_id, menu_node_id)
SELECT rmo.role_id, mn.id
FROM role_menu_options rmo
JOIN menu_options mo ON mo.id = rmo.menu_option_id
JOIN menu_nodes mn ON mn.code = mo.code AND mn.node_type = 'ITEM';

-- Platform admin: assign tenants item (platform tenant role)
INSERT INTO role_menu_nodes (role_id, menu_node_id)
SELECT r.id, mn.id
FROM roles r
JOIN menu_nodes mn ON mn.code = 'PLATFORM_TENANTS'
WHERE r.name = 'PLATFORM_ADMIN'
ON CONFLICT DO NOTHING;

ALTER TABLE role_menu_nodes ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_role_menu_nodes ON role_menu_nodes
    USING (role_id IN (SELECT id FROM roles WHERE tenant_id = current_setting('app.current_tenant', true)::bigint))
    WITH CHECK (role_id IN (SELECT id FROM roles WHERE tenant_id = current_setting('app.current_tenant', true)::bigint));

GRANT SELECT, INSERT, UPDATE, DELETE ON menu_nodes TO stepcore_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON role_menu_nodes TO stepcore_app;
GRANT USAGE, SELECT ON SEQUENCE menu_nodes_id_seq TO stepcore_app;
