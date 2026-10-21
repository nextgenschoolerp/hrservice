package com.kvs.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSettlementRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    private UUID resignationId;

    @NotNull(message = "Last working day is required")
    private LocalDate lastWorkingDay;

    private BigDecimal noticePayAdjustment;
    private BigDecimal otherAdditions;
    private BigDecimal otherDeductions;
    private String remarks;
}
