package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffShiftRepository extends JpaRepository<StaffShift, UUID> {
    Optional<StaffShift> findByTenantIdAndShiftCode(UUID tenantId, String shiftCode);
    List<StaffShift> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
