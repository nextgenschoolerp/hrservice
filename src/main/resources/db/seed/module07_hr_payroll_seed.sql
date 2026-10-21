-- =====================================================================================================
-- Module 07 (hrservice) seed: Delhi Public School (tenant a0eEBc99-…), 2026-2027
-- =====================================================================================================
-- Run after the Module 01, 03, 04, 05 and 06 seeds (it profiles their staff accounts), after hrservice has started
-- once, and after db/upgrade/module07_staff_branch_code_user_link.sql:
--     powershell -ExecutionPolicy Bypass -File .agents/skills/test-ui/scripts/db/db.ps1 -Script <this file>
--
-- Maps onto the existing data:
--   * one HR profile per seeded DPS staff account (Modules 01-07, 41 people), linked by staff_profiles.user_id =
--     users.user_id; names, e-mail and phone are copied from the account, branch is the account's campus code
--     (DPS-DELHI-NORTH; principal_sunita DPS-DELHI-SOUTH). staff_id = 5eed0007-0001-4000-8000-0000000000NN where NN is
--     the account's number (5eed0001-0001-4000-8000-0000000000NN), employee code DPS-EMP-10NN. Departments follow the
--     Module 06 timetable (teacher_kavya ENG, teacher_imran SST, teacher_deepa SCI, teacher_arjun HIN,
--     teacher_sana MATH/SCI, subjectteacher_neha MATH/SCI, classteacher_priya ENG).
--   * not profiled: trustee_arvind (board member, not an employee), parent_rohit, and the non-seed accounts
--     admin_rajesh / principal_anita / teacher_vikram.
--   * plus one retired employee without an account (Om Prakash Verma, staff …000000000901) for the archive.
--   * adds roles HR_MANAGER, PAYROLL_ACCOUNTANT, AUDITOR and accounts hr_priya, accountant_vikram, auditor_mehta,
--     compliance_singh (users 5eed0001-0001-4000-8000-000000000045 .. 48, Password@123).
--
-- "Today" is the day the seed runs (like the Module 03 attendance seed), so the Staff screens always show who is on
-- campus and on leave today; re-run the seed to move it forward.
--
-- Story coverage:
--   US-07000 profiles; US-07001 biometric IN/OUT punches (today and the previous school day, DEVICE-GATE-01);
--   US-07002 morning shift + assignments; US-07003/04 leave applications (approved, pending, rejected);
--   US-07005/06 September payroll (PF, TDS, one LWP deduction) and October drafts; US-07007 statutory data;
--   US-07008 payslip distributions; US-07009 CPD records; US-07010 appraisals (probation confirmed, one due);
--   US-07011 settlement of teacher_neeraj; US-07012 September bank payout batch; US-07014 tax declarations;
--   US-07015 punch anomalies (late arrival, missing out-punch, one regularised); US-07016 resignation with NOC;
--   US-07017 leave quotas (casual 12, sick 10, earned 15, maternity 180 for teacher_kavya);
--   US-07018 budget forecasts; US-07019 archived retiree.
--
-- Re-runnable: every row is marked created_by = 'seed:module07' and every seed UUID starts with 5eed0007-.
-- =====================================================================================================

BEGIN;

-- ---------------------------------------------------------------------------------------------------
-- 0. Clean previous seed rows
-- ---------------------------------------------------------------------------------------------------
DELETE FROM staff_archives             WHERE created_by = 'seed:module07' OR archive_id::text LIKE '5eed0007-%';
DELETE FROM payroll_budget_forecasts   WHERE created_by = 'seed:module07' OR forecast_id::text LIKE '5eed0007-%';
DELETE FROM staff_settlements          WHERE created_by = 'seed:module07' OR settlement_id::text LIKE '5eed0007-%';
DELETE FROM staff_resignations         WHERE created_by = 'seed:module07' OR resignation_id::text LIKE '5eed0007-%';
DELETE FROM biometric_anomalies        WHERE created_by = 'seed:module07' OR anomaly_id::text LIKE '5eed0007-%';
DELETE FROM staff_tax_declarations     WHERE created_by = 'seed:module07' OR declaration_id::text LIKE '5eed0007-%';
DELETE FROM bank_payout_batches        WHERE created_by = 'seed:module07' OR batch_id::text LIKE '5eed0007-%';
DELETE FROM staff_appraisals           WHERE created_by = 'seed:module07' OR appraisal_id::text LIKE '5eed0007-%';
DELETE FROM staff_cpd_records          WHERE created_by = 'seed:module07' OR cpd_id::text LIKE '5eed0007-%';
DELETE FROM payslip_distributions      WHERE created_by = 'seed:module07' OR distribution_id::text LIKE '5eed0007-%';
DELETE FROM staff_payrolls             WHERE created_by = 'seed:module07' OR payroll_id::text LIKE '5eed0007-%';
DELETE FROM leave_applications         WHERE created_by = 'seed:module07' OR application_id::text LIKE '5eed0007-%';
DELETE FROM leave_quotas               WHERE created_by = 'seed:module07' OR quota_id::text LIKE '5eed0007-%';
DELETE FROM staff_shift_assignments    WHERE created_by = 'seed:module07' OR assignment_id::text LIKE '5eed0007-%';
DELETE FROM staff_shifts               WHERE created_by = 'seed:module07' OR shift_id::text LIKE '5eed0007-%';
DELETE FROM staff_biometric_logs       WHERE created_by = 'seed:module07' OR log_id::text LIKE '5eed0007-%';
DELETE FROM staff_profiles             WHERE created_by = 'seed:module07' OR staff_id::text LIKE '5eed0007-%';
DELETE FROM user_roles                 WHERE created_by = 'seed:module07';
DELETE FROM user_credentials           WHERE created_by = 'seed:module07';
DELETE FROM users                      WHERE created_by = 'seed:module07';
DELETE FROM roles                      WHERE created_by = 'seed:module07';

-- ---------------------------------------------------------------------------------------------------
-- 1. HR roles and accounts (Module 07 actors)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO roles (name, description, active, created_at, updated_at, created_by, updated_by, version)
SELECT r.name, r.description, true, TIMESTAMPTZ '2026-03-20 04:30:00+00', TIMESTAMPTZ '2026-03-20 04:30:00+00', 'seed:module07', NULL, 0
FROM (VALUES
    ('HR_MANAGER',         'Manages staff profiles, probation, appraisals, resignations, NOCs, and leaves'),
    ('PAYROLL_ACCOUNTANT', 'Processes gross-to-net payroll, statutory PF/ESI, tax declarations, and bank payout batches'),
    ('AUDITOR',            'Audits payroll disbursements, statutory tax returns, biometric punch anomalies, and archives'),
    ('COMPLIANCE_OFFICER', 'Oversees labor law compliance, statutory maternity/medical quotas, and archive retention')
) AS r(name, description)
WHERE NOT EXISTS (SELECT 1 FROM roles x WHERE x.name = r.name);

CREATE TEMP TABLE seed_hr_accounts (user_id text, username text, first_name text, last_name text, email text, phone text, user_type text, created_at timestamptz) ON COMMIT DROP;
INSERT INTO seed_hr_accounts VALUES
('5eed0001-0001-4000-8000-000000000045', 'hr_priya',          'Priya',  'Singh',    'priya.singh@dps.edu.in',     '+919811100145', 'HR_MANAGER',         '2026-03-20 05:00:00+00'),
('5eed0001-0001-4000-8000-000000000046', 'accountant_vikram', 'Vikram', 'Malhotra', 'vikram.malhotra@dps.edu.in', '+919811100146', 'PAYROLL_ACCOUNTANT', '2026-03-20 05:05:00+00'),
('5eed0001-0001-4000-8000-000000000047', 'auditor_mehta',     'Suresh', 'Mehta',    'suresh.mehta@dps.edu.in',    '+919811100147', 'AUDITOR',            '2026-03-20 05:10:00+00'),
('5eed0001-0001-4000-8000-000000000048', 'compliance_singh',  'Harman', 'Singh',    'harman.singh@dps.edu.in',    '+919811100148', 'COMPLIANCE_OFFICER', '2026-03-20 05:15:00+00');

INSERT INTO users (user_id, tenant_id, branch_id, username, first_name, last_name, email, phone_number, user_type, is_active, is_mfa_enabled,
                   created_at, updated_at, created_by, updated_by, version)
SELECT user_id, 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', username, first_name, last_name, email, phone, user_type, true, false,
       created_at, created_at, 'seed:module07', NULL, 0
FROM seed_hr_accounts;

INSERT INTO user_credentials (user_id, password_hash, password_salt, failed_login_attempts, locked_until, must_change_password,
                              password_changed_at, created_at, updated_at, created_by, updated_by, version)
SELECT user_id, '$2a$10$8liDxOW98AN4N7eJQbqKd.LMq6EZ7rg6Q11jCO3sxeqrp39CXcXTq', -- Password@123 (documented test hash)
       'salt_' || substr(md5(user_id), 1, 8), 0, NULL, false, created_at + interval '10 minutes',
       created_at, created_at, 'seed:module07', NULL, 0
FROM seed_hr_accounts;

INSERT INTO user_roles (user_id, role_code, created_at, updated_at, created_by, updated_by, version)
SELECT user_id, user_type, created_at, created_at, 'seed:module07', NULL, 0 FROM seed_hr_accounts;

-- ---------------------------------------------------------------------------------------------------
-- 2. Staff profiles for the existing DPS staff accounts (US-07000)
--    nn = account number; names, e-mail, phone and campus come from users
-- ---------------------------------------------------------------------------------------------------
CREATE TEMP TABLE seed_staff (nn int, department text, designation text, basic numeric, joined date, probation text, probation_end date,
                              status text, active boolean, qualifications text, contract text) ON COMMIT DROP;
INSERT INTO seed_staff VALUES
( 2, 'IT',                 'IT System Administrator',             52000, '2019-07-01', 'CONFIRMED',    '2020-01-01', 'ACTIVE',   true,  'B.Tech Computer Science',     'Permanent Full-Time'),
( 3, 'Administration',     'Security Auditor',                    50000, '2021-04-01', 'CONFIRMED',    '2021-10-01', 'ACTIVE',   true,  'B.Sc IT, CISA',               'Permanent Full-Time'),
( 4, 'Science',            'Head of Science Department',          78000, '2012-06-15', 'CONFIRMED',    '2012-12-15', 'ACTIVE',   true,  'M.Sc Chemistry, B.Ed',        'Permanent Full-Time'),
( 5, 'Administration',     'Administrative Clerk',                28000, '2016-08-01', 'CONFIRMED',    '2017-02-01', 'ACTIVE',   true,  'B.Com',                       'Permanent Full-Time'),
( 6, 'Administration',     'Compliance Officer',                  60000, '2018-01-10', 'CONFIRMED',    '2018-07-10', 'ACTIVE',   true,  'LLB, Diploma in Labour Law',  'Permanent Full-Time'),
( 7, 'Accounts',           'Finance Officer',                     62000, '2015-04-01', 'CONFIRMED',    '2015-10-01', 'ACTIVE',   true,  'M.Com',                       'Permanent Full-Time'),
( 8, 'Admissions',         'Admission Officer',                   45000, '2019-05-01', 'CONFIRMED',    '2019-11-01', 'ACTIVE',   true,  'MBA',                         'Permanent Full-Time'),
( 9, 'Admissions',         'Front Office Counselor',              32000, '2022-06-01', 'CONFIRMED',    '2022-12-01', 'ACTIVE',   true,  'BA Psychology',               'Permanent Full-Time'),
(10, 'Administration',     'Registrar',                           58000, '2014-03-01', 'CONFIRMED',    '2014-09-01', 'ACTIVE',   true,  'MA History, B.Ed',            'Permanent Full-Time'),
(11, 'Mathematics',        'Entrance Exam Evaluator (PGT Maths)', 46000, '2020-07-01', 'CONFIRMED',    '2021-01-01', 'ACTIVE',   true,  'M.Sc Mathematics, B.Ed',      'Permanent Full-Time'),
(12, 'Accounts',           'Finance Cashier',                     30000, '2018-09-01', 'CONFIRMED',    '2019-03-01', 'ACTIVE',   true,  'B.Com',                       'Permanent Full-Time'),
(13, 'Admissions',         'Marketing Manager',                   55000, '2021-01-15', 'CONFIRMED',    '2021-07-15', 'ACTIVE',   true,  'MBA Marketing',               'Permanent Full-Time'),
(15, 'Social Science',     'TGT Social Science',                  40000, '2017-06-01', 'CONFIRMED',    '2017-12-01', 'RESIGNED', false, 'MA Geography, B.Ed',          'Permanent Full-Time'),
(16, 'English',            'TGT English',                         41000, '2020-06-01', 'CONFIRMED',    '2020-12-01', 'ACTIVE',   true,  'MA English, B.Ed',            'Permanent Full-Time'),
(17, 'Leadership',         'Principal',                          120000, '2010-04-01', 'CONFIRMED',    '2010-10-01', 'ACTIVE',   true,  'M.Sc Physics, M.Ed',          'Permanent Full-Time'),
(23, 'Leadership',         'Vice Principal',                      95000, '2011-07-01', 'CONFIRMED',    '2012-01-01', 'ACTIVE',   true,  'MA English, M.Ed',            'Permanent Full-Time'),
(24, 'Student Wellbeing',  'School Nurse',                        35000, '2019-08-01', 'CONFIRMED',    '2020-02-01', 'ACTIVE',   true,  'GNM Nursing',                 'Permanent Full-Time'),
(25, 'Administration',     'Office Clerk',                        26000, '2020-11-02', 'CONFIRMED',    '2021-05-02', 'ACTIVE',   true,  'B.A.',                        'Permanent Full-Time'),
(26, 'English',            'Class Teacher (TGT English)',         44000, '2016-06-01', 'CONFIRMED',    '2016-12-01', 'ACTIVE',   true,  'MA English, B.Ed',            'Permanent Full-Time'),
(27, 'Physical Education', 'House Master & PE Teacher',           43000, '2015-06-01', 'CONFIRMED',    '2015-12-01', 'ACTIVE',   true,  'B.P.Ed',                      'Permanent Full-Time'),
(28, 'Administration',     'Alumni Coordinator',                  38000, '2021-08-01', 'CONFIRMED',    '2022-02-01', 'ACTIVE',   true,  'MA Mass Communication',       'Permanent Full-Time'),
(29, 'Accounts',           'Chief Accountant',                    85000, '2009-04-01', 'CONFIRMED',    '2009-10-01', 'ACTIVE',   true,  'CA',                          'Permanent Full-Time'),
(30, 'Accounts',           'Finance Controller',                  98000, '2008-04-01', 'CONFIRMED',    '2008-10-01', 'ACTIVE',   true,  'CA, CPA',                     'Permanent Full-Time'),
(31, 'Accounts',           'Accounts Clerk',                      27000, '2022-01-10', 'CONFIRMED',    '2022-07-10', 'ACTIVE',   true,  'B.Com',                       'Permanent Full-Time'),
(32, 'Accounts',           'Finance Auditor',                     60000, '2019-02-01', 'CONFIRMED',    '2019-08-01', 'ACTIVE',   true,  'CA Inter',                    'Permanent Full-Time'),
(33, 'Examinations',       'Exam Controller',                     80000, '2013-04-01', 'CONFIRMED',    '2013-10-01', 'ACTIVE',   true,  'M.Sc Mathematics, M.Ed',      'Permanent Full-Time'),
(34, 'Mathematics',        'PGT Mathematics & Science',           52000, '2018-06-01', 'CONFIRMED',    '2018-12-01', 'ACTIVE',   true,  'M.Sc Mathematics, B.Ed',      'Permanent Full-Time'),
(35, 'Academics',          'Academic Auditor',                    62000, '2016-04-01', 'CONFIRMED',    '2016-10-01', 'ACTIVE',   true,  'M.Ed',                        'Permanent Full-Time'),
(36, 'Academics',          'Academic Coordinator',                75000, '2012-04-01', 'CONFIRMED',    '2012-10-01', 'ACTIVE',   true,  'M.Sc Botany, M.Ed',           'Permanent Full-Time'),
(37, 'Mathematics',        'Timetable In-charge (TGT Maths)',     50000, '2017-06-01', 'CONFIRMED',    '2017-12-01', 'ACTIVE',   true,  'M.Sc Mathematics, B.Ed',      'Permanent Full-Time'),
(38, 'Academics',          'Substitution In-charge',              48000, '2019-06-01', 'CONFIRMED',    '2019-12-01', 'ACTIVE',   true,  'MA Education, B.Ed',          'Permanent Full-Time'),
(39, 'Computer Science',   'E-learning Administrator',            50000, '2021-06-01', 'CONFIRMED',    '2021-12-01', 'ACTIVE',   true,  'MCA',                         'Permanent Full-Time'),
(40, 'English',            'TGT English',                         42000, '2026-05-01', 'ON_PROBATION', '2026-11-01', 'ACTIVE',   true,  'MA English, B.Ed',            'Probationary Contract'),
(41, 'Social Science',     'TGT Social Science',                  43000, '2019-06-01', 'CONFIRMED',    '2019-12-01', 'ACTIVE',   true,  'MA Political Science, B.Ed',  'Permanent Full-Time'),
(42, 'Science',            'TGT Science',                         44000, '2018-06-01', 'CONFIRMED',    '2018-12-01', 'ACTIVE',   true,  'M.Sc Physics, B.Ed',          'Permanent Full-Time'),
(43, 'Hindi',              'TGT Hindi (part-time)',               30000, '2023-07-01', 'CONFIRMED',    '2024-01-01', 'ACTIVE',   true,  'MA Hindi, B.Ed',              'Part-Time 0.7 FTE'),
(44, 'Mathematics',        'TGT Mathematics & Science',           41000, '2025-07-01', 'CONFIRMED',    '2026-01-01', 'ACTIVE',   true,  'M.Sc Mathematics, B.Ed',      'Permanent Full-Time'),
(45, 'Administration',     'HR Manager',                          65000, '2019-04-10', 'CONFIRMED',    '2019-10-10', 'ACTIVE',   true,  'MBA HR',                      'Permanent Full-Time'),
(46, 'Accounts',           'Senior Payroll Accountant',           58000, '2020-08-01', 'CONFIRMED',    '2021-02-01', 'ACTIVE',   true,  'M.Com, CA-Inter',             'Permanent Full-Time'),
(47, 'Accounts',           'Internal Auditor',                    60000, '2018-11-01', 'CONFIRMED',    '2019-05-01', 'ACTIVE',   true,  'CA',                          'Permanent Full-Time'),
(48, 'Administration',     'Labour Law Compliance Officer',       57000, '2021-03-01', 'CONFIRMED',    '2021-09-01', 'ACTIVE',   true,  'LLB',                         'Permanent Full-Time');

-- staff_id / user_id of account nn
CREATE TEMP TABLE seed_staff_ids ON COMMIT DROP AS
SELECT s.nn, ('5eed0007-0001-4000-8000-' || lpad(s.nn::text, 12, '0'))::uuid AS staff_id,
       ('5eed0001-0001-4000-8000-' || lpad(s.nn::text, 12, '0')) AS user_id
FROM seed_staff s;

INSERT INTO staff_profiles (staff_id, tenant_id, branch_id, user_id, employee_code, first_name, last_name, department, designation, basic_salary,
                            joining_date, is_active, email, phone, pan_number, aadhaar_number, pf_uan, esi_number, bank_name, bank_account_number,
                            bank_ifsc, probation_status, probation_end_date, employment_status, qualifications, contract_terms,
                            created_at, updated_at, created_by, version)
SELECT i.staff_id, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, u.branch_id, u.user_id, 'DPS-EMP-' || (1000 + s.nn), u.first_name, u.last_name,
       s.department, s.designation, s.basic, s.joined, s.active, u.email, u.phone_number,
       'DPSPA' || lpad(s.nn::text, 4, '0') || 'K', '5000000' || lpad(s.nn::text, 5, '0'), '1009045678' || lpad(s.nn::text, 2, '0'), NULL,
       (ARRAY['State Bank of India', 'HDFC Bank', 'ICICI Bank'])[s.nn % 3 + 1], '3012' || lpad(s.nn::text, 8, '0'),
       (ARRAY['SBIN0001234', 'HDFC0001234', 'ICIC0001234'])[s.nn % 3 + 1],
       s.probation, s.probation_end, s.status, s.qualifications, s.contract,
       TIMESTAMPTZ '2026-03-20 06:00:00+00', TIMESTAMPTZ '2026-03-20 06:00:00+00', 'seed:module07', 0
FROM seed_staff s
JOIN seed_staff_ids i ON i.nn = s.nn
JOIN users u ON u.user_id = i.user_id;

-- Retired employee without a sign-in account (archive, US-07019)
INSERT INTO staff_profiles (staff_id, tenant_id, branch_id, user_id, employee_code, first_name, last_name, department, designation, basic_salary,
                            joining_date, is_active, email, phone, probation_status, probation_end_date, employment_status, qualifications,
                            contract_terms, created_at, updated_at, created_by, version)
VALUES ('5eed0007-0001-4000-8000-000000000901', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', NULL, 'DPS-EMP-0901',
        'Om Prakash', 'Verma', 'Hindi', 'PGT Hindi', 72000, DATE '1992-07-01', false, 'omprakash.verma@dps.edu.in', '+919811100901',
        'CONFIRMED', DATE '1993-01-01', 'RETIRED', 'MA Hindi, B.Ed', 'Permanent Full-Time',
        TIMESTAMPTZ '2026-03-20 06:00:00+00', TIMESTAMPTZ '2026-04-01 06:00:00+00', 'seed:module07', 0);

-- ---------------------------------------------------------------------------------------------------
-- 3. Shift roster (US-07002): every active DPS North employee works the morning shift
-- ---------------------------------------------------------------------------------------------------
INSERT INTO staff_shifts (shift_id, tenant_id, shift_code, shift_name, start_time, end_time, grace_minutes, half_day_threshold_minutes, is_active,
                          created_at, updated_at, created_by, version)
VALUES ('5eed0007-0002-4000-8000-000000000001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'SHIFT-DPS-MORNING', 'DPS North Regular Morning Shift', TIME '07:30:00', TIME '14:00:00', 15, 210, true,
        TIMESTAMPTZ '2026-03-20 06:20:00+00', TIMESTAMPTZ '2026-03-20 06:20:00+00', 'seed:module07', 0);

INSERT INTO staff_shift_assignments (assignment_id, tenant_id, staff_id, shift_id, effective_from, effective_to, created_at, updated_at, created_by, version)
SELECT ('5eed0007-0003-4000-8000-' || lpad(p.nn::text, 12, '0'))::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, p.staff_id,
       '5eed0007-0002-4000-8000-000000000001'::uuid, DATE '2026-04-01', NULL::date,
       TIMESTAMPTZ '2026-03-20 06:25:00+00', TIMESTAMPTZ '2026-03-20 06:25:00+00', 'seed:module07', 0
FROM seed_staff_ids p JOIN staff_profiles sp ON sp.staff_id = p.staff_id
WHERE sp.is_active AND sp.branch_id = 'DPS-DELHI-NORTH';

-- ---------------------------------------------------------------------------------------------------
-- 4. Leave applications (US-07003, US-07004), relative to today
--    on leave today: teacher_deepa (sick, 42) and alumni_rekha (earned, 28); pending: teacher_kavya (40),
--    teacher_imran (41), housemaster_vivek (27); past: subjectteacher_neha approved, teacher_farhan rejected
-- ---------------------------------------------------------------------------------------------------
INSERT INTO leave_applications (application_id, tenant_id, staff_id, leave_type, start_date, end_date, total_days, reason, proxy_notes, status,
                                reviewed_by, review_comments, created_at, updated_at, created_by, version)
SELECT ('5eed0007-0006-4000-8000-' || lpad(l.k::text, 12, '0'))::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid,
       ('5eed0007-0001-4000-8000-' || lpad(l.nn::text, 12, '0'))::uuid, l.leave_type, l.d_from, l.d_to, (l.d_to - l.d_from + 1),
       l.reason, l.proxy, l.status, l.reviewer, l.comments, l.created, l.created + interval '1 hour', 'seed:module07', 0
FROM (VALUES
    (1, 42, 'SICK',   current_date,      current_date + 1,  'Viral fever, medical certificate attached',       'Science 6-A/6-C covered by teacher_sana',   'APPROVED', 'priya.singh@dps.edu.in', 'Approved; proxy arranged by the substitution in-charge', now() - interval '1 day'),
    (2, 28, 'EARNED', current_date - 1,  current_date + 2,  'Family function out of station',                  NULL,                                         'APPROVED', 'priya.singh@dps.edu.in', 'Approved',                                              now() - interval '7 days'),
    (3, 40, 'CASUAL', current_date + 7,  current_date + 8,  'Sister''s wedding',                               'English 6-A covered by classteacher_priya',  'PENDING',  NULL,                     NULL,                                                    now() - interval '2 hours'),
    (4, 41, 'CASUAL', current_date + 10, current_date + 10, 'Personal work at the bank',                       'Social Science 6-A/6-C to teacher_arjun',   'PENDING',  NULL,                     NULL,                                                    now() - interval '1 day'),
    (5, 27, 'EARNED', current_date + 14, current_date + 16, 'Accompanying the house team to an inter-school meet', NULL,                                     'PENDING',  NULL,                     NULL,                                                    now() - interval '3 days'),
    (6, 34, 'CASUAL', DATE '2026-09-10', DATE '2026-09-11', 'Attended CBSE zonal mathematics seminar',         'Classes covered by teacher_sana',            'APPROVED', 'priya.singh@dps.edu.in', 'Approved as official duty, proxy confirmed',            TIMESTAMPTZ '2026-09-08 09:00:00+00'),
    (7, 16, 'CASUAL', DATE '2026-09-22', DATE '2026-09-23', 'Personal travel',                                 NULL,                                         'REJECTED', 'priya.singh@dps.edu.in', 'Falls in the Half-Yearly invigilation week',            TIMESTAMPTZ '2026-09-15 10:00:00+00')
) AS l(k, nn, leave_type, d_from, d_to, reason, proxy, status, reviewer, comments, created);

-- ---------------------------------------------------------------------------------------------------
-- 5. Leave quotas for the current year (US-07017); used days = approved leave of that type this year
-- ---------------------------------------------------------------------------------------------------
INSERT INTO leave_quotas (quota_id, tenant_id, staff_id, quota_year, leave_type, total_entitled, used_days, balance_days,
                          created_at, updated_at, created_by, version)
SELECT ('5eed0007-0005-4000-8000-' || lpad((p.nn * 10 + q.k)::text, 12, '0'))::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, p.staff_id,
       extract(year FROM current_date)::int, q.leave_type, q.entitled, coalesce(u.used, 0), q.entitled - coalesce(u.used, 0),
       TIMESTAMPTZ '2026-03-20 06:30:00+00', TIMESTAMPTZ '2026-03-20 06:30:00+00', 'seed:module07', 0
FROM seed_staff_ids p
JOIN staff_profiles sp ON sp.staff_id = p.staff_id AND sp.is_active
CROSS JOIN (VALUES (1, 'CASUAL', 12.0), (2, 'SICK', 10.0), (3, 'EARNED', 15.0)) AS q(k, leave_type, entitled)
LEFT JOIN LATERAL (
    SELECT sum(la.total_days) AS used FROM leave_applications la
    WHERE la.staff_id = p.staff_id AND la.leave_type = q.leave_type AND la.status = 'APPROVED'
      AND extract(year FROM la.start_date) = extract(year FROM current_date)
) u ON true;

INSERT INTO leave_quotas (quota_id, tenant_id, staff_id, quota_year, leave_type, total_entitled, used_days, balance_days,
                          created_at, updated_at, created_by, version)
VALUES ('5eed0007-0005-4000-8000-000000000404', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0007-0001-4000-8000-000000000040',
        extract(year FROM current_date)::int, 'MATERNITY', 180.0, 0.0, 180.0,
        TIMESTAMPTZ '2026-03-20 06:30:00+00', TIMESTAMPTZ '2026-03-20 06:30:00+00', 'seed:module07', 0);

-- ---------------------------------------------------------------------------------------------------
-- 6. Biometric punches (US-07001): previous school day IN + OUT, today IN, for every active North employee
--    not on leave; teacher_farhan (16) arrives late today, officeclerk_anil (25) has no OUT on the previous day
-- ---------------------------------------------------------------------------------------------------
CREATE TEMP TABLE seed_days ON COMMIT DROP AS
SELECT current_date AS today,
       (current_date - CASE extract(isodow FROM current_date)::int WHEN 1 THEN 3 WHEN 7 THEN 2 ELSE 1 END) AS prev_day;

CREATE TEMP TABLE seed_present ON COMMIT DROP AS
SELECT p.nn, p.staff_id FROM seed_staff_ids p
JOIN staff_profiles sp ON sp.staff_id = p.staff_id AND sp.is_active AND sp.branch_id = 'DPS-DELHI-NORTH'
WHERE p.nn NOT IN (42, 28);

INSERT INTO staff_biometric_logs (log_id, tenant_id, staff_id, terminal_device_id, scan_timestamp, scan_type, created_at, updated_at, created_by, version)
SELECT ('5eed0007-0004-4000-8000-' || lpad((p.nn * 10 + x.k)::text, 12, '0'))::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, p.staff_id,
       'DEVICE-GATE-01', x.ts, x.scan_type, x.ts, x.ts, 'seed:module07', 0
FROM seed_present p CROSS JOIN seed_days d
CROSS JOIN LATERAL (VALUES
    (1, 'IN',  ((d.prev_day + TIME '07:15') + (p.nn % 12) * interval '1 minute') AT TIME ZONE 'Asia/Kolkata'),
    (2, 'OUT', ((d.prev_day + TIME '14:05') + (p.nn % 20) * interval '1 minute') AT TIME ZONE 'Asia/Kolkata'),
    (3, 'IN',  ((d.today + CASE WHEN p.nn = 16 THEN TIME '07:58' ELSE TIME '07:15' END) + (p.nn % 12) * interval '1 minute') AT TIME ZONE 'Asia/Kolkata')
) AS x(k, scan_type, ts)
WHERE NOT (p.nn = 25 AND x.k = 2);

-- ---------------------------------------------------------------------------------------------------
-- 7. Punch anomalies (US-07015)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO biometric_anomalies (anomaly_id, tenant_id, staff_id, anomaly_date, anomaly_type, corrected_in_time, corrected_out_time,
                                 regularisation_reason, reviewed_by, status, created_at, updated_at, created_by, version)
SELECT a.id::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, a.staff::uuid, a.day, a.kind, a.cin, a.cout, a.reason, a.reviewer, a.status,
       now(), now(), 'seed:module07', 0
FROM seed_days d CROSS JOIN LATERAL (VALUES
    ('5eed0007-0013-4000-8000-000000000001', '5eed0007-0001-4000-8000-000000000016', d.today,          'LATE_ARRIVAL',      NULL::time,  NULL::time,  NULL::text,                               NULL::text,               'PENDING'),
    ('5eed0007-0013-4000-8000-000000000002', '5eed0007-0001-4000-8000-000000000025', d.prev_day,       'MISSING_OUT_PUNCH', NULL::time,  NULL::time,  NULL::text,                               NULL::text,               'PENDING'),
    ('5eed0007-0013-4000-8000-000000000003', '5eed0007-0001-4000-8000-000000000005', DATE '2026-10-05', 'MISSED_SCAN',      TIME '07:25', TIME '14:10', 'Gate terminal offline during power cut', 'priya.singh@dps.edu.in', 'APPROVED')
) AS a(id, staff, day, kind, cin, cout, reason, reviewer, status);

-- ---------------------------------------------------------------------------------------------------
-- 8. Tax declarations 2026-2027 (US-07014): monthly TDS used by the payroll below
-- ---------------------------------------------------------------------------------------------------
INSERT INTO staff_tax_declarations (declaration_id, tenant_id, staff_id, financial_year, tax_regime, section_80c_amount, section_80d_amount,
                                    hra_amount, other_deductions, projected_annual_tax, monthly_tds, is_verified, created_at, updated_at, created_by, version)
SELECT ('5eed0007-0012-4000-8000-' || lpad(t.nn::text, 12, '0'))::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid,
       ('5eed0007-0001-4000-8000-' || lpad(t.nn::text, 12, '0'))::uuid, '2026-2027', t.regime, t.c80, t.d80, t.hra, 0, t.annual,
       round(t.annual / 12, 2), t.verified,
       TIMESTAMPTZ '2026-04-10 10:00:00+00', TIMESTAMPTZ '2026-04-10 10:00:00+00', 'seed:module07', 0
FROM (VALUES
    (34, 'OLD', 150000.00, 25000.00, 120000.00, 30000.00, true),
    (36, 'NEW',      0.00,     0.00,      0.00, 60000.00, true),
    (45, 'NEW',      0.00,     0.00,      0.00, 48000.00, true),
    (29, 'OLD', 150000.00, 50000.00, 180000.00, 96000.00, true),
    (40, 'NEW',      0.00,     0.00,      0.00,     0.00, false)
) AS t(nn, regime, c80, d80, hra, annual, verified);

-- ---------------------------------------------------------------------------------------------------
-- 9. Payroll (US-07005, US-07006): September 2026 disbursed, October 2026 draft, for every active employee
--    PF 12% capped at 1,800; ESI nil (all are above the ESI wage ceiling); TDS from the declaration, else 5% of the
--    salary above 50,000; teacher_farhan loses one day's pay (LWP) in September
-- ---------------------------------------------------------------------------------------------------
INSERT INTO staff_payrolls (payroll_id, tenant_id, staff_id, pay_month, pay_year, gross_salary, pf_deduction, esi_deduction, tds_deduction,
                            lwp_deduction, net_salary, disbursement_status, created_at, updated_at, created_by, version)
SELECT ('5eed0007-0007-4000-8000-' || lpad((p.nn * 100 + m.pay_month)::text, 12, '0'))::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, p.staff_id,
       m.pay_month, 2026, c.gross, c.pf, 0, c.tds, c.lwp, c.gross - c.pf - c.tds - c.lwp, m.status,
       m.at, m.at, 'seed:module07', 0
FROM seed_staff_ids p
JOIN staff_profiles sp ON sp.staff_id = p.staff_id AND sp.is_active AND sp.joining_date <= DATE '2026-09-01'
CROSS JOIN (VALUES (9, 'DISBURSED', TIMESTAMPTZ '2026-09-30 18:00:00+00'), (10, 'DRAFT', TIMESTAMPTZ '2026-10-15 12:00:00+00')) AS m(pay_month, status, at)
LEFT JOIN staff_tax_declarations td ON td.staff_id = p.staff_id AND td.financial_year = '2026-2027'
CROSS JOIN LATERAL (SELECT sp.basic_salary AS gross,
                           least(round(sp.basic_salary * 0.12, 2), 1800.00) AS pf,
                           coalesce(td.monthly_tds, CASE WHEN sp.basic_salary > 50000 THEN round((sp.basic_salary - 50000) * 0.05, 2) ELSE 0 END) AS tds,
                           CASE WHEN p.nn = 16 AND m.pay_month = 9 THEN round(sp.basic_salary / 30, 2) ELSE 0 END AS lwp) c;

-- ---------------------------------------------------------------------------------------------------
-- 10. Payslips (US-07008) and bank payout batch (US-07012) for September
-- ---------------------------------------------------------------------------------------------------
INSERT INTO payslip_distributions (distribution_id, tenant_id, staff_id, payroll_id, recipient_email, delivery_status, document_reference,
                                   password_hint, sent_timestamp, created_at, updated_at, created_by, version)
SELECT ('5eed0007-0009-4000-8000-' || lpad(p.nn::text, 12, '0'))::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, pr.staff_id, pr.payroll_id,
       sp.email, 'SENT', 'PAYSLIP-2026-09-' || sp.employee_code, 'PAN (first 5 letters) + year of birth',
       TIMESTAMPTZ '2026-10-01 04:30:00+00', TIMESTAMPTZ '2026-10-01 04:30:00+00', TIMESTAMPTZ '2026-10-01 04:30:00+00', 'seed:module07', 0
FROM staff_payrolls pr
JOIN seed_staff_ids p ON p.staff_id = pr.staff_id
JOIN staff_profiles sp ON sp.staff_id = pr.staff_id
WHERE pr.created_by = 'seed:module07' AND pr.pay_month = 9 AND pr.pay_year = 2026;

INSERT INTO bank_payout_batches (batch_id, tenant_id, batch_reference, pay_month, pay_year, total_amount, staff_count, bank_name, disbursement_date,
                                 export_format, batch_status, created_at, updated_at, created_by, version)
SELECT '5eed0007-0011-4000-8000-000000000001'::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, 'PAYOUT-SBIN-2026-09-001', 9, 2026,
       sum(net_salary), count(*), 'State Bank of India', DATE '2026-09-30', 'NEFT_CSV', 'EXPORTED',
       TIMESTAMPTZ '2026-09-30 18:30:00+00', TIMESTAMPTZ '2026-09-30 18:30:00+00', 'seed:module07', 0
FROM staff_payrolls WHERE created_by = 'seed:module07' AND pay_month = 9 AND pay_year = 2026;

-- ---------------------------------------------------------------------------------------------------
-- 11. CPD training records (US-07009)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO staff_cpd_records (cpd_id, tenant_id, staff_id, course_title, conducting_agency, completion_date, training_hours, credits_earned,
                               certificate_reference, created_at, updated_at, created_by, version)
SELECT ('5eed0007-0008-4000-8000-' || lpad(c.k::text, 12, '0'))::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid,
       ('5eed0007-0001-4000-8000-' || lpad(c.nn::text, 12, '0'))::uuid, c.title, c.agency, c.done, c.hours, c.credits, c.cert,
       (c.done + TIME '17:00') AT TIME ZONE 'Asia/Kolkata', (c.done + TIME '17:00') AT TIME ZONE 'Asia/Kolkata', 'seed:module07', 0
FROM (VALUES
    (1, 34, 'NEP 2020 Pedagogical Innovations & AI in Classroom', 'NCERT & CBSE Board',        DATE '2026-08-15', 25.0, 4.0, 'CERT-NCERT-2026-9988'),
    (2, 40, 'Foundations of Competency-Based Assessment',        'CBSE Centre of Excellence', DATE '2026-07-20', 12.0, 2.0, 'CERT-CBSE-2026-4411'),
    (3, 42, 'Inquiry-Based Science Labs',                        'DPS Society In-service',    DATE '2026-06-12',  8.0, 1.0, 'CERT-DPS-2026-0612'),
    (4, 41, 'Map Skills and Primary Sources in Social Science',  'NCERT',                     DATE '2026-05-30', 10.0, 1.5, 'CERT-NCERT-2026-7720'),
    (5, 36, 'Instructional Leadership for Coordinators',         'CBSE Centre of Excellence', DATE '2026-04-25', 30.0, 5.0, 'CERT-CBSE-2026-1180'),
    (6, 26, 'Child Safeguarding & POCSO Awareness',              'DPS Society In-service',    DATE '2026-10-12',  4.0, 0.5, 'CERT-DPS-2026-1012'),
    (7, 27, 'Child Safeguarding & POCSO Awareness',              'DPS Society In-service',    DATE '2026-10-12',  4.0, 0.5, 'CERT-DPS-2026-1013')
) AS c(k, nn, title, agency, done, hours, credits, cert);

-- ---------------------------------------------------------------------------------------------------
-- 12. Appraisals (US-07010): teacher_sana's probation confirmed, accountsclerk_sanjay's annual review;
--     teacher_kavya's probation ends 01 Nov 2026 (review due, no record yet)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO staff_appraisals (appraisal_id, tenant_id, staff_id, review_date, appraisal_cycle, performance_rating, probation_decision,
                              reviewer_comments, reviewed_by, created_at, updated_at, created_by, version)
VALUES
('5eed0007-0010-4000-8000-000000000001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0007-0001-4000-8000-000000000044', DATE '2026-01-05',
 'Probation End Review', 4.2, 'CONFIRMED', 'Reliable proxy cover for Mathematics and Science; strong classroom management', 'kiran.malhotra@dps.edu.in',
 TIMESTAMPTZ '2026-01-05 10:00:00+00', TIMESTAMPTZ '2026-01-05 10:00:00+00', 'seed:module07', 0),
('5eed0007-0010-4000-8000-000000000002', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0007-0001-4000-8000-000000000031', DATE '2026-04-15',
 'Annual Review 2025-2026', 3.8, NULL, 'Accurate day-book entries; to take on ledger reconciliation', 'priya.singh@dps.edu.in',
 TIMESTAMPTZ '2026-04-15 10:00:00+00', TIMESTAMPTZ '2026-04-15 10:00:00+00', 'seed:module07', 0);

-- ---------------------------------------------------------------------------------------------------
-- 13. Resignation with NOC (US-07016) and settlement (US-07011): teacher_neeraj left on 30 Jun 2026
--     (his account is inactive in Module 01)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO staff_resignations (resignation_id, tenant_id, staff_id, submission_date, notice_period_days, requested_last_working_day, reason, status,
                                library_cleared, accounts_cleared, it_cleared, lab_cleared, noc_reference, approved_by,
                                created_at, updated_at, created_by, version)
VALUES ('5eed0007-0014-4000-8000-000000000001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0007-0001-4000-8000-000000000015',
        DATE '2026-05-31', 30, DATE '2026-06-30', 'Relocating to Pune for family reasons', 'NOC_ISSUED', true, true, true, true,
        'NOC-DPS-5EED0007', 'priya.singh@dps.edu.in',
        TIMESTAMPTZ '2026-05-31 09:00:00+00', TIMESTAMPTZ '2026-06-30 12:00:00+00', 'seed:module07', 0);

INSERT INTO staff_settlements (settlement_id, tenant_id, staff_id, resignation_id, last_working_day, completed_years_service, gratuity_amount,
                               leave_encashment_amount, notice_pay_adjustment, other_additions, other_deductions, net_settlement_amount, status, remarks,
                               created_at, updated_at, created_by, version)
VALUES ('5eed0007-0016-4000-8000-000000000001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0007-0001-4000-8000-000000000015',
        '5eed0007-0014-4000-8000-000000000001', DATE '2026-06-30', 9, 207692.31, 15384.62, 0, 0, 0, 223076.93, 'PAID',
        'Gratuity 15/26 x 40,000 x 9 years; 10 earned leave days encashed',
        TIMESTAMPTZ '2026-07-10 10:00:00+00', TIMESTAMPTZ '2026-07-15 10:00:00+00', 'seed:module07', 0);

-- ---------------------------------------------------------------------------------------------------
-- 14. Archive (US-07019): Om Prakash Verma retired 31 Mar 2026
-- ---------------------------------------------------------------------------------------------------
INSERT INTO staff_archives (archive_id, tenant_id, staff_id, employee_code, full_name, department, designation, retirement_date,
                            retention_period_years, archive_reason, dossier_reference, created_at, updated_at, created_by, version)
VALUES ('5eed0007-0017-4000-8000-000000000001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0007-0001-4000-8000-000000000901',
        'DPS-EMP-0901', 'Om Prakash Verma', 'Hindi', 'PGT Hindi', DATE '2026-03-31', 10, 'Superannuation retirement',
        'ARCHIVE-DOSSIER-DPS-EMP-0901', TIMESTAMPTZ '2026-04-01 06:00:00+00', TIMESTAMPTZ '2026-04-01 06:00:00+00', 'seed:module07', 0);

-- ---------------------------------------------------------------------------------------------------
-- 15. Payroll budget forecasts 2027-2028 (US-07018): current cost = 12 x the September gross of the department
-- ---------------------------------------------------------------------------------------------------
INSERT INTO payroll_budget_forecasts (forecast_id, tenant_id, fiscal_year, department, current_annual_cost, projected_increment_pct,
                                      simulated_new_hires_count, projected_annual_cost, created_at, updated_at, created_by, version)
SELECT f.id::uuid, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, '2027-2028', f.dept, c.cost, f.pct, f.hires,
       round(c.cost * (1 + f.pct / 100) + f.hires * f.hire_cost * 12, 2),
       TIMESTAMPTZ '2026-09-15 14:00:00+00', TIMESTAMPTZ '2026-09-15 14:00:00+00', 'seed:module07', 0
FROM (VALUES
    ('5eed0007-0015-4000-8000-000000000001', 'Mathematics',    8.50, 1, 45000),
    ('5eed0007-0015-4000-8000-000000000002', 'Administration', 6.00, 0, 0)
) AS f(id, dept, pct, hires, hire_cost)
CROSS JOIN LATERAL (
    SELECT coalesce(sum(pr.gross_salary), 0) * 12 AS cost FROM staff_payrolls pr JOIN staff_profiles sp ON sp.staff_id = pr.staff_id
    WHERE pr.created_by = 'seed:module07' AND pr.pay_month = 9 AND pr.pay_year = 2026 AND sp.department = f.dept
) c;

COMMIT;
