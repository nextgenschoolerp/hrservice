package com.kvs.hr.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ForbiddenException;
import com.kvs.hr.config.HrRoles;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.hr.dto.*;
import com.kvs.hr.entity.*;
import com.kvs.hr.enums.LeaveStatus;
import com.kvs.hr.enums.LeaveType;
import com.kvs.hr.repo.LeaveApplicationRepository;
import com.kvs.hr.repo.LeaveQuotaRepository;
import com.kvs.hr.repo.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveService {

    private final StaffProfileRepository staffProfileRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveQuotaRepository leaveQuotaRepository;
    private final TenantAccessGuard tenantGuard;
    private final HrAuditService auditService;

    // US-07003: Submit digital leave application
    @Transactional
    public LeaveApplicationResponse applyLeave(LeaveApplicationRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());
        // Staff apply for themselves; HR, principals and department heads may record leave for anyone
        if (!tenantGuard.hasAnyRole(HrRoles.HR_OFFICERS)
                && (staff.getUserId() == null || !tenantGuard.currentUserId().map(staff.getUserId()::equals).orElse(false))) {
            throw new ForbiddenException("You may only apply for leave on your own staff profile");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        BigDecimal totalDays = BigDecimal.valueOf(days);

        LeaveApplication leave = LeaveApplication.builder()
                .tenantId(staff.getTenantId())
                .staffId(staff.getStaffId())
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(totalDays)
                .reason(request.getReason())
                .proxyNotes(request.getProxyNotes())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveApplication saved = leaveApplicationRepository.save(leave);
        auditService.logSuccess("LeaveApplication", "APPLY_LEAVE",
                "Leave applied for staffId=" + staff.getStaffId() + " days=" + totalDays);
        return mapToResponse(saved);
    }

    // US-07004: Approve or reject staff leave requests
    @Transactional
    public LeaveApplicationResponse reviewLeave(UUID applicationId, LeaveReviewRequest request) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveApplication", "applicationId", applicationId));
        tenantGuard.requireTenantRecord(leave.getTenantId(), "LeaveApplication", "applicationId", applicationId);

        leave.setStatus(request.getStatus());
        leave.setReviewedBy(SecurityUtils.getCurrentUserEmail().orElse("admin@kvs.local"));
        leave.setReviewComments(request.getReviewComments());

        // If approved and not LWP, update quota balance
        if (request.getStatus() == LeaveStatus.APPROVED && leave.getLeaveType() != LeaveType.LWP) {
            int year = leave.getStartDate().getYear();
            Optional<LeaveQuota> quotaOpt = leaveQuotaRepository.findByStaffIdAndQuotaYearAndLeaveType(
                    leave.getStaffId(), year, leave.getLeaveType());
            if (quotaOpt.isPresent()) {
                LeaveQuota quota = quotaOpt.get();
                quota.setUsedDays(quota.getUsedDays().add(leave.getTotalDays()));
                quota.computeBalance();
                leaveQuotaRepository.save(quota);
            }
        }

        LeaveApplication saved = leaveApplicationRepository.save(leave);
        auditService.logSuccess("LeaveApplication", "REVIEW_LEAVE",
                "Leave decision " + request.getStatus() + " for applicationId=" + applicationId);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public LeaveApplicationResponse getLeave(UUID applicationId) {
        LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveApplication", "applicationId", applicationId));
        tenantGuard.requireTenantRecord(leave.getTenantId(), "LeaveApplication", "applicationId", applicationId);
        return mapToResponse(leave);
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> listLeaves(UUID staffId, LeaveStatus status) {
        UUID tenantId = tenantGuard.requireCurrentTenantId();
        List<LeaveApplication> list;
        if (staffId != null) {
            requireStaffOfTenant(staffId);
            list = leaveApplicationRepository.findByStaffIdOrderByCreatedAtDesc(staffId);
        } else if (status != null) {
            list = leaveApplicationRepository.findByTenantIdAndStatus(tenantId, status);
        } else {
            list = leaveApplicationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        }
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // US-07017: Track staff maternity, medical and statutory leave quotas
    @Transactional
    public LeaveQuotaResponse configureQuota(LeaveQuotaRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        LeaveQuota quota = leaveQuotaRepository.findByStaffIdAndQuotaYearAndLeaveType(
                request.getStaffId(), request.getQuotaYear(), request.getLeaveType())
                .orElseGet(() -> LeaveQuota.builder()
                        .tenantId(staff.getTenantId())
                        .staffId(staff.getStaffId())
                        .quotaYear(request.getQuotaYear())
                        .leaveType(request.getLeaveType())
                        .usedDays(BigDecimal.ZERO)
                        .build());

        quota.setTotalEntitled(request.getTotalEntitled());
        quota.computeBalance();

        LeaveQuota saved = leaveQuotaRepository.save(quota);
        auditService.logSuccess("LeaveQuota", "CONFIGURE_QUOTA",
                "Configured quota for staffId=" + staff.getStaffId() + " type=" + request.getLeaveType());
        return mapToQuotaResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveQuotaResponse> listQuotas(UUID staffId, Integer quotaYear) {
        UUID tenantId = tenantGuard.requireCurrentTenantId();
        int year = quotaYear != null ? quotaYear : java.time.LocalDate.now().getYear();
        if (staffId != null) {
            requireStaffOfTenant(staffId);
        }
        List<LeaveQuota> list = (staffId != null)
                ? leaveQuotaRepository.findByStaffIdAndQuotaYear(staffId, year)
                : leaveQuotaRepository.findByTenantIdAndQuotaYear(tenantId, year);
        return list.stream().map(this::mapToQuotaResponse).collect(Collectors.toList());
    }

    /** A staff member of another tenant is reported as not found (no cross-tenant reads by staffId). */
    private void requireStaffOfTenant(UUID staffId) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);
    }

    private LeaveApplicationResponse mapToResponse(LeaveApplication l) {
        return LeaveApplicationResponse.builder()
                .applicationId(l.getApplicationId())
                .tenantId(l.getTenantId())
                .staffId(l.getStaffId())
                .leaveType(l.getLeaveType())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .totalDays(l.getTotalDays())
                .reason(l.getReason())
                .proxyNotes(l.getProxyNotes())
                .status(l.getStatus())
                .reviewedBy(l.getReviewedBy())
                .reviewComments(l.getReviewComments())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private LeaveQuotaResponse mapToQuotaResponse(LeaveQuota q) {
        return LeaveQuotaResponse.builder()
                .quotaId(q.getQuotaId())
                .tenantId(q.getTenantId())
                .staffId(q.getStaffId())
                .quotaYear(q.getQuotaYear())
                .leaveType(q.getLeaveType())
                .totalEntitled(q.getTotalEntitled())
                .usedDays(q.getUsedDays())
                .balanceDays(q.getBalanceDays())
                .build();
    }
}
