package com.kvs.hr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResignationRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotNull(message = "Submission date is required")
    private LocalDate submissionDate;

    @NotNull(message = "Notice period in days is required")
    @Min(0)
    private Integer noticePeriodDays;

    @NotNull(message = "Requested last working day is required")
    private LocalDate requestedLastWorkingDay;

    @NotBlank(message = "Resignation reason is required")
    private String reason;
}
