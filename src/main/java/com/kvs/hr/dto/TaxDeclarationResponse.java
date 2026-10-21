package com.kvs.hr.dto;

import com.kvs.hr.enums.TaxRegime;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxDeclarationResponse {
    private UUID declarationId;
    private UUID tenantId;
    private UUID staffId;
    private String financialYear;
    private TaxRegime taxRegime;
    private BigDecimal section80cAmount;
    private BigDecimal section80dAmount;
    private BigDecimal hraAmount;
    private BigDecimal otherDeductions;
    private BigDecimal projectedAnnualTax;
    private BigDecimal monthlyTds;
    private Boolean isVerified;
}
