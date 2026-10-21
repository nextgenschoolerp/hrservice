package com.kvs.hr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kvs.hr.dto.*;
import com.kvs.hr.enums.TaxRegime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayrollControllerIntegrationTest extends HrTestSupport {

    @Test
    @DisplayName("US-07005, US-07006, US-07007, US-07008, US-07012, US-07014, US-07018: Comprehensive Payroll Lifecycle")
    void testFullPayrollLifecycle() throws Exception {
        String accountantToken = token("PAYROLL_ACCOUNTANT");
        String hrToken = token("HR_MANAGER");
        String principalToken = token("SCHOOL_PRINCIPAL");
        String teacherToken = token("FACULTY_MEMBER");

        // US-07014: Submit tax declaration for staffRajesh
        TaxDeclarationRequest taxReq = TaxDeclarationRequest.builder()
                .staffId(staffRajesh.getStaffId())
                .financialYear("2024-2025")
                .taxRegime(TaxRegime.OLD)
                .section80cAmount(new BigDecimal("150000.00"))
                .section80dAmount(new BigDecimal("25000.00"))
                .hraAmount(new BigDecimal("120000.00"))
                .build();
        JsonNode taxNode = data(postJson("/payroll/tax-declarations", accountantToken, taxReq).andExpect(status().isCreated()));
        assertThat(taxNode.get("isVerified").asBoolean()).isTrue();

        // US-07005: Calculate monthly payroll (with PF, ESI, TDS)
        PayrollCalculationRequest payReq = PayrollCalculationRequest.builder()
                .staffId(staffRajesh.getStaffId())
                .payMonth(5)
                .payYear(2024)
                .allowances(new BigDecimal("5000.00"))
                .build();

        JsonNode payNode = data(postJson("/payroll/calculate", accountantToken, payReq).andExpect(status().isCreated()));
        UUID payrollId = UUID.fromString(payNode.get("payrollId").asText());
        assertThat(payNode.get("grossSalary").asDouble()).isEqualTo(60000.0); // 55000 + 5000
        assertThat(payNode.get("pfDeduction").asDouble()).isEqualTo(1800.0);  // capped PF
        assertThat(payNode.get("netSalary").asDouble()).isGreaterThan(0.0);

        // US-07006: Check LWP deduction calculation
        JsonNode lwpNode = data(getAs("/payroll/lwp-deduction/" + staffRajesh.getStaffId(), accountantToken,
                "payMonth", "5", "payYear", "2024").andExpect(status().isOk()));
        assertThat(lwpNode.get("unexcusedAbsenceDays").asLong()).isEqualTo(0L);

        // US-07007: Generate statutory PF & ESI reports
        JsonNode pfReport = data(getAs("/payroll/statutory-returns", accountantToken,
                "reportType", "PF", "payMonth", "5", "payYear", "2024").andExpect(status().isOk()));
        assertThat(pfReport.get("reportType").asText()).isEqualTo("PF");
        assertThat(pfReport.get("fileContent").asText()).contains("UAN#MEMBER_NAME");

        JsonNode esiReport = data(getAs("/payroll/statutory-returns", accountantToken,
                "reportType", "ESI", "payMonth", "5", "payYear", "2024").andExpect(status().isOk()));
        assertThat(esiReport.get("reportType").asText()).isEqualTo("ESI");

        // US-07008: Dispatch encrypted payslip & test PDF generation
        PayslipGenerateRequest psReq = PayslipGenerateRequest.builder()
                .payrollId(payrollId)
                .passwordHint("DOB 15081985")
                .build();
        JsonNode psNode = data(postJson("/payroll/payslips/dispatch", hrToken, psReq).andExpect(status().isCreated()));
        assertThat(psNode.get("deliveryStatus").asText()).isEqualTo("SENT");

        // Download generated encrypted PDF
        getAs("/payroll/payslips/" + payrollId + "/pdf", teacherToken, "password", "Rajesh123")
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // US-07012: Export bank bulk payout batch
        BankPayoutBatchRequest bankReq = BankPayoutBatchRequest.builder()
                .payMonth(5)
                .payYear(2024)
                .bankName("State Bank of India")
                .disbursementDate(LocalDate.of(2024, 5, 31))
                .build();
        JsonNode batchNode = data(postJson("/payroll/bank-payouts", accountantToken, bankReq).andExpect(status().isCreated()));
        assertThat(batchNode.get("batchReference").asText()).contains("PAYOUT-STATEBANKOFINDIA-");

        // US-07018: Annual payroll budget forecasting
        PayrollForecastRequest forecastReq = PayrollForecastRequest.builder()
                .fiscalYear("2025-2026")
                .department("Academics")
                .projectedIncrementPct(new BigDecimal("8.5"))
                .simulatedNewHiresCount(3)
                .build();
        JsonNode forecastNode = data(postJson("/payroll/forecasts", principalToken, forecastReq).andExpect(status().isCreated()));
        assertThat(forecastNode.get("projectedAnnualCost").asDouble()).isGreaterThan(forecastNode.get("currentAnnualCost").asDouble());
    }
}
