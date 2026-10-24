package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.DisbursementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "staff_payrolls", indexes = {
        @Index(name = "idx_payrolls_staff_period", columnList = "staff_id, pay_year, pay_month"),
        @Index(name = "idx_payrolls_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffPayroll extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payroll_id", updatable = false, nullable = false)
    private UUID payrollId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "pay_month", nullable = false)
    private Integer payMonth;

    @Column(name = "pay_year", nullable = false)
    private Integer payYear;

    @Column(name = "gross_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "pf_deduction", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pfDeduction = BigDecimal.ZERO;

    @Column(name = "esi_deduction", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal esiDeduction = BigDecimal.ZERO;

    @Column(name = "tds_deduction", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tdsDeduction = BigDecimal.ZERO;

    @Column(name = "lwp_deduction", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lwpDeduction = BigDecimal.ZERO;

    @Column(name = "net_salary", precision = 10, scale = 2)
    private BigDecimal netSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "disbursement_status", length = 20)
    @Builder.Default
    private DisbursementStatus disbursementStatus = DisbursementStatus.PROCESSED;

    @PrePersist
    @PreUpdate
    public void calculateNet() {
        BigDecimal gross = grossSalary != null ? grossSalary : BigDecimal.ZERO;
        BigDecimal pf = pfDeduction != null ? pfDeduction : BigDecimal.ZERO;
        BigDecimal esi = esiDeduction != null ? esiDeduction : BigDecimal.ZERO;
        BigDecimal tds = tdsDeduction != null ? tdsDeduction : BigDecimal.ZERO;
        BigDecimal lwp = lwpDeduction != null ? lwpDeduction : BigDecimal.ZERO;
        this.netSalary = gross.subtract(pf).subtract(esi).subtract(tds).subtract(lwp);
    }
}
