-- Bulk import v2: update existing records (import_mode UPDATE) and photo ZIPs (PHOTOS); updated_count = records changed.
ALTER TABLE staff_import_jobs ADD COLUMN IF NOT EXISTS import_mode character varying(10) NOT NULL DEFAULT 'CREATE';
ALTER TABLE staff_import_jobs ADD COLUMN IF NOT EXISTS updated_count integer NOT NULL DEFAULT 0;
