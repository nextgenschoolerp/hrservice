package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_budget_forecasts", indexes = {
        @Index(name = "idx_forecast_tenant_year", columnList = "tenant_id, fiscal_year")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollForecast extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "forecast_id", updatable = false, nullable = false)
    private UUID forecastId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "fiscal_year", nullable = false, length = 20)
    private String fiscalYear;

    @Column(name = "department", nullable = false, length = 50)
    private String department;

    @Column(name = "current_annual_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentAnnualCost;

    @Column(name = "projected_increment_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal projectedIncrementPct;

    @Column(name = "simulated_new_hires_count")
    @Builder.Default
    private Integer simulatedNewHiresCount = 0;

    @Column(name = "projected_annual_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal projectedAnnualCost;
}
