package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.LeaveStatus;
import com.kvs.hr.enums.LeaveType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_applications", indexes = {
        @Index(name = "idx_leave_staff_dates", columnList = "staff_id, start_date, end_date"),
        @Index(name = "idx_leave_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveApplication extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_id", updatable = false, nullable = false)
    private UUID applicationId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal totalDays;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "proxy_notes", length = 500)
    private String proxyNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "review_comments", length = 500)
    private String reviewComments;
}
