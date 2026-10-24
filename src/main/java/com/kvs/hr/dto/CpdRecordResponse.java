package com.kvs.hr.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpdRecordResponse {
    private UUID cpdId;
    private String tenantId;
    private UUID staffId;
    private String courseTitle;
    private String conductingAgency;
    private LocalDate completionDate;
    private BigDecimal trainingHours;
    private BigDecimal creditsEarned;
    private String certificateReference;
}
