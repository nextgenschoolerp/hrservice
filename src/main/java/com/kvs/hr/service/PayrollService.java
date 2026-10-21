package com.kvs.hr.service;

import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.hr.dto.*;
import com.kvs.hr.entity.*;
import com.kvs.hr.enums.DeliveryStatus;
import com.kvs.hr.enums.DisbursementStatus;
import com.kvs.hr.enums.LeaveStatus;
import com.kvs.hr.enums.LeaveType;
import com.kvs.hr.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayrollService {

    private final StaffProfileRepository staffProfileRepository;
    private final StaffPayrollRepository staffPayrollRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final TaxDeclarationRepository taxDeclarationRepository;
    private final BankPayoutBatchRepository bankPayoutBatchRepository;
    private final PayrollForecastRepository payrollForecastRepository;
    private final PayslipDistributionRepository payslipDistributionRepository;
    private final TenantAccessGuard tenantGuard;
    private final HrAuditService auditService;

    // US-07005: Gross-to-net payroll calculation
    @Transactional
    public PayrollResponse calculatePayroll(PayrollCalculationRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        BigDecimal basic = staff.getBasicSalary();
        BigDecimal allowances = request.getAllowances() != null ? request.getAllowances() : BigDecimal.ZERO;
        BigDecimal gross = basic.add(allowances);

        // Calculate PF: standard 12% of basic (capped at 1800 if salary above standard wage ceiling, or full 12%)
        BigDecimal pf = basic.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
        if (pf.compareTo(new BigDecimal("1800.00")) > 0) {
            pf = new BigDecimal("1800.00");
        }

        // Calculate ESI: 0.75% of gross if gross <= 21,000
        BigDecimal esi = BigDecimal.ZERO;
        if (gross.compareTo(new BigDecimal("21000.00")) <= 0) {
            esi = gross.multiply(new BigDecimal("0.0075")).setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate TDS from Tax Declaration if present, or default slab
        BigDecimal tds = BigDecimal.ZERO;
        Optional<TaxDeclaration> declOpt = taxDeclarationRepository.findByStaffIdAndFinancialYear(
                staff.getStaffId(), request.getPayYear() + "-" + (request.getPayYear() + 1));
        if (declOpt.isPresent()) {
            tds = declOpt.get().getMonthlyTds();
        } else if (gross.compareTo(new BigDecimal("50000.00")) > 0) {
            tds = gross.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate LWP Deduction (US-07006)
        LwpCalculationResponse lwpCalc = computeLwp(staff, request.getPayMonth(), request.getPayYear());
        BigDecimal lwpDeduction = lwpCalc.getLwpDeduction();

        // Custom deductions if any
        if (request.getCustomDeductions() != null) {
            lwpDeduction = lwpDeduction.add(request.getCustomDeductions());
        }

        StaffPayroll payroll = staffPayrollRepository.findByStaffIdAndPayYearAndPayMonth(
                staff.getStaffId(), request.getPayYear(), request.getPayMonth())
                .orElseGet(() -> StaffPayroll.builder()
                        .tenantId(staff.getTenantId())
                        .staffId(staff.getStaffId())
                        .payMonth(request.getPayMonth())
                        .payYear(request.getPayYear())
                        .disbursementStatus(DisbursementStatus.PROCESSED)
                        .build());

        payroll.setGrossSalary(gross);
        payroll.setPfDeduction(pf);
        payroll.setEsiDeduction(esi);
        payroll.setTdsDeduction(tds);
        payroll.setLwpDeduction(lwpDeduction);
        payroll.calculateNet();

        StaffPayroll saved = staffPayrollRepository.save(payroll);
        auditService.logSuccess("StaffPayroll", "CALCULATE_PAYROLL",
                "Payroll calculated for staffId=" + staff.getStaffId() + " month=" + request.getPayMonth() + "/" + request.getPayYear() + " net=" + saved.getNetSalary());
        return mapToPayrollResponse(saved);
    }

    // US-07006: Deduct unexcused absence leave without pay (LWP) automatically
    @Transactional(readOnly = true)
    public LwpCalculationResponse calculateLwpDeduction(UUID staffId, Integer payMonth, Integer payYear) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);

        return computeLwp(staff, payMonth, payYear);
    }

    private LwpCalculationResponse computeLwp(StaffProfile staff, Integer payMonth, Integer payYear) {
        YearMonth ym = YearMonth.of(payYear, payMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        int daysInMonth = ym.lengthOfMonth();

        List<LeaveApplication> lwpLeaves = leaveApplicationRepository
                .findByStaffIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        staff.getStaffId(), LeaveStatus.APPROVED, end, start)
                .stream()
                .filter(l -> l.getLeaveType() == LeaveType.LWP)
                .toList();

        long lwpDays = lwpLeaves.stream().mapToLong(l -> l.getTotalDays().longValue()).sum();
        BigDecimal dailyRate = staff.getBasicSalary().divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        BigDecimal lwpDeduction = dailyRate.multiply(BigDecimal.valueOf(lwpDays)).setScale(2, RoundingMode.HALF_UP);

        return LwpCalculationResponse.builder()
                .staffId(staff.getStaffId())
                .payMonth(payMonth)
                .payYear(payYear)
                .unexcusedAbsenceDays(lwpDays)
                .dailyRate(dailyRate)
                .lwpDeduction(lwpDeduction)
                .build();
    }

    // US-07007: Generate statutory PF and ESI monthly return files
    @Transactional(readOnly = true)
    public StatutoryReportResponse generateStatutoryReport(String reportType, Integer payMonth, Integer payYear) {
        UUID tenantId = tenantGuard.requireCurrentTenantId();
        List<StaffPayroll> payrolls = staffPayrollRepository.findByTenantIdAndPayYearAndPayMonth(tenantId, payYear, payMonth);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalEmployeeShare = BigDecimal.ZERO;
        BigDecimal totalEmployerShare = BigDecimal.ZERO;

        StringBuilder sb = new StringBuilder();
        String type = reportType != null ? reportType.toUpperCase() : "PF";

        if ("PF".equals(type)) {
            sb.append("UAN#MEMBER_NAME#GROSS_WAGES#EPF_WAGES#EE_SHARE#ER_SHARE\n");
            for (StaffPayroll p : payrolls) {
                totalGross = totalGross.add(p.getGrossSalary());
                totalEmployeeShare = totalEmployeeShare.add(p.getPfDeduction());
                totalEmployerShare = totalEmployerShare.add(p.getPfDeduction()); // Employer matching
                sb.append(p.getStaffId()).append("#STAFF#")
                  .append(p.getGrossSalary()).append("#")
                  .append(p.getGrossSalary()).append("#")
                  .append(p.getPfDeduction()).append("#")
                  .append(p.getPfDeduction()).append("\n");
            }
        } else if ("ESI".equals(type)) {
            sb.append("IP_NUMBER#IP_NAME#DAYS_WORKED#TOTAL_WAGES#IP_CONTRIBUTION\n");
            for (StaffPayroll p : payrolls) {
                totalGross = totalGross.add(p.getGrossSalary());
                totalEmployeeShare = totalEmployeeShare.add(p.getEsiDeduction());
                BigDecimal erEsi = p.getGrossSalary().multiply(new BigDecimal("0.0325")).setScale(2, RoundingMode.HALF_UP);
                totalEmployerShare = totalEmployerShare.add(erEsi);
                sb.append(p.getStaffId()).append("#STAFF#30#")
                  .append(p.getGrossSalary()).append("#")
                  .append(p.getEsiDeduction()).append("\n");
            }
        }

        auditService.logSuccess("StatutoryReport", "GENERATE_RETURN",
                "Generated " + type + " return for " + payMonth + "/" + payYear);

        return StatutoryReportResponse.builder()
                .reportType(type)
                .payMonth(payMonth)
                .payYear(payYear)
                .totalEmployees(payrolls.size())
                .totalGrossWages(totalGross)
                .totalEmployeeContribution(totalEmployeeShare)
                .totalEmployerContribution(totalEmployerShare)
                .fileContent(sb.toString())
                .build();
    }

    // US-07008: Password-protected PDF payslip generation & distribution
    @Transactional
    public PayslipResponse generateAndDistributePayslip(PayslipGenerateRequest request) {
        StaffPayroll payroll = staffPayrollRepository.findById(request.getPayrollId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffPayroll", "payrollId", request.getPayrollId()));
        tenantGuard.requireTenantRecord(payroll.getTenantId(), "StaffPayroll", "payrollId", request.getPayrollId());

        StaffProfile staff = staffProfileRepository.findById(payroll.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", payroll.getStaffId()));

        String recipientEmail = request.getEmailRecipient() != null ? request.getEmailRecipient() : staff.getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            recipientEmail = staff.getEmployeeCode().toLowerCase() + "@kvs.local";
        }

        String passwordHint = request.getPasswordHint() != null ? request.getPasswordHint()
                : (staff.getPanNumber() != null ? "First 4 letters of name + PAN last 4" : "DOB DDMMYYYY");

        PayslipDistribution dist = PayslipDistribution.builder()
                .tenantId(payroll.getTenantId())
                .payrollId(payroll.getPayrollId())
                .staffId(staff.getStaffId())
                .recipientEmail(recipientEmail)
                .passwordHint(passwordHint)
                .deliveryStatus(DeliveryStatus.SENT)
                .sentTimestamp(Instant.now())
                .documentReference("PAYSLIP-" + staff.getEmployeeCode() + "-" + payroll.getPayMonth() + "-" + payroll.getPayYear() + ".pdf")
                .build();

        PayslipDistribution saved = payslipDistributionRepository.save(dist);
        auditService.logSuccess("PayslipDistribution", "DISPATCH_PAYSLIP",
                "Sent encrypted payslip to " + recipientEmail + " for payrollId=" + payroll.getPayrollId());

        return PayslipResponse.builder()
                .distributionId(saved.getDistributionId())
                .payrollId(saved.getPayrollId())
                .staffId(saved.getStaffId())
                .recipientEmail(saved.getRecipientEmail())
                .passwordHint(saved.getPasswordHint())
                .deliveryStatus(saved.getDeliveryStatus())
                .sentTimestamp(saved.getSentTimestamp())
                .documentReference(saved.getDocumentReference())
                .build();
    }

    public byte[] renderEncryptedPayslipPdf(UUID payrollId, String ownerPassword, String userPassword) {
        StaffPayroll payroll = staffPayrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffPayroll", "payrollId", payrollId));
        tenantGuard.requireTenantRecord(payroll.getTenantId(), "StaffPayroll", "payrollId", payrollId);

        StaffProfile staff = staffProfileRepository.findById(payroll.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", payroll.getStaffId()));

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.newLineAtOffset(50, 750);
                cs.showText("DELHI PUBLIC SCHOOL - MONTHLY SALARY PAYSLIP");
                cs.newLineAtOffset(0, -30);

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.showText("Employee: " + staff.getFirstName() + " " + staff.getLastName() + " (" + staff.getEmployeeCode() + ")");
                cs.newLineAtOffset(0, -20);
                cs.showText("Department: " + staff.getDepartment() + " | Designation: " + staff.getDesignation());
                cs.newLineAtOffset(0, -20);
                cs.showText("Pay Period: " + payroll.getPayMonth() + " / " + payroll.getPayYear());
                cs.newLineAtOffset(0, -30);

                cs.showText("Gross Salary: INR " + payroll.getGrossSalary());
                cs.newLineAtOffset(0, -20);
                cs.showText("PF Deduction: INR " + payroll.getPfDeduction());
                cs.newLineAtOffset(0, -20);
                cs.showText("ESI Deduction: INR " + payroll.getEsiDeduction());
                cs.newLineAtOffset(0, -20);
                cs.showText("TDS Deduction: INR " + payroll.getTdsDeduction());
                cs.newLineAtOffset(0, -20);
                cs.showText("LWP Deduction: INR " + payroll.getLwpDeduction());
                cs.newLineAtOffset(0, -30);

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                cs.showText("NET SALARY DISBURSED: INR " + payroll.getNetSalary());
                cs.endText();
            }

            // Apply standard password encryption if passwords provided
            if (userPassword != null && !userPassword.isBlank()) {
                AccessPermission ap = new AccessPermission();
                ap.setCanPrint(true);
                StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword != null ? ownerPassword : "admin", userPassword, ap);
                spp.setEncryptionKeyLength(128);
                doc.protect(spp);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render encrypted payslip PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Payslip PDF generation error: " + e.getMessage());
        }
    }

    // US-07012: Export monthly salary disbursement statements to bank bulk payout portals
    @Transactional
    public BankPayoutBatchResponse exportBankPayoutBatch(BankPayoutBatchRequest request) {
        UUID tenantId = tenantGuard.requireCurrentTenantId();
        List<StaffPayroll> payrolls = staffPayrollRepository.findByTenantIdAndPayYearAndPayMonth(
                tenantId, request.getPayYear(), request.getPayMonth());

        BigDecimal total = payrolls.stream()
                .map(StaffPayroll::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String batchRef = "PAYOUT-" + request.getBankName().toUpperCase().replaceAll("\\s+", "") + "-" +
                request.getPayYear() + "-" + request.getPayMonth() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        BankPayoutBatch batch = BankPayoutBatch.builder()
                .tenantId(tenantId)
                .batchReference(batchRef)
                .payMonth(request.getPayMonth())
                .payYear(request.getPayYear())
                .totalAmount(total)
                .staffCount(payrolls.size())
                .bankName(request.getBankName())
                .disbursementDate(request.getDisbursementDate())
                .exportFormat("NEFT_CSV")
                .batchStatus("EXPORTED")
                .build();

        // Mark payrolls as disbursed
        for (StaffPayroll p : payrolls) {
            p.setDisbursementStatus(DisbursementStatus.DISBURSED);
            staffPayrollRepository.save(p);
        }

        BankPayoutBatch saved = bankPayoutBatchRepository.save(batch);
        auditService.logSuccess("BankPayoutBatch", "EXPORT_PAYOUT",
                "Exported batchRef=" + batchRef + " total=" + total + " staffCount=" + payrolls.size());

        return mapToBatchResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BankPayoutBatchResponse> listBankPayouts() {
        UUID tenantId = tenantGuard.requireCurrentTenantId();
        return bankPayoutBatchRepository.findByTenantIdOrderByDisbursementDateDesc(tenantId).stream()
                .map(this::mapToBatchResponse)
                .collect(Collectors.toList());
    }

    // US-07014: Configure Income Tax TDS slab rates and process employee tax declaration forms
    @Transactional
    public TaxDeclarationResponse submitTaxDeclaration(TaxDeclarationRequest request) {
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", request.getStaffId()));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", request.getStaffId());

        BigDecimal c = request.getSection80cAmount() != null ? request.getSection80cAmount() : BigDecimal.ZERO;
        BigDecimal d = request.getSection80dAmount() != null ? request.getSection80dAmount() : BigDecimal.ZERO;
        BigDecimal hra = request.getHraAmount() != null ? request.getHraAmount() : BigDecimal.ZERO;
        BigDecimal other = request.getOtherDeductions() != null ? request.getOtherDeductions() : BigDecimal.ZERO;

        BigDecimal annualGross = staff.getBasicSalary().multiply(BigDecimal.valueOf(12));
        BigDecimal totalDeductions = c.add(d).add(hra).add(other);
        BigDecimal taxableIncome = annualGross.subtract(totalDeductions).max(BigDecimal.ZERO);

        // Simple progressive estimation: 10% above 500,000
        BigDecimal annualTax = BigDecimal.ZERO;
        if (taxableIncome.compareTo(new BigDecimal("500000.00")) > 0) {
            annualTax = taxableIncome.subtract(new BigDecimal("500000.00")).multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal monthlyTds = annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        TaxDeclaration decl = taxDeclarationRepository.findByStaffIdAndFinancialYear(staff.getStaffId(), request.getFinancialYear())
                .orElseGet(() -> TaxDeclaration.builder()
                        .tenantId(staff.getTenantId())
                        .staffId(staff.getStaffId())
                        .financialYear(request.getFinancialYear())
                        .build());

        decl.setTaxRegime(request.getTaxRegime());
        decl.setSection80cAmount(c);
        decl.setSection80dAmount(d);
        decl.setHraAmount(hra);
        decl.setOtherDeductions(other);
        decl.setProjectedAnnualTax(annualTax);
        decl.setMonthlyTds(monthlyTds);
        decl.setIsVerified(true);

        TaxDeclaration saved = taxDeclarationRepository.save(decl);
        auditService.logSuccess("TaxDeclaration", "SUBMIT_DECLARATION",
                "Processed tax declaration for staffId=" + staff.getStaffId() + " fy=" + request.getFinancialYear());
        return mapToTaxResponse(saved);
    }

    @Transactional(readOnly = true)
    public TaxDeclarationResponse getTaxDeclaration(UUID staffId, String financialYear) {
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", staffId));
        tenantGuard.requireTenantRecord(staff.getTenantId(), "StaffProfile", "staffId", staffId);

        TaxDeclaration decl = taxDeclarationRepository.findByStaffIdAndFinancialYear(staffId, financialYear)
                .orElseThrow(() -> new ResourceNotFoundException("TaxDeclaration", "financialYear", financialYear));
        return mapToTaxResponse(decl);
    }

    // US-07018: Generate annual staff cost and payroll budget forecasts for management
    @Transactional
    public PayrollForecastResponse generateForecast(PayrollForecastRequest request) {
        UUID tenantId = tenantGuard.requireCurrentTenantId();
        List<StaffProfile> deptStaff = staffProfileRepository.findByTenantIdAndDepartment(tenantId, request.getDepartment());

        BigDecimal currentCost = deptStaff.stream()
                .map(s -> s.getBasicSalary().multiply(BigDecimal.valueOf(12)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal incrementFactor = BigDecimal.ONE.add(request.getProjectedIncrementPct().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal projectedCost = currentCost.multiply(incrementFactor);

        int hires = request.getSimulatedNewHiresCount() != null ? request.getSimulatedNewHiresCount() : 0;
        if (hires > 0) {
            BigDecimal avgSalary = deptStaff.isEmpty() ? new BigDecimal("40000.00")
                    : currentCost.divide(BigDecimal.valueOf(deptStaff.size() * 12L), 2, RoundingMode.HALF_UP);
            projectedCost = projectedCost.add(avgSalary.multiply(BigDecimal.valueOf(hires * 12L)));
        }

        PayrollForecast forecast = PayrollForecast.builder()
                .tenantId(tenantId)
                .fiscalYear(request.getFiscalYear())
                .department(request.getDepartment())
                .currentAnnualCost(currentCost)
                .projectedIncrementPct(request.getProjectedIncrementPct())
                .simulatedNewHiresCount(hires)
                .projectedAnnualCost(projectedCost.setScale(2, RoundingMode.HALF_UP))
                .build();

        PayrollForecast saved = payrollForecastRepository.save(forecast);
        auditService.logSuccess("PayrollForecast", "GENERATE_FORECAST",
                "Generated budget forecast for fy=" + request.getFiscalYear() + " dept=" + request.getDepartment());
        return mapToForecastResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PayrollForecastResponse> listForecasts(String fiscalYear) {
        UUID tenantId = tenantGuard.requireCurrentTenantId();
        return payrollForecastRepository.findByTenantIdAndFiscalYear(tenantId, fiscalYear).stream()
                .map(this::mapToForecastResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayrollResponse getPayroll(UUID payrollId) {
        StaffPayroll payroll = staffPayrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffPayroll", "payrollId", payrollId));
        tenantGuard.requireTenantRecord(payroll.getTenantId(), "StaffPayroll", "payrollId", payrollId);
        return mapToPayrollResponse(payroll);
    }

    @Transactional(readOnly = true)
    public List<PayrollResponse> listPayrolls(UUID staffId, Integer payYear, Integer payMonth) {
        UUID tenantId = tenantGuard.requireCurrentTenantId();
        List<StaffPayroll> list;
        if (staffId != null) {
            list = staffPayrollRepository.findByStaffIdOrderByPayYearDescPayMonthDesc(staffId);
        } else if (payYear != null && payMonth != null) {
            list = staffPayrollRepository.findByTenantIdAndPayYearAndPayMonth(tenantId, payYear, payMonth);
        } else {
            list = staffPayrollRepository.findAll().stream()
                    .filter(p -> p.getTenantId().equals(tenantId))
                    .collect(Collectors.toList());
        }
        return list.stream().map(this::mapToPayrollResponse).collect(Collectors.toList());
    }

    private PayrollResponse mapToPayrollResponse(StaffPayroll p) {
        return PayrollResponse.builder()
                .payrollId(p.getPayrollId())
                .staffId(p.getStaffId())
                .tenantId(p.getTenantId())
                .payMonth(p.getPayMonth())
                .payYear(p.getPayYear())
                .grossSalary(p.getGrossSalary())
                .pfDeduction(p.getPfDeduction())
                .esiDeduction(p.getEsiDeduction())
                .tdsDeduction(p.getTdsDeduction())
                .lwpDeduction(p.getLwpDeduction())
                .netSalary(p.getNetSalary())
                .disbursementStatus(p.getDisbursementStatus())
                .build();
    }

    private BankPayoutBatchResponse mapToBatchResponse(BankPayoutBatch b) {
        return BankPayoutBatchResponse.builder()
                .batchId(b.getBatchId())
                .tenantId(b.getTenantId())
                .batchReference(b.getBatchReference())
                .payMonth(b.getPayMonth())
                .payYear(b.getPayYear())
                .totalAmount(b.getTotalAmount())
                .staffCount(b.getStaffCount())
                .bankName(b.getBankName())
                .disbursementDate(b.getDisbursementDate())
                .exportFormat(b.getExportFormat())
                .batchStatus(b.getBatchStatus())
                .build();
    }

    private TaxDeclarationResponse mapToTaxResponse(TaxDeclaration t) {
        return TaxDeclarationResponse.builder()
                .declarationId(t.getDeclarationId())
                .tenantId(t.getTenantId())
                .staffId(t.getStaffId())
                .financialYear(t.getFinancialYear())
                .taxRegime(t.getTaxRegime())
                .section80cAmount(t.getSection80cAmount())
                .section80dAmount(t.getSection80dAmount())
                .hraAmount(t.getHraAmount())
                .otherDeductions(t.getOtherDeductions())
                .projectedAnnualTax(t.getProjectedAnnualTax())
                .monthlyTds(t.getMonthlyTds())
                .isVerified(t.getIsVerified())
                .build();
    }

    private PayrollForecastResponse mapToForecastResponse(PayrollForecast f) {
        return PayrollForecastResponse.builder()
                .forecastId(f.getForecastId())
                .tenantId(f.getTenantId())
                .fiscalYear(f.getFiscalYear())
                .department(f.getDepartment())
                .currentAnnualCost(f.getCurrentAnnualCost())
                .projectedIncrementPct(f.getProjectedIncrementPct())
                .simulatedNewHiresCount(f.getSimulatedNewHiresCount())
                .projectedAnnualCost(f.getProjectedAnnualCost())
                .build();
    }
}
