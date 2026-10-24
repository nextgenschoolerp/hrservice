package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {
    Optional<StaffProfile> findByTenantIdAndEmployeeCode(String tenantId, String employeeCode);
    Optional<StaffProfile> findByStaffIdAndTenantId(UUID staffId, String tenantId);
    Optional<StaffProfile> findFirstByTenantIdAndUserId(String tenantId, String userId);
    List<StaffProfile> findByTenantId(String tenantId);
    List<StaffProfile> findByTenantIdAndDepartment(String tenantId, String department);
    List<StaffProfile> findByTenantIdAndBranchId(String tenantId, String branchId);
    List<StaffProfile> findByTenantIdAndBranchIdAndDepartment(String tenantId, String branchId, String department);
    boolean existsByTenantIdAndEmployeeCode(String tenantId, String employeeCode);
}
