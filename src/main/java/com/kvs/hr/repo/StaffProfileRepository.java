package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {
    Optional<StaffProfile> findByTenantIdAndEmployeeCode(UUID tenantId, String employeeCode);
    Optional<StaffProfile> findByStaffIdAndTenantId(UUID staffId, UUID tenantId);
    Optional<StaffProfile> findFirstByTenantIdAndUserId(UUID tenantId, String userId);
    List<StaffProfile> findByTenantId(UUID tenantId);
    List<StaffProfile> findByTenantIdAndDepartment(UUID tenantId, String department);
    List<StaffProfile> findByTenantIdAndBranchId(UUID tenantId, String branchId);
    List<StaffProfile> findByTenantIdAndBranchIdAndDepartment(UUID tenantId, String branchId, String department);
    boolean existsByTenantIdAndEmployeeCode(UUID tenantId, String employeeCode);
}
