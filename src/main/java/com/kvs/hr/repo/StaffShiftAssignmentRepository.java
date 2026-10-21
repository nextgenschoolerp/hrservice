package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffShiftAssignmentRepository extends JpaRepository<StaffShiftAssignment, UUID> {
    Optional<StaffShiftAssignment> findTopByStaffIdOrderByEffectiveFromDesc(UUID staffId);
    List<StaffShiftAssignment> findByShiftId(UUID shiftId);
}
