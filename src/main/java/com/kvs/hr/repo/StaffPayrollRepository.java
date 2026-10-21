package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffPayroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffPayrollRepository extends JpaRepository<StaffPayroll, UUID> {
    Optional<StaffPayroll> findByStaffIdAndPayYearAndPayMonth(UUID staffId, Integer payYear, Integer payMonth);
    Optional<StaffPayroll> findByPayrollIdAndTenantId(UUID payrollId, UUID tenantId);
    List<StaffPayroll> findByTenantIdAndPayYearAndPayMonth(UUID tenantId, Integer payYear, Integer payMonth);
    List<StaffPayroll> findByStaffIdOrderByPayYearDescPayMonthDesc(UUID staffId);
}
