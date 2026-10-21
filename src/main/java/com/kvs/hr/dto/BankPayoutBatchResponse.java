package com.kvs.hr.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankPayoutBatchResponse {
    private UUID batchId;
    private UUID tenantId;
    private String batchReference;
    private Integer payMonth;
    private Integer payYear;
    private BigDecimal totalAmount;
    private Integer staffCount;
    private String bankName;
    private LocalDate disbursementDate;
    private String exportFormat;
    private String batchStatus;
}
