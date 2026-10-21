package com.kvs.hr.dto;

import com.kvs.hr.enums.ProbationStatus;
import jakarta.validation.constraints.DecimalMax;
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
public class StaffAppraisalRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotNull(message = "Review date is required")
    private LocalDate reviewDate;

    @NotBlank(message = "Appraisal cycle is required")
    private String appraisalCycle;

    @NotNull(message = "Performance rating is required (1.0 to 5.0)")
    @DecimalMin(value = "1.0", message = "Performance rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Performance rating cannot exceed 5.0")
    private BigDecimal performanceRating;

    private ProbationStatus probationDecision;
    private String reviewerComments;
}
