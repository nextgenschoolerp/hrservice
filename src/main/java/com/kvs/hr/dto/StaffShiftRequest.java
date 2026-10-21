package com.kvs.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffShiftRequest {

    @NotBlank(message = "Shift code is required")
    private String shiftCode;

    @NotBlank(message = "Shift name is required")
    private String shiftName;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private Integer graceMinutes;
    private Integer halfDayThresholdMinutes;
}
