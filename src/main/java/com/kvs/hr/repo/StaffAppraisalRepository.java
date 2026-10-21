package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffAppraisal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffAppraisalRepository extends JpaRepository<StaffAppraisal, UUID> {
    Optional<StaffAppraisal> findByAppraisalIdAndTenantId(UUID appraisalId, UUID tenantId);
    List<StaffAppraisal> findByStaffIdOrderByReviewDateDesc(UUID staffId);
    List<StaffAppraisal> findByTenantId(UUID tenantId);
}
