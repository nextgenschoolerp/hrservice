package com.kvs.hr.repo;

import com.kvs.hr.entity.BankPayoutBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankPayoutBatchRepository extends JpaRepository<BankPayoutBatch, UUID> {
    Optional<BankPayoutBatch> findByBatchIdAndTenantId(UUID batchId, UUID tenantId);
    Optional<BankPayoutBatch> findByBatchReference(String batchReference);
    List<BankPayoutBatch> findByTenantIdOrderByDisbursementDateDesc(UUID tenantId);
}
