package com.kvs.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffArchiveRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotNull(message = "Retirement date is required")
    private LocalDate retirementDate;

    private Integer retentionPeriodYears;
    private String archiveReason;
    private String dossierReference;
}
