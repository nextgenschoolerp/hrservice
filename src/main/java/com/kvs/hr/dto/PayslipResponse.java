package com.kvs.hr.dto;

import com.kvs.hr.enums.DeliveryStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipResponse {
    private UUID distributionId;
    private UUID payrollId;
    private UUID staffId;
    private String recipientEmail;
    private String passwordHint;
    private DeliveryStatus deliveryStatus;
    private Instant sentTimestamp;
    private String documentReference;
}
