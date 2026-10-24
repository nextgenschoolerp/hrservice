package com.kvs.hr.repo;

import com.kvs.hr.entity.PayslipDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayslipDistributionRepository extends JpaRepository<PayslipDistribution, UUID> {
    Optional<PayslipDistribution> findByDistributionIdAndTenantId(UUID distributionId, String tenantId);
    Optional<PayslipDistribution> findByPayrollId(UUID payrollId);
    List<PayslipDistribution> findByStaffIdOrderBySentTimestampDesc(UUID staffId);
    List<PayslipDistribution> findByTenantId(String tenantId);
}
