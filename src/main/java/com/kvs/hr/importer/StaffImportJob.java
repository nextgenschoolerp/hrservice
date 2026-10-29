package com.kvs.hr.importer;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One bulk staff upload: the checked rows (JSON) between "Check file" and "Import", and afterwards the outcome of each row.
 * Uploaded files themselves are never stored; after the import the row values are dropped and only the summary is kept.
 */
@Entity
@Table(name = "staff_import_jobs", indexes = @Index(name = "idx_staff_import_tenant", columnList = "tenant_id, created_at"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffImportJob extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "branch_id", nullable = false, length = 64)
    private String branchId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    /** VALIDATED (checked, waiting for Import), COMMITTED, or EXPIRED */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "error_rows", nullable = false)
    private int errorRows;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    /** CREATE (new records), UPDATE (existing records matched by UID / employee code) or PHOTOS (a ZIP of photos) */
    @Builder.Default
    @Column(name = "import_mode", nullable = false, length = 10)
    private String mode = "CREATE";

    /** Records changed by an UPDATE import, or given a photo by a PHOTOS import */
    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "committed_at")
    private Instant committedAt;

    @Column(name = "committed_by", length = 255)
    private String committedBy;

    @Column(name = "rows_json", columnDefinition = "TEXT")
    private String rowsJson;
}
