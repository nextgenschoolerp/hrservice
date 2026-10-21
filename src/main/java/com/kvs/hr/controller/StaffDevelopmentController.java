package com.kvs.hr.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.hr.config.HrRoles;
import com.kvs.hr.dto.*;
import com.kvs.hr.service.StaffDevelopmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hr/development")
@RequiredArgsConstructor
public class StaffDevelopmentController {

    private final StaffDevelopmentService developmentService;

    // US-07009: Log staff Continuous Professional Development (CPD) training hours and credits
    @PostMapping("/cpd")
    @PreAuthorize(HrRoles.CPD_MANAGE)
    public ResponseEntity<ApiResponse<CpdRecordResponse>> logCpd(
            @Valid @RequestBody CpdRecordRequest request) {
        CpdRecordResponse data = developmentService.logCpd(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CpdRecordResponse>builder()
                        .success(true)
                        .message("CPD training record logged successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/cpd/{staffId}")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<List<CpdRecordResponse>>> listCpd(@PathVariable UUID staffId) {
        List<CpdRecordResponse> data = developmentService.listCpd(staffId);
        return ResponseEntity.ok(ApiResponse.<List<CpdRecordResponse>>builder()
                .success(true)
                .message("CPD training records retrieved successfully")
                .data(data)
                .build());
    }

    // US-07011: Calculate gratuity and encashment values during staff offboarding
    @PostMapping("/settlement")
    @PreAuthorize(HrRoles.OFFBOARDING_SETTLEMENT)
    public ResponseEntity<ApiResponse<StaffSettlementResponse>> calculateSettlement(
            @Valid @RequestBody StaffSettlementRequest request) {
        StaffSettlementResponse data = developmentService.calculateSettlement(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StaffSettlementResponse>builder()
                        .success(true)
                        .message("Staff settlement, gratuity, and leave encashment calculated successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/settlement/{staffId}")
    @PreAuthorize(HrRoles.OFFBOARDING_SETTLEMENT)
    public ResponseEntity<ApiResponse<StaffSettlementResponse>> getSettlement(@PathVariable UUID staffId) {
        StaffSettlementResponse data = developmentService.getSettlement(staffId);
        return ResponseEntity.ok(ApiResponse.<StaffSettlementResponse>builder()
                .success(true)
                .message("Staff settlement details retrieved successfully")
                .data(data)
                .build());
    }
}
