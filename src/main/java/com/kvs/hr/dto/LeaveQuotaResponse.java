package com.kvs.hr.dto;

import com.kvs.hr.enums.LeaveType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveQuotaResponse {
    private UUID quotaId;
    private String tenantId;
    private UUID staffId;
    private Integer quotaYear;
    private LeaveType leaveType;
    private BigDecimal totalEntitled;
    private BigDecimal usedDays;
    private BigDecimal balanceDays;
}
