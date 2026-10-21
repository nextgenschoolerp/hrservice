package com.kvs.hr.dto;

import com.kvs.hr.enums.ScanType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricScanResponse {
    private UUID logId;
    private UUID staffId;
    private UUID tenantId;
    private String terminalDeviceId;
    private Instant scanTimestamp;
    private ScanType scanType;
}
