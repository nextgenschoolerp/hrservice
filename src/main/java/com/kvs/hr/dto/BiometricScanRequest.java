package com.kvs.hr.dto;

import com.kvs.hr.enums.ScanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricScanRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotBlank(message = "Terminal device ID is required")
    private String terminalDeviceId;

    @NotNull(message = "Scan timestamp is required")
    private Instant scanTimestamp;

    @NotNull(message = "Scan type is required (IN/OUT)")
    private ScanType scanType;
}
