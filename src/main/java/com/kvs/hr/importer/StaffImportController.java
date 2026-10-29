package com.kvs.hr.importer;

import com.kvs.core.dto.ApiResponse;
import com.kvs.core.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bulk staff import (Settings → Data import): download the template, check a filled file (nothing saved), import the
 * checked rows, and see past uploads with their error reports. IT system administrators, super administrators and HR managers.
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/hr/employees/import")
@PreAuthorize("hasAnyRole('IT_SYSTEM_ADMIN', 'SUPER_ADMIN', 'HR_MANAGER')")
public class StaffImportController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");

    @Autowired
    private StaffImportService importService;

    /** mode CREATE (default) or UPDATE; with UPDATE and prefill=true the file already holds the campus's current staff. */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam(value = "branchId", required = false) String branchId,
                                           @RequestParam(value = "format", defaultValue = "xlsx") String format,
                                           @RequestParam(value = "mode", defaultValue = "CREATE") String mode,
                                           @RequestParam(value = "prefill", defaultValue = "false") boolean prefill) {
        boolean csv = "csv".equalsIgnoreCase(format);
        boolean update = "UPDATE".equalsIgnoreCase(mode);
        if (update && prefill && (branchId == null || branchId.isBlank())) throw new BadRequestException("Choose a campus to download its staff", "BRANCH_REQUIRED");
        String name = (update ? (prefill ? "staff-current" : "staff-update-template") : "staff-import-template") + (csv ? ".csv" : ".xlsx");
        return file(importService.template(branchId, format, mode, prefill), name, csv ? CSV : XLSX);
    }

    /** Reads and checks the file; returns every row with its errors. Nothing is saved until Import. */
    @PostMapping(value = "/check", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportJobResponse>> check(@RequestParam("branchId") String branchId,
                                                                @RequestParam(value = "mode", defaultValue = "CREATE") String mode,
                                                                @RequestPart("file") MultipartFile file) {
        log.info("Checking staff {} file {} for branch {}", mode, file.getOriginalFilename(), branchId);
        return ResponseEntity.ok(ApiResponse.success(importService.check(branchId, file, mode)));
    }

    /** A ZIP of photos named after the employee code (DPS-EMP-2001.jpg). dryRun=true only reports; false saves the matched photos. */
    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportJobResponse>> photos(@RequestParam("branchId") String branchId,
                                                                 @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
                                                                 @RequestPart("file") MultipartFile file) {
        log.info("{} staff photo ZIP {}", dryRun ? "Checking" : "Importing", file.getOriginalFilename());
        ImportJobResponse result = importService.photos(branchId, file, dryRun);
        return ResponseEntity.ok(ApiResponse.success(dryRun ? "Checked " + result.getTotalRows() + " photos" : "Saved " + result.getUpdatedCount() + " photos", result));
    }

    /** Links sign-in accounts (created through authservice) to the staff they belong to; staff that already have one are left alone. */
    @PostMapping("/link-accounts")
    public ResponseEntity<ApiResponse<Integer>> linkAccounts(@RequestBody LinkAccountsRequest request) {
        Map<UUID, String> links = new LinkedHashMap<>();
        request.links().forEach(l -> links.put(l.staffId(), l.userId()));
        int linked = importService.linkAccounts(links);
        return ResponseEntity.ok(ApiResponse.success("Linked " + linked + " accounts", linked));
    }

    public record LinkAccountsRequest(List<Link> links) {
        public record Link(UUID staffId, String userId) { }
    }

    /** mode: VALID_ONLY (default) imports the rows without errors; ALL_OR_NOTHING imports only when every row is valid. */
    @PostMapping("/{jobId}/commit")
    public ResponseEntity<ApiResponse<ImportJobResponse>> commit(@PathVariable("jobId") UUID jobId,
                                                                 @RequestParam(value = "mode", defaultValue = "VALID_ONLY") String mode) {
        ImportJobResponse result = importService.commit(jobId, mode);
        return ResponseEntity.ok(ApiResponse.success(("UPDATE".equals(result.getMode()) ? "Updated " + result.getUpdatedCount() : "Imported " + result.getCreatedCount()) + " staff members", result));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<ImportJobResponse>>> history() {
        return ResponseEntity.ok(ApiResponse.success(importService.history()));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<ImportJobResponse>> job(@PathVariable("jobId") UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(importService.get(jobId)));
    }

    @GetMapping("/jobs/{jobId}/errors")
    public ResponseEntity<byte[]> errors(@PathVariable("jobId") UUID jobId) {
        return file(importService.errorReport(jobId), "staff-import-errors.csv", CSV);
    }

    private static ResponseEntity<byte[]> file(byte[] body, String name, MediaType type) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(type)
                .body(body);
    }
}
