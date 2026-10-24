package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffBiometricLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface StaffBiometricLogRepository extends JpaRepository<StaffBiometricLog, UUID> {
    List<StaffBiometricLog> findByStaffIdOrderByScanTimestampDesc(UUID staffId);
    List<StaffBiometricLog> findByTenantIdAndScanTimestampBetween(String tenantId, Instant start, Instant end);
    List<StaffBiometricLog> findByStaffIdAndScanTimestampBetween(UUID staffId, Instant start, Instant end);
}
