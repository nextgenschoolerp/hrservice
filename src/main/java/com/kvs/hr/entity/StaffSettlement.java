package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff_settlements", indexes = {
        @Index(name = "idx_settlement_staff", columnList = "staff_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSettlement extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "settlement_id", updatable = false, nullable = false)
    private UUID settlementId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "resignation_id")
    private UUID resignationId;

    @Column(name = "last_working_day", nullable = false)
    private LocalDate lastWorkingDay;

    @Column(name = "completed_years_service", nullable = false)
    private Integer completedYearsService;

    @Column(name = "gratuity_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal gratuityAmount = BigDecimal.ZERO;

    @Column(name = "leave_encashment_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal leaveEncashmentAmount = BigDecimal.ZERO;

    @Column(name = "notice_pay_adjustment", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal noticePayAdjustment = BigDecimal.ZERO;

    @Column(name = "other_additions", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal otherAdditions = BigDecimal.ZERO;

    @Column(name = "other_deductions", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "net_settlement_amount", precision = 10, scale = 2)
    private BigDecimal netSettlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.DRAFT;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @PrePersist
    @PreUpdate
    public void computeNet() {
        BigDecimal g = gratuityAmount != null ? gratuityAmount : BigDecimal.ZERO;
        BigDecimal le = leaveEncashmentAmount != null ? leaveEncashmentAmount : BigDecimal.ZERO;
        BigDecimal np = noticePayAdjustment != null ? noticePayAdjustment : BigDecimal.ZERO;
        BigDecimal oa = otherAdditions != null ? otherAdditions : BigDecimal.ZERO;
        BigDecimal od = otherDeductions != null ? otherDeductions : BigDecimal.ZERO;
        this.netSettlementAmount = g.add(le).add(np).add(oa).subtract(od);
    }
}
