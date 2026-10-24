package com.kvs.hr.dto;

import com.kvs.hr.enums.AnomalyType;
import com.kvs.hr.enums.RegularisationStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricAnomalyResponse {
    private UUID anomalyId;
    private String tenantId;
    private UUID staffId;
    private LocalDate anomalyDate;
    private AnomalyType anomalyType;
    private RegularisationStatus status;
    private String regularisationReason;
    private LocalTime correctedInTime;
    private LocalTime correctedOutTime;
    private String reviewedBy;
}
