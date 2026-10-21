package com.kvs.hr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kvs.hr.dto.LeaveApplicationRequest;
import com.kvs.hr.dto.StaffProfileRequest;
import com.kvs.hr.enums.LeaveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Staff & faculty screens: account link (userId), campus codes, authservice role codes, and masking of pay and
 * identity fields for roles outside HR / payroll / principal.
 */
class StaffDirectoryAccessIntegrationTest extends HrTestSupport {

    @Test
    @DisplayName("Directory: authservice teacher roles may list staff; pay and ID fields are hidden from them")
    void directoryMasksConfidentialFieldsForTeachers() throws Exception {
        for (String role : new String[]{"SUBJECT_TEACHER", "CLASS_TEACHER", "OFFICE_CLERK", "REGISTRAR"}) {
            JsonNode list = data(getAs("/employees", token(role)).andExpect(status().isOk()));
            assertThat(list.size()).as(role).isEqualTo(2);
            JsonNode rajesh = find(list, "EMP-RAJESH-" + run);
            assertThat(rajesh.get("designation").asText()).isEqualTo("Senior Teacher");
            assertThat(absent(rajesh, "basicSalary")).as(role + " salary").isTrue();
            assertThat(absent(rajesh, "panNumber")).isTrue();
            assertThat(absent(rajesh, "aadhaarNumber")).isTrue();
            assertThat(absent(rajesh, "bankAccountNumber")).isTrue();
        }
    }

    @Test
    @DisplayName("Directory: HR, payroll, principal and super admin see pay and ID fields")
    void confidentialViewersSeeEverything() throws Exception {
        for (String role : new String[]{"HR_MANAGER", "PAYROLL_ACCOUNTANT", "PRINCIPAL", "SCHOOL_PRINCIPAL"}) {
            JsonNode rajesh = find(data(getAs("/employees", token(role)).andExpect(status().isOk())), "EMP-RAJESH-" + run);
            assertThat(new BigDecimal(rajesh.get("basicSalary").asText())).as(role).isEqualByComparingTo("55000.00");
            assertThat(rajesh.get("panNumber").asText()).isEqualTo("ABCDE1234F");
        }
    }

    @Test
    @DisplayName("Parents and unknown roles are refused (403)")
    void parentRefused() throws Exception {
        getAs("/employees", token("APPLICANT_PARENT")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /employees/me returns the caller's own profile, with their own pay fields; 404 without a profile")
    void myProfile() throws Exception {
        JsonNode me = data(getAs("/employees/me", token("FACULTY_MEMBER", kavyaUserId)).andExpect(status().isOk()));
        assertThat(me.get("staffId").asText()).isEqualTo(staffKavya.getStaffId().toString());
        assertThat(me.get("userId").asText()).isEqualTo(kavyaUserId);
        assertThat(new BigDecimal(me.get("basicSalary").asText())).isEqualByComparingTo("42000.00");

        // A teacher reading a colleague's profile does not get the pay fields
        JsonNode colleague = data(getAs("/employees/" + staffRajesh.getStaffId(), token("FACULTY_MEMBER", kavyaUserId)).andExpect(status().isOk()));
        assertThat(absent(colleague, "basicSalary")).isTrue();

        getAs("/employees/me", token("SUBJECT_TEACHER", "nobody-" + run)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("List filters by campus code; create needs a campus and stores the account link")
    void branchFilterAndCreate() throws Exception {
        String hr = token("HR_MANAGER");
        assertThat(data(getAs("/employees", hr, "branchId", branchId)).size()).isEqualTo(2);
        assertThat(data(getAs("/employees", hr, "branchId", "OTHER-CAMPUS")).size()).isZero();

        StaffProfileRequest req = StaffProfileRequest.builder()
                .employeeCode("EMP-NEW-" + run).firstName("Meenal").lastName("Joshi").department("Academics")
                .designation("Substitution in-charge").basicSalary(new BigDecimal("48000")).joiningDate(LocalDate.of(2024, 6, 1))
                .build();
        postJson("/employees", hr, req).andExpect(status().isBadRequest());

        req.setBranchId("TEST-SOUTH-" + run);
        req.setUserId("meenal-" + run);
        JsonNode created = data(postJson("/employees", hr, req).andExpect(status().isCreated()));
        assertThat(created.get("branchId").asText()).isEqualTo("TEST-SOUTH-" + run);
        assertThat(created.get("userId").asText()).isEqualTo("meenal-" + run);
        assertThat(data(getAs("/employees", hr, "branchId", "TEST-SOUTH-" + run)).size()).isEqualTo(1);

        // An update that leaves out the bank and ID fields keeps them
        req.setPanNumber("PQRST1234U");
        String id = created.get("staffId").asText();
        putJson("/employees/" + id, hr, req).andExpect(status().isOk());
        req.setPanNumber(null);
        JsonNode updated = data(putJson("/employees/" + id, hr, req).andExpect(status().isOk()));
        assertThat(updated.get("panNumber").asText()).isEqualTo("PQRST1234U");
    }

    @Test
    @DisplayName("Leave: staff apply only for themselves; HR may apply for anyone")
    void leaveOnlyForOwnProfile() throws Exception {
        LeaveApplicationRequest req = LeaveApplicationRequest.builder()
                .staffId(staffRajesh.getStaffId()).leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.now().plusDays(5)).endDate(LocalDate.now().plusDays(5)).reason("Personal work").build();
        postJson("/leaves", token("SUBJECT_TEACHER", kavyaUserId), req).andExpect(status().isForbidden());
        postJson("/leaves", token("HR_MANAGER"), req).andExpect(status().isCreated());

        req.setStaffId(staffKavya.getStaffId());
        postJson("/leaves", token("SUBJECT_TEACHER", kavyaUserId), req).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Leave and quota lists by staffId are tenant-isolated (404 for another tenant)")
    void leaveListsTenantIsolated() throws Exception {
        String other = token("HR_MANAGER", "other-hr", "OTHER-TENANT-" + run);
        getAs("/leaves", other, "staffId", staffKavya.getStaffId().toString()).andExpect(status().isNotFound());
        getAs("/leaves/quotas", other, "staffId", staffKavya.getStaffId().toString()).andExpect(status().isNotFound());
        getAs("/leaves", token("HR_MANAGER"), "staffId", staffKavya.getStaffId().toString()).andExpect(status().isOk());
    }

    private static boolean absent(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull();
    }

    private static JsonNode find(JsonNode list, String employeeCode) {
        for (JsonNode n : list) {
            if (employeeCode.equals(n.get("employeeCode").asText())) {
                return n;
            }
        }
        throw new AssertionError("no employee " + employeeCode);
    }
}
