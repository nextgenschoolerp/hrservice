package com.kvs.hr.dto;

import com.kvs.hr.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipGenerateRequest {

    @NotNull(message = "Payroll ID is required")
    private UUID payrollId;

    private String emailRecipient;
    private String passwordHint;
}
