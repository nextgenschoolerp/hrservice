package com.kvs.hr.dto;

import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffShiftResponse {
    private UUID shiftId;
    private UUID tenantId;
    private String shiftCode;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer graceMinutes;
    private Integer halfDayThresholdMinutes;
    private Boolean isActive;
}
