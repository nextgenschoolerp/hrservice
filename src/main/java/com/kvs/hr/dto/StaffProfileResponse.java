package com.kvs.hr.dto;

import com.kvs.hr.enums.EmploymentStatus;
import com.kvs.hr.enums.ProbationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffProfileResponse {
    private UUID staffId;
    private String tenantId;
    private String branchId;
    /** authservice user of this employee, if any */
    private String userId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String department;
    private String designation;
    private BigDecimal basicSalary;
    private LocalDate joiningDate;
    private Boolean isActive;
    private String email;
    private String phone;
    private String panNumber;
    private String aadhaarNumber;
    private String pfUan;
    private String esiNumber;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;
    private ProbationStatus probationStatus;
    private LocalDate probationEndDate;
    private EmploymentStatus employmentStatus;
    private String qualifications;
    private String contractTerms;
}
