-- =====================================================================================================
-- hrservice upgrade: staff photo path (StaffProfile.photoPath). The file itself is kept in the HR photo
-- vault (StaffPhotoStorage, hr.photo-vault.base-path); this column records where it is.
-- Idempotent; run once per database before starting the new hrservice build:
--     powershell -ExecutionPolicy Bypass -File .agents/skills/test-ui/scripts/db/db.ps1 -Script <this file>
-- =====================================================================================================
BEGIN;

ALTER TABLE staff_profiles
    ADD COLUMN IF NOT EXISTS photo_path varchar(255);

COMMIT;
