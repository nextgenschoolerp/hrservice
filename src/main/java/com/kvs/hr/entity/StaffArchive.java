package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff_archives", indexes = {
        @Index(name = "idx_archive_staff", columnList = "staff_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffArchive extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "archive_id", updatable = false, nullable = false)
    private UUID archiveId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "department", nullable = false, length = 50)
    private String department;

    @Column(name = "designation", nullable = false, length = 50)
    private String designation;

    @Column(name = "retirement_date", nullable = false)
    private LocalDate retirementDate;

    @Column(name = "retention_period_years")
    @Builder.Default
    private Integer retentionPeriodYears = 10;

    @Column(name = "archive_reason", length = 250)
    private String archiveReason;

    @Column(name = "dossier_reference", length = 200)
    private String dossierReference;
}
