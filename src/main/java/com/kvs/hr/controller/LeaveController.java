package com.kvs.hr.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.hr.config.HrRoles;
import com.kvs.hr.dto.*;
import com.kvs.hr.enums.LeaveStatus;
import com.kvs.hr.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hr/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // US-07003: Submit digital leave application
    @PostMapping
    @PreAuthorize(HrRoles.LEAVE_APPLY)
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> applyLeave(
            @Valid @RequestBody LeaveApplicationRequest request) {
        LeaveApplicationResponse data = leaveService.applyLeave(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<LeaveApplicationResponse>builder()
                        .success(true)
                        .message("Leave application submitted successfully")
                        .data(data)
                        .build());
    }

    // US-07003: Get leave application
    @GetMapping("/{applicationId}")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> getLeave(@PathVariable UUID applicationId) {
        LeaveApplicationResponse data = leaveService.getLeave(applicationId);
        return ResponseEntity.ok(ApiResponse.<LeaveApplicationResponse>builder()
                .success(true)
                .message("Leave application retrieved successfully")
                .data(data)
                .build());
    }

    // US-07003: List leave applications
    @GetMapping
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<List<LeaveApplicationResponse>>> listLeaves(
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) LeaveStatus status) {
        List<LeaveApplicationResponse> data = leaveService.listLeaves(staffId, status);
        return ResponseEntity.ok(ApiResponse.<List<LeaveApplicationResponse>>builder()
                .success(true)
                .message("Leave applications retrieved successfully")
                .data(data)
                .build());
    }

    // US-07004: Approve or reject staff leave requests
    @PutMapping("/{applicationId}/review")
    @PreAuthorize(HrRoles.LEAVE_APPROVE)
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> reviewLeave(
            @PathVariable UUID applicationId,
            @Valid @RequestBody LeaveReviewRequest request) {
        LeaveApplicationResponse data = leaveService.reviewLeave(applicationId, request);
        return ResponseEntity.ok(ApiResponse.<LeaveApplicationResponse>builder()
                .success(true)
                .message("Leave application reviewed successfully")
                .data(data)
                .build());
    }

    // US-07017: Track staff maternity, medical and statutory leave quotas
    @PostMapping("/quotas")
    @PreAuthorize(HrRoles.STATUTORY_QUOTAS)
    public ResponseEntity<ApiResponse<LeaveQuotaResponse>> configureQuota(
            @Valid @RequestBody LeaveQuotaRequest request) {
        LeaveQuotaResponse data = leaveService.configureQuota(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<LeaveQuotaResponse>builder()
                        .success(true)
                        .message("Leave quota configured successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/quotas")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<List<LeaveQuotaResponse>>> listQuotas(
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) Integer quotaYear) {
        List<LeaveQuotaResponse> data = leaveService.listQuotas(staffId, quotaYear);
        return ResponseEntity.ok(ApiResponse.<List<LeaveQuotaResponse>>builder()
                .success(true)
                .message("Leave quotas retrieved successfully")
                .data(data)
                .build());
    }
}
