package com.kvs.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffProfileRequest {

    @NotBlank(message = "Employee code is required")
    @Size(max = 50, message = "Employee code cannot exceed 50 characters")
    private String employeeCode;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Department is required")
    @Size(max = 50, message = "Department cannot exceed 50 characters")
    private String department;

    @NotBlank(message = "Designation is required")
    @Size(max = 50, message = "Designation cannot exceed 50 characters")
    private String designation;

    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Basic salary must be greater than zero")
    private BigDecimal basicSalary;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    /** Campus code (e.g. DPS-DELHI-NORTH); required on create, kept when omitted on update */
    @Size(max = 50, message = "Branch cannot exceed 50 characters")
    private String branchId;

    /** authservice user_id of the employee's sign-in account, if any */
    @Size(max = 64, message = "User ID cannot exceed 64 characters")
    private String userId;

    private String email;
    private String phone;
    private String panNumber;
    private String aadhaarNumber;
    private String pfUan;
    private String esiNumber;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;
    private LocalDate probationEndDate;
    private String qualifications;
    private String contractTerms;
}
