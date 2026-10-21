package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.EmploymentStatus;
import com.kvs.hr.enums.ProbationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff_profiles", indexes = {
        @Index(name = "idx_staff_tenant_branch", columnList = "tenant_id, branch_id"),
        @Index(name = "idx_staff_emp_code", columnList = "employee_code", unique = true),
        @Index(name = "idx_staff_dept", columnList = "department"),
        @Index(name = "idx_staff_tenant_user", columnList = "tenant_id, user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "staff_id", updatable = false, nullable = false)
    private UUID staffId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Campus code as issued by authservice (e.g. DPS-DELHI-NORTH), same as the JWT branchId claim */
    @Column(name = "branch_id", nullable = false, length = 50)
    private String branchId;

    /** authservice users.user_id of this employee's sign-in account (null for staff without an account) */
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "department", nullable = false, length = 50)
    private String department;

    @Column(name = "designation", nullable = false, length = 50)
    private String designation;

    @Column(name = "basic_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;

    @Column(name = "pf_uan", length = 30)
    private String pfUan;

    @Column(name = "esi_number", length = 30)
    private String esiNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 20)
    private String bankIfsc;

    @Enumerated(EnumType.STRING)
    @Column(name = "probation_status", length = 20)
    @Builder.Default
    private ProbationStatus probationStatus = ProbationStatus.ON_PROBATION;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", length = 20)
    @Builder.Default
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Column(name = "qualifications", length = 500)
    private String qualifications;

    @Column(name = "contract_terms", length = 500)
    private String contractTerms;
}
