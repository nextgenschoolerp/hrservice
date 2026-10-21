package com.kvs.hr.dto;

import com.kvs.hr.enums.DisbursementStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollResponse {
    private UUID payrollId;
    private UUID staffId;
    private UUID tenantId;
    private Integer payMonth;
    private Integer payYear;
    private BigDecimal grossSalary;
    private BigDecimal pfDeduction;
    private BigDecimal esiDeduction;
    private BigDecimal tdsDeduction;
    private BigDecimal lwpDeduction;
    private BigDecimal netSalary;
    private DisbursementStatus disbursementStatus;
}
