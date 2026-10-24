package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.ProbationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff_appraisals", indexes = {
        @Index(name = "idx_appraisal_staff", columnList = "staff_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAppraisal extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "appraisal_id", updatable = false, nullable = false)
    private UUID appraisalId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;

    @Column(name = "appraisal_cycle", nullable = false, length = 50)
    private String appraisalCycle;

    @Column(name = "performance_rating", nullable = false, precision = 3, scale = 1)
    private BigDecimal performanceRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "probation_decision", length = 20)
    private ProbationStatus probationDecision;

    @Column(name = "reviewer_comments", length = 1000)
    private String reviewerComments;

    @Column(name = "reviewed_by", nullable = false, length = 100)
    private String reviewedBy;
}
