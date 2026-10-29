package com.kvs.hr.importer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.hr.dto.StaffProfileRequest;
import com.kvs.hr.dto.StaffProfileResponse;
import com.kvs.hr.entity.StaffProfile;
import com.kvs.hr.importer.ImportJobResponse.RowError;
import com.kvs.hr.importer.ImportJobResponse.RowResult;
import com.kvs.hr.importer.Spreadsheet.Column;
import com.kvs.hr.repo.StaffProfileRepository;
import com.kvs.hr.service.EmployeeService;
import com.kvs.hr.service.HrAuditService;
import com.kvs.hr.service.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bulk staff import (Settings → Data import): template, check (nothing saved), import, history and error report.
 * Each new staff member goes through {@link EmployeeService#createEmployee}, exactly as when added on the Staff page.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StaffImportService {

    static final String VALID = "VALID", ERROR = "ERROR", CREATED = "CREATED", UPDATED = "UPDATED", UNCHANGED = "UNCHANGED", FAILED = "FAILED";
    static final String CREATE = "CREATE", UPDATE = "UPDATE", PHOTOS = "PHOTOS";
    /** Roles a sign-in account may be created with from an import; authservice has the final say and refuses privileged roles. */
    static final List<String> ACCOUNT_ROLES = List.of("Class Teacher", "Subject Teacher", "Department Head", "Timetable In-charge", "Substitution In-charge", "Academic Coordinator",
            "Exam Controller", "E-learning Admin", "House Master", "Office Clerk", "Admin Clerk", "Receptionist", "Front Office Counselor", "Admission Officer",
            "HR Manager", "Payroll Accountant", "Accounts Clerk", "Finance Cashier", "Finance Officer", "Compliance Officer", "Marketing Manager",
            "Public Relations Officer", "School Nurse", "Security Guard", "Alumni Coordinator");
    private static final Duration EXPIRY = Duration.ofHours(24);
    private static final List<String> DEPARTMENTS = List.of("Academics", "Accounts", "Administration", "Admissions", "Computer Science",
            "English", "Examinations", "Hindi", "IT", "Leadership", "Library", "Mathematics", "Physical Education", "Science",
            "Social Science", "Student Wellbeing", "Transport");

    private final StaffProfileRepository staffRepository;
    private final StaffImportJobRepository jobRepository;
    private final EmployeeService employeeService;
    private final HrAuditService auditService;
    private final TenantAccessGuard tenantGuard;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    static String mode(String mode) {
        String m = mode == null || mode.isBlank() ? CREATE : mode.trim().toUpperCase(Locale.ROOT);
        if (!CREATE.equals(m) && !UPDATE.equals(m)) throw new BadRequestException("Unknown import type " + mode + ": use CREATE or UPDATE", "IMPORT_MODE");
        return m;
    }

    /** CREATE: the template columns. UPDATE: the same, none mandatory except the Employee code that says whom to update (blank keeps the current value). */
    List<Column> columns(String tenantId, String mode) {
        List<Column> base = baseColumns(tenantId);
        if (!UPDATE.equals(mode)) return base;
        return base.stream().map(c -> c.key().equals("employeeCode")
                ? new Column(c.header(), c.key(), true, "The employee code of the staff member to update (Staff list and profile)", null)
                : new Column(c.header(), c.key(), false, c.help() + ". Leave blank to keep the current value", c.choices())).toList();
    }

    /** The columns of the template, in order; the department dropdown adds the school's own departments. */
    private List<Column> baseColumns(String tenantId) {
        List<String> departments = Stream.concat(DEPARTMENTS.stream(),
                        staffRepository.findByTenantId(tenantId).stream().map(StaffProfile::getDepartment).filter(Objects::nonNull))
                .distinct().sorted().toList();
        return List.of(
                new Column("Employee code", "employeeCode", true, "The school's staff number, unique, up to 50 characters", null),
                new Column("First name", "firstName", true, "Given name, up to 100 characters", null),
                new Column("Last name", "lastName", true, "Family name, up to 100 characters", null),
                new Column("Department", "department", true, "Department, e.g. Mathematics or Administration (up to 50 characters)", departments),
                new Column("Designation", "designation", true, "Job title, e.g. TGT Mathematics or Office Clerk (up to 50 characters)", null),
                new Column("Joining date", "joiningDate", true, "DD/MM/YYYY", null),
                new Column("Basic salary", "basicSalary", true, "Monthly basic pay in rupees, e.g. 45000", null),
                new Column("Email", "email", true, "Official email address, unique", null),
                new Column("Mobile", "phone", true, "10-digit Indian mobile number, with or without +91", null),
                new Column("Probation end date", "probationEndDate", false, "DD/MM/YYYY; 6 months after joining when blank", null),
                new Column("Qualifications", "qualifications", false, "e.g. M.Sc. Mathematics, B.Ed.", null),
                new Column("PAN", "panNumber", false, "10 characters, e.g. ABCDE1234F", null),
                new Column("Aadhaar", "aadhaarNumber", false, "12 digits", null),
                new Column("PF UAN", "pfUan", false, "12 digits", null),
                new Column("ESI number", "esiNumber", false, "10 or 17 digits", null),
                new Column("Bank name", "bankName", false, "e.g. State Bank of India", null),
                new Column("Bank account number", "bankAccountNumber", false, "9 to 18 digits", null),
                new Column("Bank IFSC", "bankIfsc", false, "11 characters, e.g. SBIN0001234", null),
                new Column("System role", "systemRole", false, "Only when creating a sign-in account: what the person may open, e.g. Subject Teacher", ACCOUNT_ROLES));
    }

    /** The template; for UPDATE with prefill it already holds the campus's current staff, to correct and upload again. */
    public byte[] template(String branchId, String format, String mode, boolean prefill) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        String m = mode(mode);
        boolean update = UPDATE.equals(m);
        List<Column> cols = columns(tenantId, m);
        List<List<String>> current = update && prefill ? currentStaff(tenantId, branchId, cols) : List.of();
        if ("csv".equalsIgnoreCase(format)) return Spreadsheet.csv(cols.stream().map(Column::label).toList(), current);
        Map<String, String> ex = new HashMap<>(Map.ofEntries(Map.entry("employeeCode", "DPS-EMP-2001"), Map.entry("firstName", "Meera"), Map.entry("lastName", "Iyer"),
                Map.entry("department", "Mathematics"), Map.entry("designation", "TGT Mathematics"), Map.entry("joiningDate", "01/07/2026"), Map.entry("basicSalary", "45000"),
                Map.entry("email", "meera.iyer@dps.edu.in"), Map.entry("phone", "9811200101"), Map.entry("probationEndDate", "31/12/2026"),
                Map.entry("qualifications", "M.Sc. Mathematics, B.Ed."), Map.entry("panNumber", "ABCDE1234F"), Map.entry("aadhaarNumber", "234567890123"),
                Map.entry("pfUan", "100200300400"), Map.entry("esiNumber", "1234567890"), Map.entry("bankName", "State Bank of India"),
                Map.entry("bankAccountNumber", "30012345678"), Map.entry("bankIfsc", "SBIN0001234"), Map.entry("systemRole", "Subject Teacher")));
        if (update) { // an update example changes just two things; everything else stays blank
            ex.replaceAll((k, v) -> k.equals("employeeCode") || k.equals("designation") || k.equals("phone") ? v : "");
        }
        List<String> example = cols.stream().map(c -> ex.getOrDefault(c.key(), "")).toList();
        List<String> notes = update ? List.of(
                "Staff update template",
                "1. Each row updates ONE existing staff member, found by the Employee code in the first column. Rows are numbered from row 2.",
                "2. Fill only the cells you want to change. A blank cell keeps the current value; it never clears it.",
                "3. Easiest: use 'Download current staff' in Settings → Data import. The sheet then holds today's values; change what you need and upload it.",
                "4. Dates: DD/MM/YYYY. Mobile numbers: 10 digits; +91 is added automatically.",
                "5. System role: only for staff who have no sign-in account yet, and only if you tick 'Create sign-in accounts' when importing.",
                "6. At most " + Spreadsheet.MAX_ROWS + " staff per file. Check the file first; nothing is saved until you choose Import.")
            : List.of(
                "Staff bulk import template",
                "1. Fill one staff member per row on the Staff sheet, starting on row 2. Do not rename or remove the header row.",
                "2. Columns marked * (shaded yellow) are mandatory. Use the dropdown for Department, or type the school's own.",
                "3. Dates: DD/MM/YYYY. Mobile numbers: 10 digits; +91 is added automatically. PAN, Aadhaar and bank details are optional.",
                "4. System role is needed only if you tick 'Create sign-in accounts' when importing; it decides what the person can open.",
                "5. Save as .xlsx (or CSV, UTF-8) and upload it in Settings → Data import. Check the file first; nothing is saved until you choose Import.",
                "6. At most " + Spreadsheet.MAX_ROWS + " staff per file. Staff are added to the campus selected in the app, on probation.");
        return Spreadsheet.template("Staff", cols, example, notes, current);
    }

    /** The campus's active staff in template layout, by employee code. */
    private List<List<String>> currentStaff(String tenantId, String branchId, List<Column> cols) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return staffRepository.findByTenantId(tenantId).stream()
                .filter(s -> branchId != null && branchId.equals(s.getBranchId()) && !Boolean.FALSE.equals(s.getIsActive()))
                .sorted(Comparator.comparing(StaffProfile::getEmployeeCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(Spreadsheet.MAX_ROWS).map(s -> {
                    Map<String, String> v = new HashMap<>();
                    v.put("employeeCode", nz(s.getEmployeeCode())); v.put("firstName", nz(s.getFirstName())); v.put("lastName", nz(s.getLastName()));
                    v.put("department", nz(s.getDepartment())); v.put("designation", nz(s.getDesignation()));
                    v.put("joiningDate", s.getJoiningDate() == null ? "" : s.getJoiningDate().format(fmt));
                    v.put("basicSalary", s.getBasicSalary() == null ? "" : s.getBasicSalary().stripTrailingZeros().toPlainString());
                    v.put("email", nz(s.getEmail())); v.put("phone", nz(s.getPhone()));
                    v.put("probationEndDate", s.getProbationEndDate() == null ? "" : s.getProbationEndDate().format(fmt));
                    v.put("qualifications", nz(s.getQualifications())); v.put("panNumber", nz(s.getPanNumber())); v.put("aadhaarNumber", nz(s.getAadhaarNumber()));
                    v.put("pfUan", nz(s.getPfUan())); v.put("esiNumber", nz(s.getEsiNumber())); v.put("bankName", nz(s.getBankName()));
                    v.put("bankAccountNumber", nz(s.getBankAccountNumber())); v.put("bankIfsc", nz(s.getBankIfsc()));
                    return cols.stream().map(c -> v.getOrDefault(c.key(), "")).toList();
                }).toList();
    }

    public ImportJobResponse check(String branchId, MultipartFile file, String mode) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        if (branchId == null || branchId.isBlank()) throw new BadRequestException("Choose a campus before importing", "BRANCH_REQUIRED");
        String m = mode(mode);
        List<Column> cols = columns(tenantId, m);
        List<Spreadsheet.Row> sheet = Spreadsheet.read(file, cols);
        List<RowResult> results = UPDATE.equals(m) ? validateUpdate(tenantId, branchId, sheet, cols) : validate(tenantId, sheet, cols);
        int errors = (int) results.stream().filter(r -> ERROR.equals(r.getStatus())).count();
        int ready = (int) results.stream().filter(r -> VALID.equals(r.getStatus())).count();
        StaffImportJob job = jobRepository.save(StaffImportJob.builder()
                .tenantId(tenantId).branchId(branchId).fileName(file.getOriginalFilename()).status("VALIDATED").mode(m)
                .totalRows(results.size()).validRows(ready).errorRows(errors)
                .rowsJson(write(results)).build());
        auditService.logSuccess("StaffImportJob", "CHECK_STAFF_IMPORT",
                "Checked " + results.size() + " rows from " + file.getOriginalFilename() + ": " + errors + " with errors");
        return toResponse(job, results);
    }

    public ImportJobResponse commit(UUID jobId, String mode) {
        StaffImportJob job = find(jobId);
        if (!"VALIDATED".equals(job.getStatus())) throw new BadRequestException("This upload has already been imported", "IMPORT_ALREADY_DONE");
        if (job.getCreatedAt() != null && job.getCreatedAt().plus(EXPIRY).isBefore(Instant.now())) {
            job.setStatus("EXPIRED");
            jobRepository.save(job);
            throw new BadRequestException("This check is older than 24 hours. Upload the file again.", "IMPORT_EXPIRED");
        }
        boolean allOrNothing = "ALL_OR_NOTHING".equalsIgnoreCase(mode);
        boolean update = UPDATE.equals(job.getMode());
        List<Column> cols = columns(job.getTenantId(), job.getMode());
        List<Spreadsheet.Row> rows = read(job.getRowsJson()).stream().map(r -> new Spreadsheet.Row(r.getRowNumber(), r.getValues())).toList();
        List<RowResult> results = update ? validateUpdate(job.getTenantId(), job.getBranchId(), rows, cols) : validate(job.getTenantId(), rows, cols);
        long errors = results.stream().filter(r -> ERROR.equals(r.getStatus())).count();
        java.util.function.BiConsumer<StaffImportJob, RowResult> apply = update ? this::update : this::create;
        if (allOrNothing && errors > 0) {
            throw new BadRequestException(errors + " row" + (errors == 1 ? " has" : "s have") + " errors. Fix them and check the file again, or import only the valid rows.", "IMPORT_HAS_ERRORS");
        }
        if (results.stream().noneMatch(r -> VALID.equals(r.getStatus()))) throw new BadRequestException(update ? "No row has a change to save" : "No row is ready to import", "IMPORT_NOTHING_VALID");

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        if (allOrNothing) {
            tx.executeWithoutResult(s -> results.stream().filter(r -> VALID.equals(r.getStatus())).forEach(r -> apply.accept(job, r)));
        } else {
            for (RowResult r : results) {
                if (!VALID.equals(r.getStatus())) continue;
                try {
                    tx.executeWithoutResult(s -> apply.accept(job, r));
                } catch (RuntimeException e) {
                    log.warn("Staff import row {} failed: {}", r.getRowNumber(), e.getMessage());
                    r.setStatus(FAILED);
                    r.setRecordId(null);
                    r.setRecordCode(null);
                    r.getErrors().add(new RowError(null, "Could not be saved: " + e.getMessage()));
                }
            }
        }
        int done = (int) results.stream().filter(r -> CREATED.equals(r.getStatus()) || UPDATED.equals(r.getStatus())).count();
        results.stream().filter(r -> CREATED.equals(r.getStatus()) || UPDATED.equals(r.getStatus())).forEach(r -> r.setValues(null));
        job.setStatus("COMMITTED");
        job.setCreatedCount(update ? 0 : done);
        job.setUpdatedCount(update ? done : 0);
        job.setErrorRows((int) results.stream().filter(r -> ERROR.equals(r.getStatus()) || FAILED.equals(r.getStatus())).count());
        job.setValidRows((int) results.stream().filter(r -> !ERROR.equals(r.getStatus()) && !UNCHANGED.equals(r.getStatus())).count());
        job.setCommittedAt(Instant.now());
        job.setCommittedBy(SecurityUtils.getAuthentication().map(Authentication::getName).orElse(null));
        job.setRowsJson(write(results));
        jobRepository.save(job);
        auditService.logSuccess("StaffImportJob", "COMMIT_STAFF_IMPORT", (update ? "Updated " : "Imported ") + done + " of " + results.size() + " staff from " + job.getFileName());
        return toResponse(job, results);
    }

    private void create(StaffImportJob job, RowResult r) {
        Map<String, String> v = r.getValues();
        StaffProfileRequest req = StaffProfileRequest.builder()
                .employeeCode(v.get("employeeCode")).firstName(v.get("firstName")).lastName(v.get("lastName"))
                .department(v.get("department")).designation(v.get("designation"))
                .basicSalary(ImportRules.amount(v.get("basicSalary"))).joiningDate(ImportRules.date(v.get("joiningDate")))
                .branchId(job.getBranchId()).email(v.get("email").toLowerCase(Locale.ROOT)).phone(ImportRules.mobile(v.get("phone")))
                .probationEndDate(ImportRules.date(v.get("probationEndDate"))).qualifications(v.get("qualifications"))
                .panNumber(upper(v.get("panNumber"))).aadhaarNumber(digits(v.get("aadhaarNumber"))).pfUan(digits(v.get("pfUan")))
                .esiNumber(digits(v.get("esiNumber"))).bankName(v.get("bankName")).bankAccountNumber(digits(v.get("bankAccountNumber")))
                .bankIfsc(upper(v.get("bankIfsc")))
                .build();
        StaffProfileResponse saved = employeeService.createEmployee(req);
        r.setStatus(CREATED);
        if (!isBlank(v.get("systemRole"))) { // the sign-in account step needs these after the row values are dropped
            r.getSummary().put("email", v.get("email").toLowerCase(Locale.ROOT));
            r.getSummary().put("systemRole", v.get("systemRole"));
            r.getSummary().put("firstName", v.get("firstName"));
            r.getSummary().put("lastName", v.get("lastName"));
        }
        r.setRecordId(String.valueOf(saved.getStaffId()));
        r.setRecordCode(saved.getEmployeeCode());
    }

    /** Applies one checked UPDATE row: only the cells that were filled change; blank cells keep what is on record. */
    private void update(StaffImportJob job, RowResult r) {
        Map<String, String> v = r.getValues();
        UUID id = UUID.fromString(r.getRecordId());
        StaffProfile s = staffRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "staffId", id));
        boolean profileChanges = !"Sign-in account".equals(r.getSummary().get("changes"));
        if (profileChanges) {
            // the same service as the Edit staff form; fields left null there keep their stored value
            employeeService.updateEmployee(id, StaffProfileRequest.builder()
                    .employeeCode(s.getEmployeeCode()).branchId(s.getBranchId())
                    .firstName(or(v.get("firstName"), s.getFirstName())).lastName(or(v.get("lastName"), s.getLastName()))
                    .department(or(v.get("department"), s.getDepartment())).designation(or(v.get("designation"), s.getDesignation()))
                    .basicSalary(has(v, "basicSalary") ? ImportRules.amount(v.get("basicSalary")) : s.getBasicSalary())
                    .joiningDate(has(v, "joiningDate") ? ImportRules.date(v.get("joiningDate")) : s.getJoiningDate())
                    .email(has(v, "email") ? v.get("email").toLowerCase(Locale.ROOT) : s.getEmail())
                    .phone(has(v, "phone") ? ImportRules.mobile(v.get("phone")) : s.getPhone())
                    .probationEndDate(has(v, "probationEndDate") ? ImportRules.date(v.get("probationEndDate")) : null)
                    .qualifications(or(v.get("qualifications"), s.getQualifications())).contractTerms(s.getContractTerms())
                    .panNumber(has(v, "panNumber") ? upper(v.get("panNumber")) : null).aadhaarNumber(has(v, "aadhaarNumber") ? digits(v.get("aadhaarNumber")) : null)
                    .pfUan(has(v, "pfUan") ? digits(v.get("pfUan")) : null).esiNumber(has(v, "esiNumber") ? digits(v.get("esiNumber")) : null)
                    .bankName(has(v, "bankName") ? v.get("bankName") : null).bankAccountNumber(has(v, "bankAccountNumber") ? digits(v.get("bankAccountNumber")) : null)
                    .bankIfsc(has(v, "bankIfsc") ? upper(v.get("bankIfsc")) : null)
                    .build());
        }
        auditService.logSuccess("StaffImportJob", "UPDATE_STAFF_IMPORT", "Updated " + s.getEmployeeCode() + " by bulk import from " + job.getFileName() + ": " + r.getSummary().get("changes"));
        r.setStatus(UPDATED);
    }

    /**
     * Photos from a ZIP, each file named after the employee code (DPS-EMP-2001.jpg). dryRun only reports what would happen;
     * otherwise the matched photos replace the current ones and the upload is kept in the history.
     */
    public ImportJobResponse photos(String branchId, MultipartFile zip, boolean dryRun) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        List<PhotoZip.Entry> entries = PhotoZip.read(zip);
        Map<String, StaffProfile> byCode = new HashMap<>();
        staffRepository.findByTenantId(tenantId).forEach(s -> { if (s.getEmployeeCode() != null) byCode.put(s.getEmployeeCode().toLowerCase(Locale.ROOT), s); });
        Map<String, Integer> seen = new HashMap<>();
        List<RowResult> results = new ArrayList<>();
        Map<Integer, PhotoZip.Entry> byRow = new HashMap<>();
        int n = 0;
        for (PhotoZip.Entry e : entries) {
            n++;
            List<RowError> errs = new ArrayList<>();
            StaffProfile s = byCode.get(e.key());
            if (!e.extension().matches("jpe?g|png")) errs.add(new RowError(null, "Only JPG or PNG photos are accepted"));
            else if (e.tooBig()) errs.add(new RowError(null, "The photo is larger than 5 MB"));
            else if (e.imageType() == null) errs.add(new RowError(null, "The file is not a valid JPEG or PNG image"));
            if (s == null) errs.add(new RowError(null, "No staff member has the employee code \"" + e.key().toUpperCase(Locale.ROOT) + "\"; name the file after the employee code"));
            Integer first = seen.putIfAbsent(e.key(), n);
            if (first != null) errs.add(new RowError(null, "Repeats the photo in row " + first));
            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("name", s == null ? "—" : (s.getFirstName() + " " + s.getLastName()).trim());
            summary.put("file", e.fileName());
            summary.put("department", s == null ? "" : nz(s.getDepartment()));
            RowResult row = RowResult.builder().rowNumber(n).status(errs.isEmpty() ? VALID : ERROR).summary(summary).errors(errs).build();
            if (s != null) { row.setRecordId(String.valueOf(s.getStaffId())); row.setRecordCode(s.getEmployeeCode()); }
            results.add(row);
            byRow.put(n, e);
        }
        if (!dryRun) {
            for (RowResult row : results) {
                if (!VALID.equals(row.getStatus())) continue;
                PhotoZip.Entry e = byRow.get(row.getRowNumber());
                try {
                    employeeService.uploadPhoto(UUID.fromString(row.getRecordId()), new BytesMultipartFile(e.fileName(), e.imageType(), e.bytes()));
                    row.setStatus(UPDATED);
                } catch (RuntimeException ex) {
                    log.warn("Staff photo {} failed: {}", e.fileName(), ex.getMessage());
                    row.setStatus(FAILED);
                    row.getErrors().add(new RowError(null, "Could not be saved: " + ex.getMessage()));
                }
            }
        }
        int saved = (int) results.stream().filter(r -> UPDATED.equals(r.getStatus())).count();
        int bad = (int) results.stream().filter(r -> ERROR.equals(r.getStatus()) || FAILED.equals(r.getStatus())).count();
        int ready = (int) results.stream().filter(r -> VALID.equals(r.getStatus())).count();
        if (dryRun) {
            return ImportJobResponse.builder().fileName(zip.getOriginalFilename()).branchId(branchId).status("VALIDATED").mode(PHOTOS)
                    .totalRows(results.size()).validRows(ready).errorRows(bad).rows(results).build();
        }
        StaffImportJob job = jobRepository.save(StaffImportJob.builder().tenantId(tenantId).branchId(branchId).fileName(zip.getOriginalFilename())
                .status("COMMITTED").mode(PHOTOS).totalRows(results.size()).validRows(saved).errorRows(bad).updatedCount(saved)
                .committedAt(Instant.now()).committedBy(SecurityUtils.getAuthentication().map(Authentication::getName).orElse(null)).rowsJson(write(results)).build());
        auditService.logSuccess("StaffImportJob", "IMPORT_STAFF_PHOTOS", "Saved " + saved + " of " + results.size() + " photos from " + zip.getOriginalFilename());
        return toResponse(job, results);
    }

    /** Links the sign-in accounts created for imported staff: sets the user id of staff who have none yet. Returns how many were linked. */
    @Transactional
    public int linkAccounts(Map<UUID, String> links) {
        String tenantId = tenantGuard.requireCurrentTenantId();
        int linked = 0;
        for (Map.Entry<UUID, String> l : links.entrySet()) {
            StaffProfile s = staffRepository.findById(l.getKey()).orElse(null);
            if (s == null || !tenantId.equals(s.getTenantId()) || s.getUserId() != null || l.getValue() == null || l.getValue().isBlank()) continue;
            s.setUserId(l.getValue().trim());
            staffRepository.save(s);
            linked++;
        }
        auditService.logSuccess("StaffImportJob", "LINK_STAFF_ACCOUNTS", "Linked " + linked + " sign-in accounts to staff");
        return linked;
    }

    public List<ImportJobResponse> history() {
        String tenantId = tenantGuard.requireCurrentTenantId();
        return jobRepository.findTop50ByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(j -> toResponse(j, null)).toList();
    }

    public ImportJobResponse get(UUID jobId) {
        StaffImportJob job = find(jobId);
        return toResponse(job, read(job.getRowsJson()));
    }

    public byte[] errorReport(UUID jobId) {
        StaffImportJob job = find(jobId);
        if (PHOTOS.equals(job.getMode())) {
            List<List<String>> lines = read(job.getRowsJson()).stream().filter(r -> ERROR.equals(r.getStatus()) || FAILED.equals(r.getStatus()))
                    .map(r -> List.of(String.valueOf(r.getRowNumber()), r.getSummary().getOrDefault("file", ""),
                            r.getErrors().stream().map(RowError::getMessage).collect(Collectors.joining("; ")))).toList();
            return Spreadsheet.csv(List.of("Row", "File", "Problems"), lines);
        }
        List<Column> cols = columns(job.getTenantId(), job.getMode());
        List<String> header = new ArrayList<>(List.of("Row"));
        cols.forEach(c -> header.add(c.label()));
        header.add("Errors");
        List<List<String>> lines = read(job.getRowsJson()).stream().filter(r -> ERROR.equals(r.getStatus()) || FAILED.equals(r.getStatus())).map(r -> {
            List<String> line = new ArrayList<>(List.of(String.valueOf(r.getRowNumber())));
            Map<String, String> v = Optional.ofNullable(r.getValues()).orElse(Map.of());
            cols.forEach(c -> line.add(v.getOrDefault(c.key(), "")));
            line.add(r.getErrors().stream().map(e -> (e.getColumn() == null ? "" : e.getColumn() + ": ") + e.getMessage()).collect(Collectors.joining("; ")));
            return line;
        }).toList();
        return Spreadsheet.csv(header, lines);
    }

    private StaffImportJob find(UUID jobId) {
        StaffImportJob job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("StaffImportJob", "jobId", jobId));
        tenantGuard.requireTenantRecord(job.getTenantId(), "StaffImportJob", "jobId", jobId);
        return job;
    }

    /* ---------- checks ---------- */

    private List<RowResult> validate(String tenantId, List<Spreadsheet.Row> rows, List<Column> cols) {
        Map<String, String> headers = cols.stream().collect(Collectors.toMap(Column::key, Column::header));
        List<StaffProfile> staff = staffRepository.findByTenantId(tenantId);
        Map<String, String> codes = new HashMap<>(), emails = new HashMap<>(), pans = new HashMap<>();
        staff.forEach(s -> {
            if (s.getEmployeeCode() != null) codes.put(s.getEmployeeCode().toLowerCase(Locale.ROOT), s.getEmployeeCode());
            if (s.getEmail() != null) emails.put(s.getEmail().toLowerCase(Locale.ROOT), s.getEmployeeCode());
            if (s.getPanNumber() != null) pans.put(s.getPanNumber().toUpperCase(Locale.ROOT), s.getEmployeeCode());
        });
        Map<String, Integer> codesInFile = new HashMap<>(), emailsInFile = new HashMap<>(), pansInFile = new HashMap<>();
        LocalDate today = LocalDate.now();

        List<RowResult> out = new ArrayList<>();
        for (Spreadsheet.Row row : rows) {
            Map<String, String> v = row.values();
            List<RowError> errs = new ArrayList<>();
            java.util.function.BiConsumer<String, String> err = (key, msg) -> errs.add(new RowError(headers.getOrDefault(key, key), msg));
            cols.stream().filter(Column::required).filter(c -> isBlank(v.get(c.key()))).forEach(c -> err.accept(c.key(), "is required"));

            LocalDate joining = checkFields(v, err);

            // duplicates: already on the staff list, or earlier in this file
            unique(v.get("employeeCode"), codes, codesInFile, row.rowNumber(), "employeeCode", "Employee code", err, false);
            unique(v.get("email"), emails, emailsInFile, row.rowNumber(), "email", "Email", err, false);
            unique(v.get("panNumber"), pans, pansInFile, row.rowNumber(), "panNumber", "PAN", err, true);

            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("name", (v.getOrDefault("firstName", "") + " " + v.getOrDefault("lastName", "")).trim());
            summary.put("code", v.getOrDefault("employeeCode", ""));
            summary.put("department", v.getOrDefault("department", ""));
            out.add(RowResult.builder().rowNumber(row.rowNumber()).status(errs.isEmpty() ? VALID : ERROR).summary(summary)
                    .errors(errs).values(new LinkedHashMap<>(v)).build());
        }
        return out;
    }

    /** The format checks shared by new and updated rows; only filled cells are checked. Returns the joining date, if valid. */
    private LocalDate checkFields(Map<String, String> v, java.util.function.BiConsumer<String, String> err) {
        LocalDate today = LocalDate.now();
        length(v, "employeeCode", 50, err);
        length(v, "firstName", 100, err);
        length(v, "lastName", 100, err);
        length(v, "department", 50, err);
        length(v, "designation", 50, err);
        LocalDate joining = null;
        if (!isBlank(v.get("joiningDate"))) {
            joining = ImportRules.date(v.get("joiningDate"));
            if (joining == null) err.accept("joiningDate", "is not a valid date; use DD/MM/YYYY, e.g. 01/07/2026");
            else if (joining.isAfter(today.plusDays(90))) err.accept("joiningDate", "is more than 90 days ahead");
            else if (joining.isBefore(LocalDate.of(1960, 1, 1))) err.accept("joiningDate", "is before 1960");
        }
        if (!isBlank(v.get("probationEndDate"))) {
            LocalDate p = ImportRules.date(v.get("probationEndDate"));
            if (p == null) err.accept("probationEndDate", "is not a valid date; use DD/MM/YYYY");
            else if (joining != null && p.isBefore(joining)) err.accept("probationEndDate", "is before the joining date");
        }
        if (!isBlank(v.get("basicSalary")) && ImportRules.amount(v.get("basicSalary")) == null) err.accept("basicSalary", "must be an amount above 0, e.g. 45000");
        if (!isBlank(v.get("email")) && !ImportRules.email(v.get("email"))) err.accept("email", "is not a valid email address");
        if (!isBlank(v.get("phone")) && ImportRules.mobile(v.get("phone")) == null) err.accept("phone", "enter a 10-digit Indian mobile number");
        if (!isBlank(v.get("panNumber")) && !ImportRules.pan(v.get("panNumber"))) err.accept("panNumber", "must look like ABCDE1234F");
        if (!isBlank(v.get("aadhaarNumber")) && !ImportRules.aadhaar(v.get("aadhaarNumber"))) err.accept("aadhaarNumber", "must be 12 digits");
        if (!isBlank(v.get("pfUan")) && !digits(v.get("pfUan")).matches("\\d{12}")) err.accept("pfUan", "must be 12 digits");
        if (!isBlank(v.get("esiNumber")) && !digits(v.get("esiNumber")).matches("\\d{10}|\\d{17}")) err.accept("esiNumber", "must be 10 or 17 digits");
        if (!isBlank(v.get("bankAccountNumber")) && !digits(v.get("bankAccountNumber")).matches("\\d{9,18}")) err.accept("bankAccountNumber", "must be 9 to 18 digits");
        if (!isBlank(v.get("bankIfsc")) && !ImportRules.ifsc(v.get("bankIfsc"))) err.accept("bankIfsc", "must look like SBIN0001234");
        if (!isBlank(v.get("systemRole")) && ACCOUNT_ROLES.stream().noneMatch(r -> sameRole(r, v.get("systemRole")))) err.accept("systemRole", "is not one of the roles in the list");
        return joining;
    }

    private static boolean sameRole(String a, String b) { return a.replaceAll("[^A-Za-z]", "").equalsIgnoreCase(b.replaceAll("[^A-Za-z]", "")); }

    /** Checks UPDATE rows: the staff member is found by employee code; filled cells are checked and compared with what is on record. */
    private List<RowResult> validateUpdate(String tenantId, String branchId, List<Spreadsheet.Row> rows, List<Column> cols) {
        Map<String, String> headers = cols.stream().collect(Collectors.toMap(Column::key, Column::header));
        Map<String, StaffProfile> byCode = new HashMap<>();
        Map<String, String> emails = new HashMap<>(), pans = new HashMap<>();
        staffRepository.findByTenantId(tenantId).forEach(s -> {
            if (s.getEmployeeCode() != null) byCode.put(s.getEmployeeCode().toLowerCase(Locale.ROOT), s);
            if (s.getEmail() != null) emails.put(s.getEmail().toLowerCase(Locale.ROOT), s.getEmployeeCode());
            if (s.getPanNumber() != null) pans.put(s.getPanNumber().toUpperCase(Locale.ROOT), s.getEmployeeCode());
        });
        Map<String, Integer> codesInFile = new HashMap<>(), emailsInFile = new HashMap<>(), pansInFile = new HashMap<>();

        List<RowResult> out = new ArrayList<>();
        for (Spreadsheet.Row row : rows) {
            Map<String, String> v = row.values();
            List<RowError> errs = new ArrayList<>();
            java.util.function.BiConsumer<String, String> err = (key, msg) -> errs.add(new RowError(headers.getOrDefault(key, key), msg));
            List<String> changes = new ArrayList<>();
            StaffProfile s = null;
            String code = v.get("employeeCode");
            if (isBlank(code)) err.accept("employeeCode", "is required");
            else {
                s = byCode.get(code.toLowerCase(Locale.ROOT));
                if (s == null) err.accept("employeeCode", "No staff member has the employee code " + code);
                else {
                    if (branchId != null && s.getBranchId() != null && !branchId.equals(s.getBranchId())) err.accept("employeeCode", code + " is on campus " + s.getBranchId() + "; choose that campus to update this staff member");
                    Integer first = codesInFile.putIfAbsent(code.toLowerCase(Locale.ROOT), row.rowNumber());
                    if (first != null) err.accept("employeeCode", "repeats row " + first + " of this file");
                }
            }
            LocalDate joining = checkFields(v, err);
            boolean wantsAccount = false;
            if (s != null) {
                changed(changes, "First name", v.get("firstName"), s.getFirstName());
                changed(changes, "Last name", v.get("lastName"), s.getLastName());
                changed(changes, "Department", v.get("department"), s.getDepartment());
                changed(changes, "Designation", v.get("designation"), s.getDesignation());
                if (joining != null && !joining.equals(s.getJoiningDate())) changes.add("Joining date");
                java.math.BigDecimal pay = isBlank(v.get("basicSalary")) ? null : ImportRules.amount(v.get("basicSalary"));
                if (pay != null && (s.getBasicSalary() == null || pay.compareTo(s.getBasicSalary()) != 0)) changes.add("Basic salary");
                changed(changes, "Email", v.get("email"), s.getEmail());
                String phone = isBlank(v.get("phone")) ? null : ImportRules.mobile(v.get("phone"));
                if (phone != null && !phone.equals(ImportRules.mobile(s.getPhone()))) changes.add("Mobile");
                LocalDate probation = isBlank(v.get("probationEndDate")) ? null : ImportRules.date(v.get("probationEndDate"));
                if (probation != null && !probation.equals(s.getProbationEndDate())) changes.add("Probation end date");
                if (probation != null && joining == null && s.getJoiningDate() != null && probation.isBefore(s.getJoiningDate())) err.accept("probationEndDate", "is before the joining date");
                changed(changes, "Qualifications", v.get("qualifications"), s.getQualifications());
                changedId(changes, "PAN", upper(v.get("panNumber")), s.getPanNumber());
                changedId(changes, "Aadhaar", digits(v.get("aadhaarNumber")), s.getAadhaarNumber());
                changedId(changes, "PF UAN", digits(v.get("pfUan")), s.getPfUan());
                changedId(changes, "ESI number", digits(v.get("esiNumber")), s.getEsiNumber());
                changed(changes, "Bank name", v.get("bankName"), s.getBankName());
                changedId(changes, "Bank account", digits(v.get("bankAccountNumber")), s.getBankAccountNumber());
                changedId(changes, "Bank IFSC", upper(v.get("bankIfsc")), s.getBankIfsc());
                // email and PAN must stay unique across the school
                if (!isBlank(v.get("email"))) {
                    String owner = emails.get(v.get("email").toLowerCase(Locale.ROOT));
                    if (owner != null && !owner.equalsIgnoreCase(s.getEmployeeCode())) err.accept("email", "is already used by " + owner);
                    Integer first = emailsInFile.putIfAbsent(v.get("email").toLowerCase(Locale.ROOT), row.rowNumber());
                    if (first != null) err.accept("email", "repeats row " + first + " of this file");
                }
                if (!isBlank(v.get("panNumber"))) {
                    String owner = pans.get(v.get("panNumber").toUpperCase(Locale.ROOT));
                    if (owner != null && !owner.equalsIgnoreCase(s.getEmployeeCode())) err.accept("panNumber", "is already used by " + owner);
                    Integer first = pansInFile.putIfAbsent(v.get("panNumber").toUpperCase(Locale.ROOT), row.rowNumber());
                    if (first != null) err.accept("panNumber", "repeats row " + first + " of this file");
                }
                // a System role asks for a sign-in account: only for staff who have none
                wantsAccount = !isBlank(v.get("systemRole")) && s.getUserId() == null;
                if (wantsAccount && changes.isEmpty()) changes.add("Sign-in account");
                else if (!isBlank(v.get("systemRole")) && s.getUserId() != null) err.accept("systemRole", s.getEmployeeCode() + " already has a sign-in account; leave System role blank");
            }
            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("name", s == null ? (code == null ? "" : code) : (or(v.get("firstName"), s.getFirstName()) + " " + or(v.get("lastName"), s.getLastName())).trim());
            summary.put("code", code == null ? "" : code);
            summary.put("department", s == null ? "" : or(v.get("department"), s.getDepartment()));
            summary.put("changes", changes.isEmpty() ? "Nothing to change" : String.join(", ", changes));
            if (wantsAccount) { summary.put("email", or(v.get("email"), s.getEmail())); summary.put("systemRole", v.get("systemRole")); summary.put("firstName", or(v.get("firstName"), s.getFirstName())); summary.put("lastName", or(v.get("lastName"), s.getLastName())); }
            RowResult r = RowResult.builder().rowNumber(row.rowNumber()).status(!errs.isEmpty() ? ERROR : changes.isEmpty() ? UNCHANGED : VALID)
                    .summary(summary).errors(errs).values(new LinkedHashMap<>(v)).build();
            if (s != null) { r.setRecordId(String.valueOf(s.getStaffId())); r.setRecordCode(s.getEmployeeCode()); }
            out.add(r);
        }
        return out;
    }

    /** Adds the label when the cell is filled and differs from the value on record (ignoring case and extra spaces). */
    private static void changed(List<String> changes, String label, String given, String current) {
        if (isBlank(given)) return;
        String a = given.trim().replaceAll("\\s+", " ");
        String b = current == null ? "" : current.trim().replaceAll("\\s+", " ");
        if (!a.equalsIgnoreCase(b)) changes.add(label);
    }

    private static void changedId(List<String> changes, String label, String given, String current) {
        if (!isBlank(given) && !given.equalsIgnoreCase(current == null ? "" : current)) changes.add(label);
    }

    private static boolean has(Map<String, String> v, String key) { return !isBlank(v.get(key)); }
    private static String or(String given, String current) { return isBlank(given) ? current : given.trim(); }
    private static String nz(String s) { return s == null ? "" : s; }

    private static void unique(String value, Map<String, String> existing, Map<String, Integer> inFile, int rowNumber,
                               String key, String label, java.util.function.BiConsumer<String, String> err, boolean upper) {
        if (isBlank(value)) return;
        String k = upper ? value.toUpperCase(Locale.ROOT) : value.toLowerCase(Locale.ROOT);
        if (existing.containsKey(k)) err.accept(key, "is already used by " + existing.get(k));
        Integer first = inFile.putIfAbsent(k, rowNumber);
        if (first != null) err.accept(key, "repeats row " + first + " of this file");
    }

    private static void length(Map<String, String> v, String key, int max, java.util.function.BiConsumer<String, String> err) {
        if (v.get(key) != null && v.get(key).length() > max) err.accept(key, "is longer than " + max + " characters");
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String digits(String s) { return s == null ? null : s.replaceAll("[\\s\\-]", ""); }
    private static String upper(String s) { return s == null ? null : s.toUpperCase(Locale.ROOT); }

    private String write(List<RowResult> rows) {
        try { return objectMapper.writeValueAsString(rows); }
        catch (Exception e) { throw new IllegalStateException("Could not store the checked rows", e); }
    }

    private List<RowResult> read(String json) {
        if (json == null) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<List<RowResult>>() { }); }
        catch (Exception e) { throw new IllegalStateException("Could not read the checked rows", e); }
    }

    private ImportJobResponse toResponse(StaffImportJob j, List<RowResult> rows) {
        return ImportJobResponse.builder().jobId(j.getJobId()).fileName(j.getFileName()).branchId(j.getBranchId()).status(j.getStatus())
                .totalRows(j.getTotalRows()).validRows(j.getValidRows()).errorRows(j.getErrorRows()).mode(j.getMode()).updatedCount(j.getUpdatedCount()).createdCount(j.getCreatedCount())
                .createdBy(j.getCreatedBy()).createdAt(j.getCreatedAt()).committedBy(j.getCommittedBy()).committedAt(j.getCommittedAt())
                .rows(rows).build();
    }
}
