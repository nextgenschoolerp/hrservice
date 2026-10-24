package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.LeaveType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "leave_quotas", indexes = {
        @Index(name = "idx_quota_staff_year_type", columnList = "staff_id, quota_year, leave_type", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveQuota extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quota_id", updatable = false, nullable = false)
    private UUID quotaId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "quota_year", nullable = false)
    private Integer quotaYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(name = "total_entitled", nullable = false, precision = 5, scale = 1)
    private BigDecimal totalEntitled;

    @Column(name = "used_days", precision = 5, scale = 1)
    @Builder.Default
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(name = "balance_days", precision = 5, scale = 1)
    private BigDecimal balanceDays;

    @PrePersist
    @PreUpdate
    public void computeBalance() {
        BigDecimal total = totalEntitled != null ? totalEntitled : BigDecimal.ZERO;
        BigDecimal used = usedDays != null ? usedDays : BigDecimal.ZERO;
        this.balanceDays = total.subtract(used);
    }
}
