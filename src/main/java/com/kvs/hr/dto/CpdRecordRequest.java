package com.kvs.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpdRecordRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotBlank(message = "Course title is required")
    private String courseTitle;

    @NotBlank(message = "Conducting agency is required")
    private String conductingAgency;

    @NotNull(message = "Completion date is required")
    private LocalDate completionDate;

    @NotNull(message = "Training hours is required")
    @DecimalMin(value = "0.1", message = "Training hours must be positive")
    private BigDecimal trainingHours;

    @NotNull(message = "Credits earned is required")
    @DecimalMin(value = "0.0", message = "Credits earned cannot be negative")
    private BigDecimal creditsEarned;

    private String certificateReference;
}
