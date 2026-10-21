package com.kvs.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollCalculationRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotNull(message = "Pay month is required (1-12)")
    @Min(value = 1, message = "Pay month must be between 1 and 12")
    @Max(value = 12, message = "Pay month must be between 1 and 12")
    private Integer payMonth;

    @NotNull(message = "Pay year is required")
    private Integer payYear;

    private BigDecimal allowances;
    private BigDecimal customDeductions;
}
