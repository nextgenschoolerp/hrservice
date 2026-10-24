package com.kvs.hr.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollForecastResponse {
    private UUID forecastId;
    private String tenantId;
    private String fiscalYear;
    private String department;
    private BigDecimal currentAnnualCost;
    private BigDecimal projectedIncrementPct;
    private Integer simulatedNewHiresCount;
    private BigDecimal projectedAnnualCost;
}
