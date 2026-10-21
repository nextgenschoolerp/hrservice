package com.kvs.hr.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.hr.config.HrRoles;
import com.kvs.hr.dto.*;
import com.kvs.hr.enums.AnomalyType;
import com.kvs.hr.service.BiometricAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hr/attendance")
@RequiredArgsConstructor
public class BiometricAttendanceController {

    private final BiometricAttendanceService attendanceService;

    // US-07001: Ingest biometric/RFID hardware terminal scan logs
    @PostMapping("/scans")
    @PreAuthorize(HrRoles.BIOMETRIC_INGEST)
    public ResponseEntity<ApiResponse<BiometricScanResponse>> ingestScan(
            @Valid @RequestBody BiometricScanRequest request) {
        BiometricScanResponse data = attendanceService.ingestScan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BiometricScanResponse>builder()
                        .success(true)
                        .message("Biometric scan logged successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/scans/{staffId}")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<List<BiometricScanResponse>>> listScans(@PathVariable UUID staffId) {
        List<BiometricScanResponse> data = attendanceService.listScans(staffId);
        return ResponseEntity.ok(ApiResponse.<List<BiometricScanResponse>>builder()
                .success(true)
                .message("Biometric scan history retrieved successfully")
                .data(data)
                .build());
    }

    // US-07002: Shift roster planning
    @PostMapping("/shifts")
    @PreAuthorize(HrRoles.SHIFT_ROSTER)
    public ResponseEntity<ApiResponse<StaffShiftResponse>> createShift(
            @Valid @RequestBody StaffShiftRequest request) {
        StaffShiftResponse data = attendanceService.createShift(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StaffShiftResponse>builder()
                        .success(true)
                        .message("Shift schedule created successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/shifts")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<List<StaffShiftResponse>>> listShifts() {
        List<StaffShiftResponse> data = attendanceService.listShifts();
        return ResponseEntity.ok(ApiResponse.<List<StaffShiftResponse>>builder()
                .success(true)
                .message("Shift schedules retrieved successfully")
                .data(data)
                .build());
    }

    @PostMapping("/shifts/assignments")
    @PreAuthorize(HrRoles.SHIFT_ROSTER)
    public ResponseEntity<ApiResponse<Void>> assignShift(
            @Valid @RequestBody ShiftAssignmentRequest request) {
        attendanceService.assignShift(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Shift assigned to staff successfully")
                .build());
    }

    // US-07013: Department-wise staff attendance summaries on principal dashboard
    @GetMapping("/summary")
    @PreAuthorize(HrRoles.ATTENDANCE_ANALYTICS)
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getAttendanceSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AttendanceSummaryResponse data = attendanceService.getAttendanceSummary(date);
        return ResponseEntity.ok(ApiResponse.<AttendanceSummaryResponse>builder()
                .success(true)
                .message("Attendance summary retrieved successfully")
                .data(data)
                .build());
    }

    // US-07015: Flag staff biometric punch anomalies & regularisation
    @PostMapping("/anomalies/{staffId}")
    @PreAuthorize(HrRoles.ANOMALY_REGULARISE)
    public ResponseEntity<ApiResponse<BiometricAnomalyResponse>> flagAnomaly(
            @PathVariable UUID staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anomalyDate,
            @RequestParam AnomalyType type) {
        BiometricAnomalyResponse data = attendanceService.flagAnomaly(staffId, anomalyDate, type);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BiometricAnomalyResponse>builder()
                        .success(true)
                        .message("Biometric anomaly flagged successfully")
                        .data(data)
                        .build());
    }

    @PutMapping("/anomalies/{anomalyId}/regularise")
    @PreAuthorize(HrRoles.ANOMALY_REGULARISE)
    public ResponseEntity<ApiResponse<BiometricAnomalyResponse>> regulariseAnomaly(
            @PathVariable UUID anomalyId,
            @Valid @RequestBody AnomalyRegularisationRequest request) {
        BiometricAnomalyResponse data = attendanceService.regulariseAnomaly(anomalyId, request);
        return ResponseEntity.ok(ApiResponse.<BiometricAnomalyResponse>builder()
                .success(true)
                .message("Punch anomaly regularised successfully")
                .data(data)
                .build());
    }

    @GetMapping("/anomalies")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<List<BiometricAnomalyResponse>>> listAnomalies(
            @RequestParam(required = false) UUID staffId) {
        List<BiometricAnomalyResponse> data = attendanceService.listAnomalies(staffId);
        return ResponseEntity.ok(ApiResponse.<List<BiometricAnomalyResponse>>builder()
                .success(true)
                .message("Biometric anomalies retrieved successfully")
                .data(data)
                .build());
    }
}
