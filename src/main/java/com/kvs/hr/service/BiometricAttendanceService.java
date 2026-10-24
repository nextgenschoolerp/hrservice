package com.kvs.hr.service;

import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.hr.dto.*;
import com.kvs.hr.entity.*;
import com.kvs.hr.enums.AnomalyType;
import com.kvs.hr.enums.RegularisationStatus;
import com.kvs.hr.enums.ScanType;
import com.kvs.hr.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BiometricAttendanceService {

    private final StaffProfileRepository staffProfileRepository;
    private final StaffBiometricLogRepository biometricLogRepository;
    private final StaffShiftRepository shiftRepository;
    private final StaffShiftAssignmentRepository shiftAssignmentRepository;
    private final BiometricAnomalyRepository anomalyRepository;
    private final TenantAccessGuard tenantGuard;
    private final HrAuditService auditService;

    // US-07001: Ingest biometric scan logs
    @Transactional
    public BiometricScanResponse ingestScan(BiometricScanRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        StaffBiometricLog logEntry = StaffBiometricLog.builder()
                .tenantId(staff.getTenantId())
                .staffId(staff.getStaffId())
                .terminalDeviceId(request.getTerminalDeviceId())
                .scanTimestamp(request.getScanTimestamp())
                .scanType(request.getScanType())
                .build();

        StaffBiometricLog saved = biometricLogRepository.save(logEntry);
        auditService.logSuccess("StaffBiometricLog", "INGEST_SCAN",
                "Scan logged staffId=" + staff.getStaffId() + " type=" + request.getScanType());
        return BiometricScanResponse.builder()
                .logId(saved.getLogId())
                .staffId(saved.getStaffId())
                .tenantId(saved.getTenantId())
                .terminalDeviceId(saved.getTerminalDeviceId())
                .scanTimestamp(saved.getScanTimestamp())
                .scanType(saved.getScanType())
                .build();
    }

    @Transactional(readOnly = true)
    public List<BiometricScanResponse> listScans(UUID staffId) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);

        return biometricLogRepository.findByStaffIdOrderByScanTimestampDesc(staffId).stream()
                .map(s -> BiometricScanResponse.builder()
                        .logId(s.getLogId())
                        .staffId(s.getStaffId())
                        .tenantId(s.getTenantId())
                        .terminalDeviceId(s.getTerminalDeviceId())
                        .scanTimestamp(s.getScanTimestamp())
                        .scanType(s.getScanType())
                        .build())
                .collect(Collectors.toList());
    }

    // US-07002: Shift roster planning
    @Transactional
    public StaffShiftResponse createShift(StaffShiftRequest request) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        if (shiftRepository.findByTenantIdAndShiftCode(tenantId, request.getShiftCode()).isPresent()) {
            throw new ConflictException("Shift with code " + request.getShiftCode() + " already exists", "DUPLICATE_SHIFT_CODE");
        }

        StaffShift shift = StaffShift.builder()
                .tenantId(tenantId)
                .shiftCode(request.getShiftCode())
                .shiftName(request.getShiftName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .graceMinutes(request.getGraceMinutes() != null ? request.getGraceMinutes() : 15)
                .halfDayThresholdMinutes(request.getHalfDayThresholdMinutes() != null ? request.getHalfDayThresholdMinutes() : 240)
                .isActive(true)
                .build();

        StaffShift saved = shiftRepository.save(shift);
        auditService.logSuccess("StaffShift", "CREATE_SHIFT", "Created shift=" + saved.getShiftCode());
        return mapToShiftResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StaffShiftResponse> listShifts() {
        String tenantId = tenantGuard.requireCurrentTenantId();
        return shiftRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .map(this::mapToShiftResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignShift(ShiftAssignmentRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        StaffShift shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffShift", "shiftId", request.getShiftId()));
        tenantGuard.requireTenantRecord(shift.getTenantId(), "StaffShift", "shiftId", request.getShiftId());

        StaffShiftAssignment assignment = StaffShiftAssignment.builder()
                .tenantId(staff.getTenantId())
                .staffId(staff.getStaffId())
                .shiftId(shift.getShiftId())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();

        shiftAssignmentRepository.save(assignment);
        auditService.logSuccess("StaffShiftAssignment", "ASSIGN_SHIFT",
                "Assigned shift=" + shift.getShiftCode() + " to staffId=" + staff.getStaffId());
    }

    // US-07013: Department-wise staff attendance summaries on principal dashboard
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummary(LocalDate date) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        LocalDate queryDate = date != null ? date : LocalDate.now();
        Instant dayStart = queryDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = queryDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<StaffProfile> activeStaff = staffProfileRepository.findByTenantId(tenantId).stream()
                .filter(StaffProfile::getIsActive)
                .toList();
        long totalStaff = activeStaff.size();

        List<StaffBiometricLog> dayLogs = biometricLogRepository.findByTenantIdAndScanTimestampBetween(tenantId, dayStart, dayEnd);
        Set<UUID> presentStaffIds = dayLogs.stream()
                .map(StaffBiometricLog::getStaffId)
                .collect(Collectors.toSet());

        long presentCount = activeStaff.stream().filter(s -> presentStaffIds.contains(s.getStaffId())).count();
        long absentCount = Math.max(0, totalStaff - presentCount);

        BigDecimal pct = totalStaff > 0
                ? BigDecimal.valueOf(presentCount * 100.0 / totalStaff).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Long> deptCounts = activeStaff.stream()
                .filter(s -> presentStaffIds.contains(s.getStaffId()))
                .collect(Collectors.groupingBy(StaffProfile::getDepartment, Collectors.counting()));

        auditService.logSuccess("StaffAttendance", "VIEW_SUMMARY", "Viewed attendance summary for date=" + queryDate);
        return AttendanceSummaryResponse.builder()
                .date(queryDate)
                .totalStaff(totalStaff)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .onLeaveCount(0L)
                .attendancePercentage(pct)
                .departmentPresentCounts(deptCounts)
                .build();
    }

    // US-07015: Flag staff biometric punch anomalies & regularisation
    @Transactional
    public BiometricAnomalyResponse flagAnomaly(UUID staffId, LocalDate anomalyDate, AnomalyType type) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);

        BiometricAnomaly anomaly = BiometricAnomaly.builder()
                .tenantId(staff.getTenantId())
                .staffId(staff.getStaffId())
                .anomalyDate(anomalyDate)
                .anomalyType(type)
                .status(RegularisationStatus.PENDING)
                .build();

        BiometricAnomaly saved = anomalyRepository.save(anomaly);
        auditService.logSuccess("BiometricAnomaly", "FLAG_ANOMALY",
                "Flagged anomaly " + type + " for staffId=" + staffId);
        return mapToAnomalyResponse(saved);
    }

    @Transactional
    public BiometricAnomalyResponse regulariseAnomaly(UUID anomalyId, AnomalyRegularisationRequest request) {
        BiometricAnomaly anomaly = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new ResourceNotFoundException("BiometricAnomaly", "anomalyId", anomalyId));
        tenantGuard.requireTenantRecord(anomaly.getTenantId(), "BiometricAnomaly", "anomalyId", anomalyId);

        anomaly.setRegularisationReason(request.getRegularisationReason());
        anomaly.setCorrectedInTime(request.getCorrectedInTime());
        anomaly.setCorrectedOutTime(request.getCorrectedOutTime());
        anomaly.setStatus(RegularisationStatus.APPROVED);
        anomaly.setReviewedBy(SecurityUtils.getCurrentUserEmail().orElse("hr@kvs.local"));

        BiometricAnomaly saved = anomalyRepository.save(anomaly);
        auditService.logSuccess("BiometricAnomaly", "REGULARISE_ANOMALY", "Regularised anomalyId=" + anomalyId);
        return mapToAnomalyResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BiometricAnomalyResponse> listAnomalies(UUID staffId) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        List<BiometricAnomaly> list = (staffId != null)
                ? anomalyRepository.findByStaffIdOrderByAnomalyDateDesc(staffId)
                : anomalyRepository.findByTenantIdOrderByAnomalyDateDesc(tenantId);
        return list.stream().map(this::mapToAnomalyResponse).collect(Collectors.toList());
    }

    private StaffShiftResponse mapToShiftResponse(StaffShift s) {
        return StaffShiftResponse.builder()
                .shiftId(s.getShiftId())
                .tenantId(s.getTenantId())
                .shiftCode(s.getShiftCode())
                .shiftName(s.getShiftName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .graceMinutes(s.getGraceMinutes())
                .halfDayThresholdMinutes(s.getHalfDayThresholdMinutes())
                .isActive(s.getIsActive())
                .build();
    }

    private BiometricAnomalyResponse mapToAnomalyResponse(BiometricAnomaly a) {
        return BiometricAnomalyResponse.builder()
                .anomalyId(a.getAnomalyId())
                .tenantId(a.getTenantId())
                .staffId(a.getStaffId())
                .anomalyDate(a.getAnomalyDate())
                .anomalyType(a.getAnomalyType())
                .status(a.getStatus())
                .regularisationReason(a.getRegularisationReason())
                .correctedInTime(a.getCorrectedInTime())
                .correctedOutTime(a.getCorrectedOutTime())
                .reviewedBy(a.getReviewedBy())
                .build();
    }
}
