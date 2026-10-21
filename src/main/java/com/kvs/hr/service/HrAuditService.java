package com.kvs.hr.service;

import com.kvs.core.util.SecurityUtils;
import com.kvs.hr.entity.AuditLog;
import com.kvs.hr.repo.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Service
@Slf4j
public class HrAuditService {

    private static final int MAX_DETAILS_LENGTH = 1000;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    public void log(String resource, String action, String outcome, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(SecurityUtils.getCurrentUserEmail().orElse("system"));
            auditLog.setIpAddress(resolveIpAddress());
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setOutcome(outcome);
            auditLog.setEventTime(Instant.now());
            auditLog.setDetails(details != null && details.length() > MAX_DETAILS_LENGTH
                    ? details.substring(0, MAX_DETAILS_LENGTH)
                    : details);
            TransactionTemplate own = new TransactionTemplate(transactionManager);
            own.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            own.executeWithoutResult(status -> auditLogRepository.save(auditLog));
        } catch (Exception e) {
            log.warn("Failed to write audit log for action {}: {}", action, e.getMessage());
        }
    }

    public void logSuccess(String resource, String action, String details) {
        log(resource, action, "SUCCESS", details);
    }

    private String resolveIpAddress() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            String xForwarded = request.getHeader("X-Forwarded-For");
            if (xForwarded != null && !xForwarded.isBlank()) {
                return xForwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
        }
        return "127.0.0.1";
    }
}
