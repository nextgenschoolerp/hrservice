package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.ResignationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff_resignations", indexes = {
        @Index(name = "idx_resignation_staff", columnList = "staff_id"),
        @Index(name = "idx_resignation_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffResignation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "resignation_id", updatable = false, nullable = false)
    private UUID resignationId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "submission_date", nullable = false)
    private LocalDate submissionDate;

    @Column(name = "notice_period_days", nullable = false)
    private Integer noticePeriodDays;

    @Column(name = "requested_last_working_day", nullable = false)
    private LocalDate requestedLastWorkingDay;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ResignationStatus status = ResignationStatus.SUBMITTED;

    @Column(name = "library_cleared")
    @Builder.Default
    private Boolean libraryCleared = false;

    @Column(name = "accounts_cleared")
    @Builder.Default
    private Boolean accountsCleared = false;

    @Column(name = "it_cleared")
    @Builder.Default
    private Boolean itCleared = false;

    @Column(name = "lab_cleared")
    @Builder.Default
    private Boolean labCleared = false;

    @Column(name = "noc_reference", length = 100)
    private String nocReference;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;
}
