package com.kvs.hr.dto;

import com.kvs.hr.enums.SettlementStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSettlementResponse {
    private UUID settlementId;
    private UUID tenantId;
    private UUID staffId;
    private UUID resignationId;
    private LocalDate lastWorkingDay;
    private Integer completedYearsService;
    private BigDecimal gratuityAmount;
    private BigDecimal leaveEncashmentAmount;
    private BigDecimal noticePayAdjustment;
    private BigDecimal otherAdditions;
    private BigDecimal otherDeductions;
    private BigDecimal netSettlementAmount;
    private SettlementStatus status;
    private String remarks;
}
