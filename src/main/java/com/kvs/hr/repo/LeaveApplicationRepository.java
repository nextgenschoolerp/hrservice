package com.kvs.hr.repo;

import com.kvs.hr.entity.LeaveApplication;
import com.kvs.hr.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, UUID> {
    Optional<LeaveApplication> findByApplicationIdAndTenantId(UUID applicationId, String tenantId);
    List<LeaveApplication> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<LeaveApplication> findByStaffIdOrderByCreatedAtDesc(UUID staffId);
    List<LeaveApplication> findByTenantIdAndStatus(String tenantId, LeaveStatus status);
    List<LeaveApplication> findByStaffIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID staffId, LeaveStatus status, LocalDate end, LocalDate start);
}
