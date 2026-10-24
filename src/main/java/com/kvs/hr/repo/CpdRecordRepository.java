package com.kvs.hr.repo;

import com.kvs.hr.entity.CpdRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CpdRecordRepository extends JpaRepository<CpdRecord, UUID> {
    Optional<CpdRecord> findByCpdIdAndTenantId(UUID cpdId, String tenantId);
    List<CpdRecord> findByStaffIdOrderByCompletionDateDesc(UUID staffId);
    List<CpdRecord> findByTenantId(String tenantId);
}
