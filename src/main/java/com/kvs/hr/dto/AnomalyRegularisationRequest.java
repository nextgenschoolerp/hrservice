package com.kvs.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyRegularisationRequest {

    @NotBlank(message = "Regularisation reason is required")
    private String regularisationReason;

    private LocalTime correctedInTime;
    private LocalTime correctedOutTime;
}
