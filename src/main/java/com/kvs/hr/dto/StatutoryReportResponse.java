package com.kvs.hr.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatutoryReportResponse {
    private String reportType;
    private Integer payMonth;
    private Integer payYear;
    private Integer totalEmployees;
    private BigDecimal totalGrossWages;
    private BigDecimal totalEmployeeContribution;
    private BigDecimal totalEmployerContribution;
    private String fileContent;
}
