package com.kvs.hr.dto;

import com.kvs.hr.enums.LeaveType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveQuotaRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotNull(message = "Quota year is required")
    private Integer quotaYear;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "Total entitled days is required")
    @DecimalMin(value = "0.0", message = "Total entitled days must be non-negative")
    private BigDecimal totalEntitled;
}
