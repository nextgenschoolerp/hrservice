-- =====================================================================================================
-- hrservice upgrade: staff_profiles.branch_id holds the campus code (DPS-DELHI-NORTH) instead of a UUID, and
-- staff_profiles.user_id links a profile to its authservice account. Idempotent; run once per database before
-- starting the new hrservice build:
--     powershell -ExecutionPolicy Bypass -File .agents/skills/test-ui/scripts/db/db.ps1 -Script <this file>
-- =====================================================================================================
BEGIN;

ALTER TABLE staff_profiles ALTER COLUMN branch_id TYPE varchar(50) USING branch_id::text;
ALTER TABLE staff_profiles ADD COLUMN IF NOT EXISTS user_id varchar(64);
CREATE INDEX IF NOT EXISTS idx_staff_tenant_user ON staff_profiles (tenant_id, user_id);

-- The first Module 07 seed used a placeholder UUID for DPS North
UPDATE staff_profiles SET branch_id = 'DPS-DELHI-NORTH' WHERE branch_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380b11';

COMMIT;
