package com.kvs.hr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kvs.hr.dto.*;
import com.kvs.hr.enums.ProbationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeControllerIntegrationTest extends HrTestSupport {

    @Test
    @DisplayName("US-07000: Create, retrieve, update employee master record and verify tenant isolation and validations")
    void testEmployeeMasterLifecycle() throws Exception {
        String hrToken = token("HR_MANAGER");
        String teacherToken = token("FACULTY_MEMBER");

        // Happy path: create employee
        StaffProfileRequest req = StaffProfileRequest.builder()
                .employeeCode("EMP-ANIL-" + run)
                .branchId(branchId)
                .firstName("Anil")
                .lastName("Kumar")
                .department("Mathematics")
                .designation("PGT Teacher")
                .basicSalary(new BigDecimal("60000.00"))
                .joiningDate(LocalDate.of(2022, 4, 1))
                .email("anil." + run + "@kvs.local")
                .phone("9876543222")
                .panNumber("ABCDE9999Z")
                .aadhaarNumber("999988887777")
                .pfUan("100904569999")
                .esiNumber("3100459999")
                .bankName("State Bank of India")
                .bankAccountNumber("30999999999")
                .bankIfsc("SBIN0001234")
                .qualifications("M.Sc Mathematics, B.Ed")
                .contractTerms("Regular")
                .build();

        JsonNode created = data(postJson("/employees", hrToken, req).andExpect(status().isCreated()));
        UUID staffId = UUID.fromString(created.get("staffId").asText());
        assertThat(created.get("employeeCode").asText()).isEqualTo("EMP-ANIL-" + run);
        assertThat(created.get("probationStatus").asText()).isEqualTo("ON_PROBATION");

        // Retrieve by ID
        JsonNode fetched = data(getAs("/employees/" + staffId, teacherToken).andExpect(status().isOk()));
        assertThat(fetched.get("firstName").asText()).isEqualTo("Anil");
        assertThat(fetched.get("branchId").asText()).isEqualTo(branchId);

        // Retrieve by Code
        JsonNode fetchedByCode = data(getAs("/employees/by-code/EMP-ANIL-" + run, hrToken).andExpect(status().isOk()));
        assertThat(fetchedByCode.get("staffId").asText()).isEqualTo(staffId.toString());

        // Update employee
        req.setDesignation("Head of Mathematics");
        req.setBasicSalary(new BigDecimal("65000.00"));
        JsonNode updated = data(putJson("/employees/" + staffId, hrToken, req).andExpect(status().isOk()));
        assertThat(updated.get("designation").asText()).isEqualTo("Head of Mathematics");
        assertThat(new BigDecimal(updated.get("basicSalary").asText())).isEqualByComparingTo("65000.00");

        // Negative: Duplicate employee code -> 409 Conflict
        postJson("/employees", hrToken, req).andExpect(status().isConflict());

        // Negative: Validation failure (blank employee code) -> 400 Bad Request
        req.setEmployeeCode("");
        postJson("/employees", hrToken, req).andExpect(status().isBadRequest());

        // Negative: Unauthenticated -> 401 Unauthorized
        getAs("/employees/" + staffId, null).andExpect(status().isUnauthorized());

        // Negative: Forbidden role for create -> 403 Forbidden
        req.setEmployeeCode("EMP-FORBIDDEN-" + run);
        postJson("/employees", teacherToken, req).andExpect(status().isForbidden());

        // Negative: Other tenant access -> 404 Not Found
        String otherTenantToken = token("HR_MANAGER", "other-user", "OTHER-TENANT-" + run);
        getAs("/employees/" + staffId, otherTenantToken).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-07010: Track employee probation completion dates and record appraisal reviews")
    void testProbationAndAppraisals() throws Exception {
        String principalToken = token("SCHOOL_PRINCIPAL");
        String teacherToken = token("FACULTY_MEMBER");

        StaffAppraisalRequest req = StaffAppraisalRequest.builder()
                .staffId(staffKavya.getStaffId())
                .reviewDate(LocalDate.now())
                .appraisalCycle("Probation End Review")
                .performanceRating(new BigDecimal("4.5"))
                .probationDecision(ProbationStatus.CONFIRMED)
                .reviewerComments("Excellent subject knowledge and positive student feedback")
                .build();

        JsonNode appraisal = data(postJson("/employees/appraisals", principalToken, req).andExpect(status().isCreated()));
        assertThat(appraisal.get("performanceRating").asDouble()).isEqualTo(4.5);
        assertThat(appraisal.get("probationDecision").asText()).isEqualTo("CONFIRMED");

        // Verify staff profile probation status is updated to CONFIRMED
        JsonNode staffNode = data(getAs("/employees/" + staffKavya.getStaffId(), teacherToken).andExpect(status().isOk()));
        assertThat(staffNode.get("probationStatus").asText()).isEqualTo("CONFIRMED");

        // List appraisals
        JsonNode list = data(getAs("/employees/" + staffKavya.getStaffId() + "/appraisals", teacherToken).andExpect(status().isOk()));
        assertThat(list.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("US-07016: Manage resignation notices and official clearance NOC workflow")
    void testResignationAndClearance() throws Exception {
        String hrToken = token("HR_MANAGER");

        StaffResignationRequest req = StaffResignationRequest.builder()
                .staffId(staffRajesh.getStaffId())
                .submissionDate(LocalDate.now())
                .noticePeriodDays(30)
                .requestedLastWorkingDay(LocalDate.now().plusDays(30))
                .reason("Relocating to another city")
                .build();

        JsonNode resNode = data(postJson("/employees/resignations", hrToken, req).andExpect(status().isCreated()));
        UUID resId = UUID.fromString(resNode.get("resignationId").asText());
        assertThat(resNode.get("status").asText()).isEqualTo("SUBMITTED");

        // Update partial clearance
        DepartmentClearanceRequest lib = DepartmentClearanceRequest.builder().department("LIBRARY").cleared(true).build();
        JsonNode step1 = data(putJson("/employees/resignations/" + resId + "/clearance", hrToken, lib).andExpect(status().isOk()));
        assertThat(step1.get("status").asText()).isEqualTo("CLEARANCE_PENDING");

        // Complete remaining clearances
        putJson("/employees/resignations/" + resId + "/clearance", hrToken,
                DepartmentClearanceRequest.builder().department("ACCOUNTS").cleared(true).build());
        putJson("/employees/resignations/" + resId + "/clearance", hrToken,
                DepartmentClearanceRequest.builder().department("IT").cleared(true).build());
        JsonNode finalRes = data(putJson("/employees/resignations/" + resId + "/clearance", hrToken,
                DepartmentClearanceRequest.builder().department("LAB").cleared(true).build()).andExpect(status().isOk()));

        assertThat(finalRes.get("status").asText()).isEqualTo("NOC_ISSUED");
        assertThat(finalRes.get("nocReference").asText()).startsWith("NOC-DPS-");
    }

    @Test
    @DisplayName("US-07019: Archive retired staff employment records for statutory audit compliance")
    void testStaffArchival() throws Exception {
        String hrToken = token("HR_MANAGER");

        StaffArchiveRequest req = StaffArchiveRequest.builder()
                .staffId(staffRajesh.getStaffId())
                .retirementDate(LocalDate.now())
                .retentionPeriodYears(10)
                .archiveReason("Superannuation retirement")
                .dossierReference("VAULT-DOSSIER-" + staffRajesh.getEmployeeCode())
                .build();

        JsonNode archive = data(postJson("/employees/archives", hrToken, req).andExpect(status().isCreated()));
        assertThat(archive.get("dossierReference").asText()).contains("VAULT-DOSSIER-");

        // List archives
        JsonNode list = data(getAs("/employees/archives", hrToken).andExpect(status().isOk()));
        assertThat(list.size()).isGreaterThanOrEqualTo(1);
    }
}
