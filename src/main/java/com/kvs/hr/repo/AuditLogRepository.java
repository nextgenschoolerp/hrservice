package com.kvs.hr.repo;

import com.kvs.hr.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop50ByOrderByEventTimeDesc();
    List<AuditLog> findByUserIdOrderByEventTimeDesc(String userId);
}
