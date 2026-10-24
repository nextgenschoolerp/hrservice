package com.kvs.hr.dto;

import com.kvs.hr.enums.ResignationStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResignationResponse {
    private UUID resignationId;
    private String tenantId;
    private UUID staffId;
    private LocalDate submissionDate;
    private Integer noticePeriodDays;
    private LocalDate requestedLastWorkingDay;
    private String reason;
    private ResignationStatus status;
    private Boolean libraryCleared;
    private Boolean accountsCleared;
    private Boolean itCleared;
    private Boolean labCleared;
    private String nocReference;
    private String approvedBy;
}
