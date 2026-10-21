package com.kvs.hr.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {
    private LocalDate date;
    private Long totalStaff;
    private Long presentCount;
    private Long absentCount;
    private Long onLeaveCount;
    private BigDecimal attendancePercentage;
    private Map<String, Long> departmentPresentCounts;
}
