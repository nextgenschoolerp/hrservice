package com.kvs.hr.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffArchiveResponse {
    private UUID archiveId;
    private UUID tenantId;
    private UUID staffId;
    private String employeeCode;
    private String fullName;
    private String department;
    private String designation;
    private LocalDate retirementDate;
    private Integer retentionPeriodYears;
    private String archiveReason;
    private String dossierReference;
}
