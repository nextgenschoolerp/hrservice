-- =====================================================================================================
-- hrservice upgrade: bulk staff import jobs (Settings → Data import).
-- Idempotent; run once per database before (or after) starting the new hrservice build:
--     powershell -ExecutionPolicy Bypass -File .agents/skills/test-ui/scripts/db/db.ps1 -Script <this file>
-- =====================================================================================================
BEGIN;

--
-- Name: staff_import_jobs; Type: TABLE; Schema: public; Owner: -
-- Settings → Data import: bulk staff uploads (checked rows as JSON until imported, then the outcome per row)
--

CREATE TABLE IF NOT EXISTS staff_import_jobs (
    job_id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(255),
    updated_at timestamp(6) with time zone,
    updated_by character varying(255),
    version bigint,
    tenant_id character varying(64) NOT NULL,
    branch_id character varying(64) NOT NULL,
    file_name character varying(255),
    status character varying(20) NOT NULL,
    total_rows integer NOT NULL,
    valid_rows integer NOT NULL,
    error_rows integer NOT NULL,
    created_count integer NOT NULL,
    committed_at timestamp(6) with time zone,
    committed_by character varying(255),
    rows_json text,
    CONSTRAINT staff_import_jobs_pkey PRIMARY KEY (job_id)
);

CREATE INDEX IF NOT EXISTS idx_staff_import_tenant ON staff_import_jobs USING btree (tenant_id, created_at);

COMMIT;
