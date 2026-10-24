package com.kvs.hr.dto;

import com.kvs.hr.enums.ProbationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAppraisalResponse {
    private UUID appraisalId;
    private String tenantId;
    private UUID staffId;
    private LocalDate reviewDate;
    private String appraisalCycle;
    private BigDecimal performanceRating;
    private ProbationStatus probationDecision;
    private String reviewerComments;
    private String reviewedBy;
}
