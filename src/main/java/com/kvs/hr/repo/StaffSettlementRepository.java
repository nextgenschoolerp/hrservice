package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffSettlementRepository extends JpaRepository<StaffSettlement, UUID> {
    Optional<StaffSettlement> findBySettlementIdAndTenantId(UUID settlementId, String tenantId);
    Optional<StaffSettlement> findByStaffId(UUID staffId);
    List<StaffSettlement> findByTenantId(String tenantId);
}
