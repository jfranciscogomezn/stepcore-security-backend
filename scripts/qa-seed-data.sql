-- QA seed data for multi-tenant testing (ACME tenant id=3, Globex tenant id=4).
-- Idempotent: removes prior @acme.qa / @globex.qa rows before re-inserting.
-- Password for all QA employee users: Admin@2026! (same bcrypt as admin@stepcore.com)

BEGIN;

DO $$
DECLARE
    pwd_hash TEXT := '$2a$12$4sxNV2UlP34Ak5YQpgPb.On0gMn/7g.lQPDUkUZrLxavAA9VxY7CW';
    acme_tenant BIGINT := 3;
    globex_tenant BIGINT := 4;
    acme_employee_role BIGINT;
    globex_employee_role BIGINT;
    uid BIGINT;
    eid BIGINT;
BEGIN
    -- Payroll config (copy baseline from legacy tenant)
    INSERT INTO payroll_configs (
        tenant_id, year, minimum_wage, transport_subsidy, monthly_work_hours, normal_daily_hours,
        max_daily_extra_hours, daytime_start, daytime_end, daytime_ot_start, daytime_ot_end,
        night_surcharge_start, night_surcharge_end, nocturnal_ot_start, nocturnal_ot_end,
        sunday_ot_start, sunday_ot_end, daytime_ot_factor, nocturnal_ot_factor, night_surcharge_factor,
        sunday_holiday_daytime_ot_factor, sunday_holiday_nocturnal_ot_factor, sunday_holiday_normal_factor,
        non_billable_rest_minutes
    )
    SELECT t.id, 2026, minimum_wage, transport_subsidy, monthly_work_hours, normal_daily_hours,
           max_daily_extra_hours, daytime_start, daytime_end, daytime_ot_start, daytime_ot_end,
           night_surcharge_start, night_surcharge_end, nocturnal_ot_start, nocturnal_ot_end,
           sunday_ot_start, sunday_ot_end, daytime_ot_factor, nocturnal_ot_factor, night_surcharge_factor,
           sunday_holiday_daytime_ot_factor, sunday_holiday_nocturnal_ot_factor, sunday_holiday_normal_factor,
           non_billable_rest_minutes
    FROM payroll_configs pc
    CROSS JOIN (VALUES (acme_tenant), (globex_tenant)) AS t(id)
    WHERE pc.tenant_id = 2 AND pc.year = 2026
    ON CONFLICT (tenant_id, year) DO NOTHING;

    INSERT INTO holidays (tenant_id, holiday_date, description)
    SELECT t.id, '2026-05-25'::date, 'QA test holiday'
    FROM (VALUES (acme_tenant), (globex_tenant)) AS t(id)
    ON CONFLICT (tenant_id, holiday_date) DO NOTHING;

    -- Clean prior QA rows (time records -> employees -> users)
    DELETE FROM time_records tr
    USING employees e
    WHERE tr.employee_id = e.id
      AND e.tenant_id IN (acme_tenant, globex_tenant)
      AND e.email LIKE '%.qa';

    DELETE FROM employees
    WHERE tenant_id IN (acme_tenant, globex_tenant)
      AND email LIKE '%.qa';

    DELETE FROM users
    WHERE tenant_id IN (acme_tenant, globex_tenant)
      AND email LIKE '%.qa';

    -- EMPLOYEE roles with self-service menu
    INSERT INTO roles (name, description, tenant_id)
    VALUES ('EMPLOYEE', 'Regular employee — time tracking and profile', acme_tenant)
    ON CONFLICT (tenant_id, name) DO UPDATE SET description = EXCLUDED.description;

    INSERT INTO roles (name, description, tenant_id)
    VALUES ('EMPLOYEE', 'Regular employee — time tracking and profile', globex_tenant)
    ON CONFLICT (tenant_id, name) DO UPDATE SET description = EXCLUDED.description;

    SELECT id INTO acme_employee_role FROM roles WHERE tenant_id = acme_tenant AND name = 'EMPLOYEE';
    SELECT id INTO globex_employee_role FROM roles WHERE tenant_id = globex_tenant AND name = 'EMPLOYEE';

    INSERT INTO role_menu_nodes (role_id, menu_node_id)
    SELECT acme_employee_role, mn.id FROM menu_nodes mn
    WHERE mn.code IN ('MY_TIME', 'MY_PROFILE') AND mn.node_type = 'ITEM'
    ON CONFLICT DO NOTHING;

    INSERT INTO role_menu_nodes (role_id, menu_node_id)
    SELECT globex_employee_role, mn.id FROM menu_nodes mn
    WHERE mn.code IN ('MY_TIME', 'MY_PROFILE') AND mn.node_type = 'ITEM'
    ON CONFLICT DO NOTHING;

    -- ========== ACME Corp (tenant 3) ==========
    INSERT INTO users (first_name, last_name, email, phone, password_hash, enabled, must_change_password, role_id, tenant_id, updated_at)
    VALUES ('Maria', 'Lopez', 'maria.lopez@acme.qa', '3001110001', pwd_hash, TRUE, FALSE, acme_employee_role, acme_tenant, NOW())
    RETURNING id INTO uid;
    INSERT INTO employees (tenant_id, user_id, first_name, last_name, id_type, id_number, email, phone, monthly_salary)
    VALUES (acme_tenant, uid, 'Maria', 'Lopez', 'CC', '1001001001', 'maria.lopez@acme.qa', '3001110001', 2800000.00)
    RETURNING id INTO eid;
    INSERT INTO time_records (tenant_id, employee_id, work_date, clock_in, clock_out, status, corrected) VALUES
        (acme_tenant, eid, '2026-05-02', '2026-05-02 13:00:00+00', '2026-05-02 21:00:00+00', 'CLOSED', FALSE),
        (acme_tenant, eid, '2026-05-05', '2026-05-05 13:00:00+00', '2026-05-05 23:30:00+00', 'CLOSED', FALSE),
        (acme_tenant, eid, '2026-05-12', '2026-05-12 13:00:00+00', '2026-05-12 21:00:00+00', 'CLOSED', TRUE);

    INSERT INTO users (first_name, last_name, email, phone, password_hash, enabled, must_change_password, role_id, tenant_id, updated_at)
    VALUES ('Carlos', 'Ruiz', 'carlos.ruiz@acme.qa', '3001110002', pwd_hash, TRUE, FALSE, acme_employee_role, acme_tenant, NOW())
    RETURNING id INTO uid;
    INSERT INTO employees (tenant_id, user_id, first_name, last_name, id_type, id_number, email, phone, monthly_salary)
    VALUES (acme_tenant, uid, 'Carlos', 'Ruiz', 'CC', '1001001002', 'carlos.ruiz@acme.qa', '3001110002', 3200000.00)
    RETURNING id INTO eid;
    INSERT INTO time_records (tenant_id, employee_id, work_date, clock_in, clock_out, status, corrected) VALUES
        (acme_tenant, eid, '2026-05-08', '2026-05-08 02:00:00+00', '2026-05-08 10:00:00+00', 'CLOSED', FALSE),
        (acme_tenant, eid, '2026-05-15', '2026-05-15 02:00:00+00', '2026-05-15 11:00:00+00', 'CLOSED', FALSE),
        (acme_tenant, eid, '2026-05-25', '2026-05-25 13:00:00+00', '2026-05-25 21:00:00+00', 'CLOSED', FALSE);

    INSERT INTO users (first_name, last_name, email, phone, password_hash, enabled, must_change_password, role_id, tenant_id, updated_at)
    VALUES ('Ana', 'Torres', 'ana.torres@acme.qa', '3001110003', pwd_hash, TRUE, FALSE, acme_employee_role, acme_tenant, NOW())
    RETURNING id INTO uid;
    INSERT INTO employees (tenant_id, user_id, first_name, last_name, id_type, id_number, email, phone, monthly_salary)
    VALUES (acme_tenant, uid, 'Ana', 'Torres', 'CC', '1001001003', 'ana.torres@acme.qa', '3001110003', 2500000.00)
    RETURNING id INTO eid;
    INSERT INTO time_records (tenant_id, employee_id, work_date, clock_in, clock_out, status, corrected) VALUES
        (acme_tenant, eid, '2026-05-19', '2026-05-19 13:00:00+00', NULL, 'INCOMPLETE', FALSE),
        (acme_tenant, eid, '2026-05-20', '2026-05-20 13:00:00+00', '2026-05-20 21:00:00+00', 'CLOSED', FALSE);

    INSERT INTO users (first_name, last_name, email, phone, password_hash, enabled, must_change_password, role_id, tenant_id, updated_at)
    VALUES ('Diego', 'Morales', 'diego.morales@acme.qa', '3001110004', pwd_hash, TRUE, FALSE, acme_employee_role, acme_tenant, NOW())
    RETURNING id INTO uid;
    INSERT INTO employees (tenant_id, user_id, first_name, last_name, id_type, id_number, email, phone, monthly_salary)
    VALUES (acme_tenant, uid, 'Diego', 'Morales', 'CE', '1001001004', 'diego.morales@acme.qa', '3001110004', 3500000.00)
    RETURNING id INTO eid;
    INSERT INTO time_records (tenant_id, employee_id, work_date, clock_in, clock_out, status, corrected) VALUES
        (acme_tenant, eid, '2026-05-30', '2026-05-30 13:30:00+00', NULL, 'OPEN', FALSE),
        (acme_tenant, eid, '2026-05-28', '2026-05-28 13:00:00+00', '2026-05-28 17:00:00+00', 'CLOSED', FALSE),
        (acme_tenant, eid, '2026-05-29', '2026-05-29 13:00:00+00', '2026-05-29 21:00:00+00', 'CLOSED', FALSE);

    -- ========== Globex (tenant 4) ==========
    INSERT INTO users (first_name, last_name, email, phone, password_hash, enabled, must_change_password, role_id, tenant_id, updated_at)
    VALUES ('Sofia', 'Ramirez', 'sofia.ramirez@globex.qa', '3002220001', pwd_hash, TRUE, FALSE, globex_employee_role, globex_tenant, NOW())
    RETURNING id INTO uid;
    INSERT INTO employees (tenant_id, user_id, first_name, last_name, id_type, id_number, email, phone, monthly_salary)
    VALUES (globex_tenant, uid, 'Sofia', 'Ramirez', 'CC', '2002002001', 'sofia.ramirez@globex.qa', '3002220001', 2900000.00)
    RETURNING id INTO eid;
    INSERT INTO time_records (tenant_id, employee_id, work_date, clock_in, clock_out, status, corrected) VALUES
        (globex_tenant, eid, '2026-05-03', '2026-05-03 13:00:00+00', '2026-05-03 21:00:00+00', 'CLOSED', FALSE),
        (globex_tenant, eid, '2026-05-10', '2026-05-10 13:00:00+00', '2026-05-10 22:30:00+00', 'CLOSED', FALSE),
        (globex_tenant, eid, '2026-05-17', '2026-05-17 13:00:00+00', '2026-05-17 21:00:00+00', 'CLOSED', TRUE);

    INSERT INTO users (first_name, last_name, email, phone, password_hash, enabled, must_change_password, role_id, tenant_id, updated_at)
    VALUES ('Luis', 'Herrera', 'luis.herrera@globex.qa', '3002220002', pwd_hash, TRUE, FALSE, globex_employee_role, globex_tenant, NOW())
    RETURNING id INTO uid;
    INSERT INTO employees (tenant_id, user_id, first_name, last_name, id_type, id_number, email, phone, monthly_salary)
    VALUES (globex_tenant, uid, 'Luis', 'Herrera', 'CC', '2002002002', 'luis.herrera@globex.qa', '3002220002', 3100000.00)
    RETURNING id INTO eid;
    INSERT INTO time_records (tenant_id, employee_id, work_date, clock_in, clock_out, status, corrected) VALUES
        (globex_tenant, eid, '2026-05-07', '2026-05-07 03:00:00+00', '2026-05-07 11:00:00+00', 'CLOSED', FALSE),
        (globex_tenant, eid, '2026-05-14', '2026-05-14 13:00:00+00', '2026-05-14 21:00:00+00', 'CLOSED', FALSE);

    INSERT INTO users (first_name, last_name, email, phone, password_hash, enabled, must_change_password, role_id, tenant_id, updated_at)
    VALUES ('Paula', 'Castro', 'paula.castro@globex.qa', '3002220003', pwd_hash, TRUE, FALSE, globex_employee_role, globex_tenant, NOW())
    RETURNING id INTO uid;
    INSERT INTO employees (tenant_id, user_id, first_name, last_name, id_type, id_number, email, phone, monthly_salary)
    VALUES (globex_tenant, uid, 'Paula', 'Castro', 'TI', '2002002003', 'paula.castro@globex.qa', '3002220003', 2400000.00)
    RETURNING id INTO eid;
    INSERT INTO time_records (tenant_id, employee_id, work_date, clock_in, clock_out, status, corrected) VALUES
        (globex_tenant, eid, '2026-05-21', '2026-05-21 13:00:00+00', NULL, 'INCOMPLETE', FALSE),
        (globex_tenant, eid, '2026-05-22', '2026-05-22 13:00:00+00', '2026-05-22 21:00:00+00', 'CLOSED', FALSE);

    INSERT INTO users (first_name, last_name, email, phone, password_hash, enabled, must_change_password, role_id, tenant_id, updated_at)
    VALUES ('Jorge', 'Mendoza', 'jorge.mendoza@globex.qa', '3002220004', pwd_hash, TRUE, FALSE, globex_employee_role, globex_tenant, NOW())
    RETURNING id INTO uid;
    INSERT INTO employees (tenant_id, user_id, first_name, last_name, id_type, id_number, email, phone, monthly_salary)
    VALUES (globex_tenant, uid, 'Jorge', 'Mendoza', 'CC', '2002002004', 'jorge.mendoza@globex.qa', '3002220004', 3600000.00)
    RETURNING id INTO eid;
    INSERT INTO time_records (tenant_id, employee_id, work_date, clock_in, clock_out, status, corrected) VALUES
        (globex_tenant, eid, '2026-05-30', '2026-05-30 14:00:00+00', NULL, 'OPEN', FALSE),
        (globex_tenant, eid, '2026-05-24', '2026-05-24 13:00:00+00', '2026-05-24 21:00:00+00', 'CLOSED', FALSE),
        (globex_tenant, eid, '2026-05-27', '2026-05-27 13:00:00+00', '2026-05-27 16:30:00+00', 'CLOSED', FALSE);

    RAISE NOTICE 'QA seed complete: 4 employees + diverse time records per tenant (acme=%, globex=%)', acme_tenant, globex_tenant;
END $$;

COMMIT;
