package com.kvs.hr.repo;

import com.kvs.hr.entity.LeaveQuota;
import com.kvs.hr.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveQuotaRepository extends JpaRepository<LeaveQuota, UUID> {
    Optional<LeaveQuota> findByStaffIdAndQuotaYearAndLeaveType(UUID staffId, Integer quotaYear, LeaveType leaveType);
    List<LeaveQuota> findByStaffIdAndQuotaYear(UUID staffId, Integer quotaYear);
    List<LeaveQuota> findByTenantIdAndQuotaYear(String tenantId, Integer quotaYear);
}
