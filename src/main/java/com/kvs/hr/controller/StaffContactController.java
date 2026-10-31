package com.kvs.hr.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.core.exception.ForbiddenException;
import com.kvs.hr.entity.StaffProfile;
import com.kvs.hr.repo.StaffProfileRepository;
import com.kvs.hr.service.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Staff to send a circular or emergency alert to (communicationservice, Communications → Circulars): active staff with their
 * work phone and email. Readable by those who may send circulars.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hr/staff-contacts")
public class StaffContactController {

    private static final String SEND = "hasAnyRole('PUBLIC_RELATIONS_OFFICER', 'COMMUNICATION_OFFICER', 'PRINCIPAL', 'SCHOOL_PRINCIPAL', 'SUPER_ADMIN', 'ADMIN') or hasAnyAuthority('ROLE_PUBLIC_RELATIONS_OFFICER', 'ROLE_COMMUNICATION_OFFICER', 'ROLE_PRINCIPAL', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_SUPER_ADMIN', 'ROLE_ADMIN')";

    public record StaffContact(UUID staffId, String name, String department, String designation, String phone, String email) { }

    private final StaffProfileRepository staff;
    private final TenantAccessGuard tenantGuard;

    @GetMapping
    @PreAuthorize(SEND)
    public ResponseEntity<ApiResponse<List<StaffContact>>> contacts(@RequestParam("tenantId") String tenantId) {
        if (!tenantGuard.canAccessTenant(tenantId)) throw new ForbiddenException("You may only access records of your own institution");
        List<StaffContact> out = staff.findByTenantId(tenantId).stream()
                .filter(s -> !Boolean.FALSE.equals(s.getIsActive()))
                .sorted(Comparator.comparing(StaffProfile::getFirstName))
                .map(s -> new StaffContact(s.getStaffId(), s.getFirstName() + " " + s.getLastName(), s.getDepartment(), s.getDesignation(), s.getPhone(), s.getEmail()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(out));
    }
}
