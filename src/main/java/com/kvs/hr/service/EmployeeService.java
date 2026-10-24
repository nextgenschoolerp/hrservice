package com.kvs.hr.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ConflictException;
import com.kvs.hr.config.HrRoles;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.hr.dto.*;
import com.kvs.hr.entity.*;
import com.kvs.hr.enums.EmploymentStatus;
import com.kvs.hr.enums.ProbationStatus;
import com.kvs.hr.enums.ResignationStatus;
import com.kvs.hr.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeService {

    private final StaffProfileRepository staffProfileRepository;
    private final StaffAppraisalRepository staffAppraisalRepository;
    private final StaffResignationRepository staffResignationRepository;
    private final StaffArchiveRepository staffArchiveRepository;
    private final TenantAccessGuard tenantGuard;
    private final HrAuditService auditService;

    // US-07000: Maintain employee master records
    @Transactional
    public StaffProfileResponse createEmployee(StaffProfileRequest request) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        if (staffProfileRepository.existsByTenantIdAndEmployeeCode(tenantId, request.getEmployeeCode())) {
            throw new ConflictException("Employee with code " + request.getEmployeeCode() + " already exists", "DUPLICATE_EMPLOYEE_CODE");
        }

        if (request.getBranchId() == null || request.getBranchId().isBlank()) {
            throw new BadRequestException("Branch is required (campus code, e.g. DPS-DELHI-NORTH)", "BRANCH_REQUIRED");
        }
        StaffProfile staff = StaffProfile.builder()
                .tenantId(tenantId)
                .branchId(request.getBranchId())
                .userId(blankToNull(request.getUserId()))
                .employeeCode(request.getEmployeeCode())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .basicSalary(request.getBasicSalary())
                .joiningDate(request.getJoiningDate())
                .isActive(true)
                .email(request.getEmail())
                .phone(request.getPhone())
                .panNumber(request.getPanNumber())
                .aadhaarNumber(request.getAadhaarNumber())
                .pfUan(request.getPfUan())
                .esiNumber(request.getEsiNumber())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankIfsc(request.getBankIfsc())
                .probationStatus(ProbationStatus.ON_PROBATION)
                .probationEndDate(request.getProbationEndDate() != null ? request.getProbationEndDate() : request.getJoiningDate().plusMonths(6))
                .employmentStatus(EmploymentStatus.ACTIVE)
                .qualifications(request.getQualifications())
                .contractTerms(request.getContractTerms())
                .build();

        StaffProfile saved = staffProfileRepository.save(staff);
        auditService.logSuccess("StaffProfile", "CREATE_EMPLOYEE", "Created staff employeeCode=" + saved.getEmployeeCode());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public StaffProfileResponse getEmployee(UUID staffId) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);
        return mapToResponse(staff);
    }

    @Transactional(readOnly = true)
    public StaffProfileResponse getEmployeeByCode(String employeeCode) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        StaffProfile staff = staffProfileRepository.findByTenantIdAndEmployeeCode(tenantId, employeeCode)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "employeeCode", employeeCode));
        return mapToResponse(staff);
    }

    /** The signed-in user's own profile, matched on the token's userId claim. */
    @Transactional(readOnly = true)
    public StaffProfileResponse getMyProfile() {
        String tenantId = tenantGuard.requireCurrentTenantId();
        String userId = tenantGuard.currentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "userId", "missing in JWT claims"));
        StaffProfile staff = staffProfileRepository.findFirstByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "userId", userId));
        return mapToResponse(staff);
    }

    @Transactional(readOnly = true)
    public List<StaffProfileResponse> listEmployees(String department, String branchId) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        boolean byDept = department != null && !department.isBlank();
        boolean byBranch = branchId != null && !branchId.isBlank();
        List<StaffProfile> list;
        if (byBranch && byDept) {
            list = staffProfileRepository.findByTenantIdAndBranchIdAndDepartment(tenantId, branchId, department);
        } else if (byBranch) {
            list = staffProfileRepository.findByTenantIdAndBranchId(tenantId, branchId);
        } else if (byDept) {
            list = staffProfileRepository.findByTenantIdAndDepartment(tenantId, department);
        } else {
            list = staffProfileRepository.findByTenantId(tenantId);
        }
        return list.stream()
                .sorted(java.util.Comparator.comparing(StaffProfile::getFirstName).thenComparing(StaffProfile::getLastName))
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public StaffProfileResponse updateEmployee(UUID staffId, StaffProfileRequest request) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);

        if (request.getBranchId() != null && !request.getBranchId().isBlank()) {
            staff.setBranchId(request.getBranchId());
        }
        if (request.getUserId() != null) {
            staff.setUserId(blankToNull(request.getUserId()));
        }
        staff.setFirstName(request.getFirstName());
        staff.setLastName(request.getLastName());
        staff.setDepartment(request.getDepartment());
        staff.setDesignation(request.getDesignation());
        staff.setBasicSalary(request.getBasicSalary());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        // Identity and bank fields are kept when the request leaves them out (the staff form does not edit them)
        if (request.getPanNumber() != null) staff.setPanNumber(request.getPanNumber());
        if (request.getAadhaarNumber() != null) staff.setAadhaarNumber(request.getAadhaarNumber());
        if (request.getPfUan() != null) staff.setPfUan(request.getPfUan());
        if (request.getEsiNumber() != null) staff.setEsiNumber(request.getEsiNumber());
        if (request.getBankName() != null) staff.setBankName(request.getBankName());
        if (request.getBankAccountNumber() != null) staff.setBankAccountNumber(request.getBankAccountNumber());
        if (request.getBankIfsc() != null) staff.setBankIfsc(request.getBankIfsc());
        staff.setQualifications(request.getQualifications());
        staff.setContractTerms(request.getContractTerms());

        StaffProfile updated = staffProfileRepository.save(staff);
        auditService.logSuccess("StaffProfile", "UPDATE_EMPLOYEE", "Updated staffId=" + staffId);
        return mapToResponse(updated);
    }

    // US-07010: Track probation completion dates and schedule appraisal reviews
    @Transactional
    public StaffAppraisalResponse recordAppraisal(StaffAppraisalRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        String reviewer = SecurityUtils.getCurrentUserEmail().orElse("principal@kvs.local");
        StaffAppraisal appraisal = StaffAppraisal.builder()
                .tenantId(staff.getTenantId())
                .staffId(staff.getStaffId())
                .reviewDate(request.getReviewDate())
                .appraisalCycle(request.getAppraisalCycle())
                .performanceRating(request.getPerformanceRating())
                .probationDecision(request.getProbationDecision())
                .reviewerComments(request.getReviewerComments())
                .reviewedBy(reviewer)
                .build();

        if (request.getProbationDecision() != null) {
            staff.setProbationStatus(request.getProbationDecision());
            staffProfileRepository.save(staff);
        }

        StaffAppraisal saved = staffAppraisalRepository.save(appraisal);
        auditService.logSuccess("StaffAppraisal", "RECORD_APPRAISAL", "Recorded appraisal for staffId=" + staff.getStaffId());
        return mapToAppraisalResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StaffAppraisalResponse> listAppraisals(UUID staffId) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);

        return staffAppraisalRepository.findByStaffIdOrderByReviewDateDesc(staffId).stream()
                .map(this::mapToAppraisalResponse)
                .collect(Collectors.toList());
    }

    // US-07016: Manage staff resignation notices and generate official clearance NOCs
    @Transactional
    public StaffResignationResponse submitResignation(StaffResignationRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        StaffResignation resignation = StaffResignation.builder()
                .tenantId(staff.getTenantId())
                .staffId(staff.getStaffId())
                .submissionDate(request.getSubmissionDate())
                .noticePeriodDays(request.getNoticePeriodDays())
                .requestedLastWorkingDay(request.getRequestedLastWorkingDay())
                .reason(request.getReason())
                .status(ResignationStatus.SUBMITTED)
                .libraryCleared(false)
                .accountsCleared(false)
                .itCleared(false)
                .labCleared(false)
                .build();

        staff.setEmploymentStatus(EmploymentStatus.NOTICE_PERIOD);
        staffProfileRepository.save(staff);

        StaffResignation saved = staffResignationRepository.save(resignation);
        auditService.logSuccess("StaffResignation", "SUBMIT_RESIGNATION", "Resignation submitted for staffId=" + staff.getStaffId());
        return mapToResignationResponse(saved);
    }

    @Transactional
    public StaffResignationResponse updateClearance(UUID resignationId, DepartmentClearanceRequest request) {
        StaffResignation res = staffResignationRepository.findById(resignationId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffResignation", "resignationId", resignationId));
        tenantGuard.requireTenantRecord(res.getTenantId(), "StaffResignation", "resignationId", resignationId);

        String dept = request.getDepartment().toUpperCase();
        switch (dept) {
            case "LIBRARY" -> res.setLibraryCleared(request.getCleared());
            case "ACCOUNTS" -> res.setAccountsCleared(request.getCleared());
            case "IT" -> res.setItCleared(request.getCleared());
            case "LAB" -> res.setLabCleared(request.getCleared());
            default -> throw new IllegalArgumentException("Unknown clearance department: " + request.getDepartment());
        }

        // Check if all clearances are complete
        if (Boolean.TRUE.equals(res.getLibraryCleared()) &&
            Boolean.TRUE.equals(res.getAccountsCleared()) &&
            Boolean.TRUE.equals(res.getItCleared()) &&
            Boolean.TRUE.equals(res.getLabCleared())) {
            res.setStatus(ResignationStatus.NOC_ISSUED);
            res.setNocReference("NOC-DPS-" + res.getStaffId().toString().substring(0, 8).toUpperCase());
            res.setApprovedBy(SecurityUtils.getCurrentUserEmail().orElse("hr@kvs.local"));
        } else {
            res.setStatus(ResignationStatus.CLEARANCE_PENDING);
        }

        StaffResignation saved = staffResignationRepository.save(res);
        auditService.logSuccess("StaffResignation", "CLEARANCE_UPDATE", "Department " + dept + " cleared for resignationId=" + resignationId);
        return mapToResignationResponse(saved);
    }

    @Transactional(readOnly = true)
    public StaffResignationResponse getResignation(UUID resignationId) {
        StaffResignation res = staffResignationRepository.findById(resignationId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffResignation", "resignationId", resignationId));
        tenantGuard.requireTenantRecord(res.getTenantId(), "StaffResignation", "resignationId", resignationId);
        return mapToResignationResponse(res);
    }

    // US-07019: Archive retired staff employment records
    @Transactional
    public StaffArchiveResponse archiveStaff(StaffArchiveRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        StaffArchive archive = StaffArchive.builder()
                .tenantId(staff.getTenantId())
                .staffId(staff.getStaffId())
                .employeeCode(staff.getEmployeeCode())
                .fullName(staff.getFirstName() + " " + staff.getLastName())
                .department(staff.getDepartment())
                .designation(staff.getDesignation())
                .retirementDate(request.getRetirementDate())
                .retentionPeriodYears(request.getRetentionPeriodYears() != null ? request.getRetentionPeriodYears() : 10)
                .archiveReason(request.getArchiveReason())
                .dossierReference(request.getDossierReference() != null ? request.getDossierReference() : "ARCHIVE-DOSSIER-" + staff.getEmployeeCode())
                .build();

        staff.setIsActive(false);
        staff.setEmploymentStatus(EmploymentStatus.RETIRED);
        staffProfileRepository.save(staff);

        StaffArchive saved = staffArchiveRepository.save(archive);
        auditService.logSuccess("StaffArchive", "ARCHIVE_STAFF", "Archived staffId=" + staff.getStaffId());
        return mapToArchiveResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StaffArchiveResponse> listArchives() {
        String tenantId = tenantGuard.requireCurrentTenantId();
        return staffArchiveRepository.findByTenantId(tenantId).stream()
                .map(this::mapToArchiveResponse)
                .collect(Collectors.toList());
    }

    /**
     * Pay and identity fields (salary, PAN, Aadhaar, PF/ESI, bank) are only returned to HR, payroll, the principal,
     * a super admin, or the employee themself; everyone else gets the directory fields.
     */
    private StaffProfileResponse mapToResponse(StaffProfile s) {
        StaffProfileResponse.StaffProfileResponseBuilder b = StaffProfileResponse.builder()
                .staffId(s.getStaffId())
                .tenantId(s.getTenantId())
                .branchId(s.getBranchId())
                .userId(s.getUserId())
                .employeeCode(s.getEmployeeCode())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .department(s.getDepartment())
                .designation(s.getDesignation())
                .joiningDate(s.getJoiningDate())
                .isActive(s.getIsActive())
                .email(s.getEmail())
                .phone(s.getPhone())
                .probationStatus(s.getProbationStatus())
                .probationEndDate(s.getProbationEndDate())
                .employmentStatus(s.getEmploymentStatus())
                .qualifications(s.getQualifications())
                .contractTerms(s.getContractTerms());
        if (canSeeConfidential(s)) {
            b.basicSalary(s.getBasicSalary())
                    .panNumber(s.getPanNumber())
                    .aadhaarNumber(s.getAadhaarNumber())
                    .pfUan(s.getPfUan())
                    .esiNumber(s.getEsiNumber())
                    .bankName(s.getBankName())
                    .bankAccountNumber(s.getBankAccountNumber())
                    .bankIfsc(s.getBankIfsc());
        }
        return b.build();
    }

    private boolean canSeeConfidential(StaffProfile s) {
        if (tenantGuard.hasAnyRole(HrRoles.CONFIDENTIAL_VIEWERS)) {
            return true;
        }
        return s.getUserId() != null && tenantGuard.currentUserId().map(s.getUserId()::equals).orElse(false);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private StaffAppraisalResponse mapToAppraisalResponse(StaffAppraisal a) {
        return StaffAppraisalResponse.builder()
                .appraisalId(a.getAppraisalId())
                .tenantId(a.getTenantId())
                .staffId(a.getStaffId())
                .reviewDate(a.getReviewDate())
                .appraisalCycle(a.getAppraisalCycle())
                .performanceRating(a.getPerformanceRating())
                .probationDecision(a.getProbationDecision())
                .reviewerComments(a.getReviewerComments())
                .reviewedBy(a.getReviewedBy())
                .build();
    }

    private StaffResignationResponse mapToResignationResponse(StaffResignation r) {
        return StaffResignationResponse.builder()
                .resignationId(r.getResignationId())
                .tenantId(r.getTenantId())
                .staffId(r.getStaffId())
                .submissionDate(r.getSubmissionDate())
                .noticePeriodDays(r.getNoticePeriodDays())
                .requestedLastWorkingDay(r.getRequestedLastWorkingDay())
                .reason(r.getReason())
                .status(r.getStatus())
                .libraryCleared(r.getLibraryCleared())
                .accountsCleared(r.getAccountsCleared())
                .itCleared(r.getItCleared())
                .labCleared(r.getLabCleared())
                .nocReference(r.getNocReference())
                .approvedBy(r.getApprovedBy())
                .build();
    }

    private StaffArchiveResponse mapToArchiveResponse(StaffArchive a) {
        return StaffArchiveResponse.builder()
                .archiveId(a.getArchiveId())
                .tenantId(a.getTenantId())
                .staffId(a.getStaffId())
                .employeeCode(a.getEmployeeCode())
                .fullName(a.getFullName())
                .department(a.getDepartment())
                .designation(a.getDesignation())
                .retirementDate(a.getRetirementDate())
                .retentionPeriodYears(a.getRetentionPeriodYears())
                .archiveReason(a.getArchiveReason())
                .dossierReference(a.getDossierReference())
                .build();
    }
}
