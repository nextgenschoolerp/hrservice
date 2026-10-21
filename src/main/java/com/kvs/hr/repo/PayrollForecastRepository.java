package com.kvs.hr.repo;

import com.kvs.hr.entity.PayrollForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollForecastRepository extends JpaRepository<PayrollForecast, UUID> {
    Optional<PayrollForecast> findByForecastIdAndTenantId(UUID forecastId, UUID tenantId);
    List<PayrollForecast> findByTenantIdAndFiscalYear(UUID tenantId, String fiscalYear);
}
