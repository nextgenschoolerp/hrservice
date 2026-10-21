package com.kvs.hr.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.hr.config.HrRoles;
import com.kvs.hr.dto.*;
import com.kvs.hr.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hr/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // US-07000: Create employee master record
    @PostMapping
    @PreAuthorize(HrRoles.EMPLOYEE_MANAGE)
    public ResponseEntity<ApiResponse<StaffProfileResponse>> createEmployee(
            @Valid @RequestBody StaffProfileRequest request) {
        StaffProfileResponse data = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StaffProfileResponse>builder()
                        .success(true)
                        .message("Employee profile created successfully")
                        .data(data)
                        .build());
    }

    // US-07000: Get employee by ID
    @GetMapping("/{staffId}")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<StaffProfileResponse>> getEmployee(@PathVariable UUID staffId) {
        StaffProfileResponse data = employeeService.getEmployee(staffId);
        return ResponseEntity.ok(ApiResponse.<StaffProfileResponse>builder()
                .success(true)
                .message("Employee profile retrieved successfully")
                .data(data)
                .build());
    }

    // US-07000: Get employee by employee code
    @GetMapping("/by-code/{code}")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<StaffProfileResponse>> getEmployeeByCode(@PathVariable String code) {
        StaffProfileResponse data = employeeService.getEmployeeByCode(code);
        return ResponseEntity.ok(ApiResponse.<StaffProfileResponse>builder()
                .success(true)
                .message("Employee profile retrieved successfully")
                .data(data)
                .build());
    }

    // US-07000: The signed-in staff member's own profile (matched on the token's userId)
    @GetMapping("/me")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<StaffProfileResponse>> getMyProfile() {
        StaffProfileResponse data = employeeService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.<StaffProfileResponse>builder()
                .success(true)
                .message("Employee profile retrieved successfully")
                .data(data)
                .build());
    }

    // US-07000: List employees (optionally one campus and/or one department)
    @GetMapping
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<List<StaffProfileResponse>>> listEmployees(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String branchId) {
        List<StaffProfileResponse> data = employeeService.listEmployees(department, branchId);
        return ResponseEntity.ok(ApiResponse.<List<StaffProfileResponse>>builder()
                .success(true)
                .message("Employee profiles retrieved successfully")
                .data(data)
                .build());
    }

    // US-07000: Update employee master record
    @PutMapping("/{staffId}")
    @PreAuthorize(HrRoles.EMPLOYEE_MANAGE)
    public ResponseEntity<ApiResponse<StaffProfileResponse>> updateEmployee(
            @PathVariable UUID staffId,
            @Valid @RequestBody StaffProfileRequest request) {
        StaffProfileResponse data = employeeService.updateEmployee(staffId, request);
        return ResponseEntity.ok(ApiResponse.<StaffProfileResponse>builder()
                .success(true)
                .message("Employee profile updated successfully")
                .data(data)
                .build());
    }

    // US-07010: Track probation completion dates and schedule appraisal reviews
    @PostMapping("/appraisals")
    @PreAuthorize(HrRoles.PROBATION_APPRAISAL)
    public ResponseEntity<ApiResponse<StaffAppraisalResponse>> recordAppraisal(
            @Valid @RequestBody StaffAppraisalRequest request) {
        StaffAppraisalResponse data = employeeService.recordAppraisal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StaffAppraisalResponse>builder()
                        .success(true)
                        .message("Staff appraisal recorded successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/{staffId}/appraisals")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<List<StaffAppraisalResponse>>> listAppraisals(@PathVariable UUID staffId) {
        List<StaffAppraisalResponse> data = employeeService.listAppraisals(staffId);
        return ResponseEntity.ok(ApiResponse.<List<StaffAppraisalResponse>>builder()
                .success(true)
                .message("Staff appraisals retrieved successfully")
                .data(data)
                .build());
    }

    // US-07016: Manage staff resignation notices and clearance NOC workflow
    @PostMapping("/resignations")
    @PreAuthorize(HrRoles.RESIGNATION_CLEARANCE)
    public ResponseEntity<ApiResponse<StaffResignationResponse>> submitResignation(
            @Valid @RequestBody StaffResignationRequest request) {
        StaffResignationResponse data = employeeService.submitResignation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StaffResignationResponse>builder()
                        .success(true)
                        .message("Resignation submitted successfully")
                        .data(data)
                        .build());
    }

    @PutMapping("/resignations/{resignationId}/clearance")
    @PreAuthorize(HrRoles.RESIGNATION_CLEARANCE)
    public ResponseEntity<ApiResponse<StaffResignationResponse>> updateClearance(
            @PathVariable UUID resignationId,
            @Valid @RequestBody DepartmentClearanceRequest request) {
        StaffResignationResponse data = employeeService.updateClearance(resignationId, request);
        return ResponseEntity.ok(ApiResponse.<StaffResignationResponse>builder()
                .success(true)
                .message("Department clearance updated successfully")
                .data(data)
                .build());
    }

    @GetMapping("/resignations/{resignationId}")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<StaffResignationResponse>> getResignation(@PathVariable UUID resignationId) {
        StaffResignationResponse data = employeeService.getResignation(resignationId);
        return ResponseEntity.ok(ApiResponse.<StaffResignationResponse>builder()
                .success(true)
                .message("Resignation record retrieved successfully")
                .data(data)
                .build());
    }

    // US-07019: Archive retired staff employment records
    @PostMapping("/archives")
    @PreAuthorize(HrRoles.ARCHIVE_MANAGE)
    public ResponseEntity<ApiResponse<StaffArchiveResponse>> archiveStaff(
            @Valid @RequestBody StaffArchiveRequest request) {
        StaffArchiveResponse data = employeeService.archiveStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StaffArchiveResponse>builder()
                        .success(true)
                        .message("Staff record archived successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/archives")
    @PreAuthorize(HrRoles.ARCHIVE_MANAGE)
    public ResponseEntity<ApiResponse<List<StaffArchiveResponse>>> listArchives() {
        List<StaffArchiveResponse> data = employeeService.listArchives();
        return ResponseEntity.ok(ApiResponse.<List<StaffArchiveResponse>>builder()
                .success(true)
                .message("Archived staff records retrieved successfully")
                .data(data)
                .build());
    }
}
