package com.kvs.hr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class TenantAccessGuard {

    @Autowired
    private ObjectMapper objectMapper;

    public boolean isSuperAdmin() {
        return SecurityUtils.getCurrentUserRoles().contains("ROLE_SUPER_ADMIN");
    }

    public Optional<String> currentTenantId() {
        return claim("tenantId");
    }

    public Optional<String> currentUserId() {
        return claim("userId");
    }

    /** True when the caller holds any of the given roles (codes without the ROLE_ prefix). */
    public boolean hasAnyRole(String... roles) {
        java.util.List<String> held = SecurityUtils.getCurrentUserRoles();
        for (String role : roles) {
            if (held.contains("ROLE_" + role) || held.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAccessTenant(String tenantId) {
        if (isSuperAdmin()) {
            return true;
        }
        if (tenantId == null) {
            return false;
        }
        return currentTenantId()
                .map(t -> {
                    if (t.equalsIgnoreCase(tenantId)) {
                        return true;
                    }
                    try {
                        UUID parsed = UUID.fromString(t);
                        return parsed.toString().equalsIgnoreCase(tenantId);
                    } catch (IllegalArgumentException e) {
                        UUID hashed = UUID.nameUUIDFromBytes(t.getBytes(StandardCharsets.UTF_8));
                        return hashed.toString().equalsIgnoreCase(tenantId);
                    }
                })
                .orElse(false);
    }

    public boolean canAccessTenant(UUID tenantId) {
        if (isSuperAdmin()) {
            return true;
        }
        if (tenantId == null) {
            return false;
        }
        return canAccessTenant(tenantId.toString());
    }

    public void requireTenantRecord(UUID tenantId, String resourceName, String fieldName, Object fieldValue) {
        if (!canAccessTenant(tenantId)) {
            throw new ResourceNotFoundException(resourceName, fieldName, fieldValue);
        }
    }

    public UUID requireCurrentTenantId() {
        String tenantStr = currentTenantId()
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "currentTenant", "missing in JWT claims"));
        try {
            return UUID.fromString(tenantStr);
        } catch (IllegalArgumentException e) {
            // For testing or custom tenants formatted as strings (e.g. T-ACAD-...), create a deterministic UUID
            return UUID.nameUUIDFromBytes(tenantStr.getBytes(StandardCharsets.UTF_8));
        }
    }

    private Optional<String> claim(String name) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return Optional.empty();
            }
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Optional.empty();
            }
            String token = auth.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return Optional.empty();
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode tree = objectMapper.readTree(payload);
            JsonNode node = tree.get(name);
            return node == null || node.isNull() ? Optional.empty() : Optional.of(node.asText());
        } catch (Exception e) {
            log.debug("Could not extract claim '{}' from request: {}", name, e.getMessage());
            return Optional.empty();
        }
    }
}
