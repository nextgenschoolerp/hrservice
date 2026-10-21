package com.kvs.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankPayoutBatchRequest {

    @NotNull(message = "Pay month is required")
    @Min(1)
    @Max(12)
    private Integer payMonth;

    @NotNull(message = "Pay year is required")
    private Integer payYear;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotNull(message = "Disbursement date is required")
    private LocalDate disbursementDate;
}
