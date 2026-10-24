package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.ScanType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff_biometric_logs", indexes = {
        @Index(name = "idx_biometric_staff_time", columnList = "staff_id, scan_timestamp"),
        @Index(name = "idx_biometric_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffBiometricLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id", updatable = false, nullable = false)
    private UUID logId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "terminal_device_id", nullable = false, length = 50)
    private String terminalDeviceId;

    @Column(name = "scan_timestamp", nullable = false)
    private Instant scanTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", nullable = false, length = 10)
    private ScanType scanType;
}
