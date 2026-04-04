INSERT INTO menu_options (code, label, route, sort_order) VALUES
    ('ROLE_MANAGEMENT',   'Role Management',        '/admin/roles',    1),
    ('USER_MANAGEMENT',   'User Management',        '/admin/users',    2),
    ('PAYROLL_CONFIG',    'Payroll Configuration',  '/admin/config',   3),
    ('EMPLOYEE_CONFIG',   'Employee Configuration', '/admin/employees',4),
    ('TIME_RECORDS_ADMIN','Time Records (Admin)',   '/admin/time',     5),
    ('REPORTS',           'Reports',                '/reports',        6),
    ('MY_TIME',           'My Time Records',        '/my/time',       10),
    ('MY_PROFILE',        'My Profile',             '/my/profile',    11);

INSERT INTO roles (name, description) VALUES
    ('ADMIN',    'System administrator — full access to all modules'),
    ('EMPLOYEE', 'Regular employee — self-service time tracking and reports');

INSERT INTO role_menu_options (role_id, menu_option_id)
    SELECT r.id, m.id
    FROM roles r, menu_options m
    WHERE r.name = 'ADMIN';

INSERT INTO role_menu_options (role_id, menu_option_id)
    SELECT r.id, m.id
    FROM roles r, menu_options m
    WHERE r.name = 'EMPLOYEE'
      AND m.code IN ('MY_TIME', 'MY_PROFILE');
