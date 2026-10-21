package com.kvs.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollForecastRequest {

    @NotBlank(message = "Fiscal year is required")
    private String fiscalYear;

    @NotBlank(message = "Department is required")
    private String department;

    @NotNull(message = "Projected increment percentage is required")
    @DecimalMin(value = "0.0", message = "Increment percentage must be non-negative")
    private BigDecimal projectedIncrementPct;

    private Integer simulatedNewHiresCount;
}
