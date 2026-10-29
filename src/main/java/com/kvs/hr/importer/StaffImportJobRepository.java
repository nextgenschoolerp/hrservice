package com.kvs.hr.importer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaffImportJobRepository extends JpaRepository<StaffImportJob, UUID> {
    List<StaffImportJob> findTop50ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
