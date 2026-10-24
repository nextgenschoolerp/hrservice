package com.kvs.hr.repo;

import com.kvs.hr.entity.StaffArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffArchiveRepository extends JpaRepository<StaffArchive, UUID> {
    Optional<StaffArchive> findByArchiveIdAndTenantId(UUID archiveId, String tenantId);
    Optional<StaffArchive> findByStaffId(UUID staffId);
    List<StaffArchive> findByTenantId(String tenantId);
}
