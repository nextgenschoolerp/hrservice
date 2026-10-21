package com.kvs.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentClearanceRequest {

    @NotBlank(message = "Department type is required (LIBRARY/ACCOUNTS/IT/LAB)")
    private String department;

    @NotNull(message = "Cleared flag is required")
    private Boolean cleared;
}
