package com.kvs.hr.repo;

import com.kvs.hr.entity.BiometricAnomaly;
import com.kvs.hr.enums.RegularisationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BiometricAnomalyRepository extends JpaRepository<BiometricAnomaly, UUID> {
    Optional<BiometricAnomaly> findByAnomalyIdAndTenantId(UUID anomalyId, String tenantId);
    List<BiometricAnomaly> findByTenantIdOrderByAnomalyDateDesc(String tenantId);
    List<BiometricAnomaly> findByStaffIdOrderByAnomalyDateDesc(UUID staffId);
    List<BiometricAnomaly> findByTenantIdAndStatus(String tenantId, RegularisationStatus status);
    List<BiometricAnomaly> findByStaffIdAndAnomalyDate(UUID staffId, LocalDate anomalyDate);
}
