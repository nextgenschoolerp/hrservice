package com.kvs.hr.dto;

import com.kvs.hr.enums.TaxRegime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxDeclarationRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotBlank(message = "Financial year is required")
    private String financialYear;

    @NotNull(message = "Tax regime is required (OLD/NEW)")
    private TaxRegime taxRegime;

    private BigDecimal section80cAmount;
    private BigDecimal section80dAmount;
    private BigDecimal hraAmount;
    private BigDecimal otherDeductions;
}
