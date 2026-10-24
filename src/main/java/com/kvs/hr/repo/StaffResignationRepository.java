package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffResignation;
import com.kvs.hr.enums.ResignationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffResignationRepository extends JpaRepository<StaffResignation, UUID> {
    Optional<StaffResignation> findByResignationIdAndTenantId(UUID resignationId, String tenantId);
    Optional<StaffResignation> findTopByStaffIdOrderBySubmissionDateDesc(UUID staffId);
    List<StaffResignation> findByTenantIdOrderBySubmissionDateDesc(String tenantId);
    List<StaffResignation> findByTenantIdAndStatus(String tenantId, ResignationStatus status);
}
