package com.kvs.hr.dto;

import com.kvs.hr.enums.LeaveStatus;
import com.kvs.hr.enums.LeaveType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationResponse {
    private UUID applicationId;
    private String tenantId;
    private UUID staffId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String reason;
    private String proxyNotes;
    private LeaveStatus status;
    private String reviewedBy;
    private String reviewComments;
    private Instant createdAt;
}
