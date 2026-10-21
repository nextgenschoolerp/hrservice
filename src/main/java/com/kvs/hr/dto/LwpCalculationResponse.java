package com.kvs.hr.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LwpCalculationResponse {
    private UUID staffId;
    private Integer payMonth;
    private Integer payYear;
    private Long unexcusedAbsenceDays;
    private BigDecimal dailyRate;
    private BigDecimal lwpDeduction;
}
