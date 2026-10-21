package com.kvs.hr.service;

import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.hr.dto.*;
import com.kvs.hr.entity.*;
import com.kvs.hr.enums.LeaveType;
import com.kvs.hr.enums.SettlementStatus;
import com.kvs.hr.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StaffDevelopmentService {

    private final StaffProfileRepository staffProfileRepository;
    private final CpdRecordRepository cpdRecordRepository;
    private final StaffSettlementRepository staffSettlementRepository;
    private final LeaveQuotaRepository leaveQuotaRepository;
    private final TenantAccessGuard tenantGuard;
    private final HrAuditService auditService;

    // US-07009: Log CPD training hours and credits
    @Transactional
    public CpdRecordResponse logCpd(CpdRecordRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        CpdRecord cpd = CpdRecord.builder()
                .tenantId(staff.getTenantId())
                .staffId(staff.getStaffId())
                .courseTitle(request.getCourseTitle())
                .conductingAgency(request.getConductingAgency())
                .completionDate(request.getCompletionDate())
                .trainingHours(request.getTrainingHours())
                .creditsEarned(request.getCreditsEarned())
                .certificateReference(request.getCertificateReference())
                .build();

        CpdRecord saved = cpdRecordRepository.save(cpd);
        auditService.logSuccess("CpdRecord", "LOG_CPD",
                "Logged CPD course=" + request.getCourseTitle() + " credits=" + request.getCreditsEarned() + " for staffId=" + staff.getStaffId());
        return mapToCpdResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CpdRecordResponse> listCpd(UUID staffId) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);

        return cpdRecordRepository.findByStaffIdOrderByCompletionDateDesc(staffId).stream()
                .map(this::mapToCpdResponse)
                .collect(Collectors.toList());
    }

    // US-07011: Calculate gratuity and encashment values during staff offboarding
    @Transactional
    public StaffSettlementResponse calculateSettlement(StaffSettlementRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        long years = ChronoUnit.YEARS.between(staff.getJoiningDate(), request.getLastWorkingDay());
        int completedYears = (int) Math.max(0, years);

        // Gratuity calculation: (15 * lastDrawnBasic * completedYears) / 26
        BigDecimal gratuity = BigDecimal.ZERO;
        if (completedYears >= 5) {
            gratuity = staff.getBasicSalary()
                    .multiply(BigDecimal.valueOf(15L * completedYears))
                    .divide(BigDecimal.valueOf(26), 2, RoundingMode.HALF_UP);
        }

        // Leave encashment calculation: balance earned leaves * daily rate
        BigDecimal encashment = BigDecimal.ZERO;
        Optional<LeaveQuota> earnedQuota = leaveQuotaRepository.findByStaffIdAndQuotaYearAndLeaveType(
                staff.getStaffId(), request.getLastWorkingDay().getYear(), LeaveType.EARNED);
        if (earnedQuota.isPresent() && earnedQuota.get().getBalanceDays().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dailyRate = staff.getBasicSalary().divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
            encashment = dailyRate.multiply(earnedQuota.get().getBalanceDays()).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal np = request.getNoticePayAdjustment() != null ? request.getNoticePayAdjustment() : BigDecimal.ZERO;
        BigDecimal oa = request.getOtherAdditions() != null ? request.getOtherAdditions() : BigDecimal.ZERO;
        BigDecimal od = request.getOtherDeductions() != null ? request.getOtherDeductions() : BigDecimal.ZERO;

        StaffSettlement settlement = staffSettlementRepository.findByStaffId(staff.getStaffId())
                .orElseGet(() -> StaffSettlement.builder()
                        .tenantId(staff.getTenantId())
                        .staffId(staff.getStaffId())
                        .build());

        settlement.setResignationId(request.getResignationId());
        settlement.setLastWorkingDay(request.getLastWorkingDay());
        settlement.setCompletedYearsService(completedYears);
        settlement.setGratuityAmount(gratuity);
        settlement.setLeaveEncashmentAmount(encashment);
        settlement.setNoticePayAdjustment(np);
        settlement.setOtherAdditions(oa);
        settlement.setOtherDeductions(od);
        settlement.computeNet();
        settlement.setStatus(SettlementStatus.DRAFT);
        settlement.setRemarks(request.getRemarks());

        StaffSettlement saved = staffSettlementRepository.save(settlement);
        auditService.logSuccess("StaffSettlement", "CALCULATE_SETTLEMENT",
                "Settlement calculated for staffId=" + staff.getStaffId() + " net=" + saved.getNetSettlementAmount());
        return mapToSettlementResponse(saved);
    }

    @Transactional(readOnly = true)
    public StaffSettlementResponse getSettlement(UUID staffId) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);

        StaffSettlement settlement = staffSettlementRepository.findByStaffId(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffSettlement", "staffId", staffId));
        return mapToSettlementResponse(settlement);
    }

    private CpdRecordResponse mapToCpdResponse(CpdRecord c) {
        return CpdRecordResponse.builder()
                .cpdId(c.getCpdId())
                .tenantId(c.getTenantId())
                .staffId(c.getStaffId())
                .courseTitle(c.getCourseTitle())
                .conductingAgency(c.getConductingAgency())
                .completionDate(c.getCompletionDate())
                .trainingHours(c.getTrainingHours())
                .creditsEarned(c.getCreditsEarned())
                .certificateReference(c.getCertificateReference())
                .build();
    }

    private StaffSettlementResponse mapToSettlementResponse(StaffSettlement s) {
        return StaffSettlementResponse.builder()
                .settlementId(s.getSettlementId())
                .tenantId(s.getTenantId())
                .staffId(s.getStaffId())
                .resignationId(s.getResignationId())
                .lastWorkingDay(s.getLastWorkingDay())
                .completedYearsService(s.getCompletedYearsService())
                .gratuityAmount(s.getGratuityAmount())
                .leaveEncashmentAmount(s.getLeaveEncashmentAmount())
                .noticePayAdjustment(s.getNoticePayAdjustment())
                .otherAdditions(s.getOtherAdditions())
                .otherDeductions(s.getOtherDeductions())
                .netSettlementAmount(s.getNetSettlementAmount())
                .status(s.getStatus())
                .remarks(s.getRemarks())
                .build();
    }
}
