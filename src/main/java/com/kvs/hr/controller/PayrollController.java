package com.kvs.hr.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.hr.config.HrRoles;
import com.kvs.hr.dto.*;
import com.kvs.hr.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hr/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    // US-07005: Gross-to-net payroll calculation
    @PostMapping("/calculate")
    @PreAuthorize(HrRoles.PAYROLL_CALCULATE)
    public ResponseEntity<ApiResponse<PayrollResponse>> calculatePayroll(
            @Valid @RequestBody PayrollCalculationRequest request) {
        PayrollResponse data = payrollService.calculatePayroll(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<PayrollResponse>builder()
                        .success(true)
                        .message("Payroll calculated successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/{payrollId}")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<PayrollResponse>> getPayroll(@PathVariable UUID payrollId) {
        PayrollResponse data = payrollService.getPayroll(payrollId);
        return ResponseEntity.ok(ApiResponse.<PayrollResponse>builder()
                .success(true)
                .message("Payroll record retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> listPayrolls(
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) Integer payYear,
            @RequestParam(required = false) Integer payMonth) {
        List<PayrollResponse> data = payrollService.listPayrolls(staffId, payYear, payMonth);
        return ResponseEntity.ok(ApiResponse.<List<PayrollResponse>>builder()
                .success(true)
                .message("Payroll records retrieved successfully")
                .data(data)
                .build());
    }

    // US-07006: Deduct unexcused absence leave without pay (LWP) automatically from monthly pay
    @GetMapping("/lwp-deduction/{staffId}")
    @PreAuthorize(HrRoles.LWP_DEDUCTION)
    public ResponseEntity<ApiResponse<LwpCalculationResponse>> calculateLwpDeduction(
            @PathVariable UUID staffId,
            @RequestParam Integer payMonth,
            @RequestParam Integer payYear) {
        LwpCalculationResponse data = payrollService.calculateLwpDeduction(staffId, payMonth, payYear);
        return ResponseEntity.ok(ApiResponse.<LwpCalculationResponse>builder()
                .success(true)
                .message("LWP absence deduction calculated successfully")
                .data(data)
                .build());
    }

    // US-07007: Generate statutory PF and ESI monthly return files
    @GetMapping("/statutory-returns")
    @PreAuthorize(HrRoles.STATUTORY_REPORTS)
    public ResponseEntity<ApiResponse<StatutoryReportResponse>> generateStatutoryReturns(
            @RequestParam(defaultValue = "PF") String reportType,
            @RequestParam Integer payMonth,
            @RequestParam Integer payYear) {
        StatutoryReportResponse data = payrollService.generateStatutoryReport(reportType, payMonth, payYear);
        return ResponseEntity.ok(ApiResponse.<StatutoryReportResponse>builder()
                .success(true)
                .message("Statutory " + reportType.toUpperCase() + " return report generated successfully")
                .data(data)
                .build());
    }

    // US-07008: Password-protected encrypted PDF payslip generation & distribution
    @PostMapping("/payslips/dispatch")
    @PreAuthorize(HrRoles.PAYSLIP_DISTRIBUTE)
    public ResponseEntity<ApiResponse<PayslipResponse>> dispatchPayslip(
            @Valid @RequestBody PayslipGenerateRequest request) {
        PayslipResponse data = payrollService.generateAndDistributePayslip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<PayslipResponse>builder()
                        .success(true)
                        .message("Encrypted PDF payslip generated and dispatched successfully")
                        .data(data)
                        .build());
    }

    @GetMapping(value = "/payslips/{payrollId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<byte[]> downloadPayslipPdf(
            @PathVariable UUID payrollId,
            @RequestParam(required = false) String password) {
        byte[] pdfBytes = payrollService.renderEncryptedPayslipPdf(payrollId, "admin", password);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip-" + payrollId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // US-07012: Export monthly salary disbursement statements to bank bulk payout portals
    @PostMapping("/bank-payouts")
    @PreAuthorize(HrRoles.BANK_DISBURSEMENT)
    public ResponseEntity<ApiResponse<BankPayoutBatchResponse>> exportBankPayout(
            @Valid @RequestBody BankPayoutBatchRequest request) {
        BankPayoutBatchResponse data = payrollService.exportBankPayoutBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BankPayoutBatchResponse>builder()
                        .success(true)
                        .message("Bank payout batch statement exported successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/bank-payouts")
    @PreAuthorize(HrRoles.BANK_DISBURSEMENT)
    public ResponseEntity<ApiResponse<List<BankPayoutBatchResponse>>> listBankPayouts() {
        List<BankPayoutBatchResponse> data = payrollService.listBankPayouts();
        return ResponseEntity.ok(ApiResponse.<List<BankPayoutBatchResponse>>builder()
                .success(true)
                .message("Bank payout batches retrieved successfully")
                .data(data)
                .build());
    }

    // US-07014: Configure Income Tax TDS slab rates and process employee tax declarations
    @PostMapping("/tax-declarations")
    @PreAuthorize(HrRoles.TAX_DECLARATION)
    public ResponseEntity<ApiResponse<TaxDeclarationResponse>> submitTaxDeclaration(
            @Valid @RequestBody TaxDeclarationRequest request) {
        TaxDeclarationResponse data = payrollService.submitTaxDeclaration(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<TaxDeclarationResponse>builder()
                        .success(true)
                        .message("Tax declaration processed and TDS calculated successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/tax-declarations/{staffId}")
    @PreAuthorize(HrRoles.STAFF_VIEW)
    public ResponseEntity<ApiResponse<TaxDeclarationResponse>> getTaxDeclaration(
            @PathVariable UUID staffId,
            @RequestParam String financialYear) {
        TaxDeclarationResponse data = payrollService.getTaxDeclaration(staffId, financialYear);
        return ResponseEntity.ok(ApiResponse.<TaxDeclarationResponse>builder()
                .success(true)
                .message("Tax declaration retrieved successfully")
                .data(data)
                .build());
    }

    // US-07018: Generate annual staff cost and payroll budget forecasts for management
    @PostMapping("/forecasts")
    @PreAuthorize(HrRoles.PAYROLL_FORECAST)
    public ResponseEntity<ApiResponse<PayrollForecastResponse>> generateForecast(
            @Valid @RequestBody PayrollForecastRequest request) {
        PayrollForecastResponse data = payrollService.generateForecast(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<PayrollForecastResponse>builder()
                        .success(true)
                        .message("Annual payroll budget forecast generated successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/forecasts")
    @PreAuthorize(HrRoles.PAYROLL_FORECAST)
    public ResponseEntity<ApiResponse<List<PayrollForecastResponse>>> listForecasts(
            @RequestParam String fiscalYear) {
        List<PayrollForecastResponse> data = payrollService.listForecasts(fiscalYear);
        return ResponseEntity.ok(ApiResponse.<List<PayrollForecastResponse>>builder()
                .success(true)
                .message("Payroll budget forecasts retrieved successfully")
                .data(data)
                .build());
    }
}
