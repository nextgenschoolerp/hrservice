package com.kvs.hr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kvs.hr.dto.*;
import com.kvs.hr.enums.AnomalyType;
import com.kvs.hr.enums.RegularisationStatus;
import com.kvs.hr.enums.ScanType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BiometricAttendanceControllerIntegrationTest extends HrTestSupport {

    @Test
    @DisplayName("US-07001: Ingest biometric scan logs and query staff scan history")
    void testBiometricScanIngest() throws Exception {
        String accountantToken = token("PAYROLL_ACCOUNTANT");
        String teacherToken = token("FACULTY_MEMBER");

        BiometricScanRequest req = BiometricScanRequest.builder()
                .staffId(staffRajesh.getStaffId())
                .terminalDeviceId("DEVICE-GATE-01")
                .scanTimestamp(Instant.now())
                .scanType(ScanType.IN)
                .build();

        JsonNode scanNode = data(postJson("/attendance/scans", accountantToken, req).andExpect(status().isCreated()));
        assertThat(scanNode.get("terminalDeviceId").asText()).isEqualTo("DEVICE-GATE-01");
        assertThat(scanNode.get("scanType").asText()).isEqualTo("IN");

        // List scans
        JsonNode list = data(getAs("/attendance/scans/" + staffRajesh.getStaffId(), teacherToken).andExpect(status().isOk()));
        assertThat(list.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("US-07002: Configure flexible shift schedules and assign to staff")
    void testShiftRoster() throws Exception {
        String principalToken = token("SCHOOL_PRINCIPAL");
        String teacherToken = token("FACULTY_MEMBER");

        StaffShiftRequest shiftReq = StaffShiftRequest.builder()
                .shiftCode("SHIFT-MORNING-" + run)
                .shiftName("Morning Academic Shift")
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(14, 0))
                .graceMinutes(15)
                .halfDayThresholdMinutes(210)
                .build();

        JsonNode shiftNode = data(postJson("/attendance/shifts", principalToken, shiftReq).andExpect(status().isCreated()));
        UUID shiftId = UUID.fromString(shiftNode.get("shiftId").asText());
        assertThat(shiftNode.get("shiftCode").asText()).isEqualTo("SHIFT-MORNING-" + run);

        // List shifts
        JsonNode shifts = data(getAs("/attendance/shifts", teacherToken).andExpect(status().isOk()));
        assertThat(shifts.size()).isGreaterThanOrEqualTo(1);

        // Assign shift
        ShiftAssignmentRequest assignReq = ShiftAssignmentRequest.builder()
                .staffId(staffRajesh.getStaffId())
                .shiftId(shiftId)
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .build();

        postJson("/attendance/shifts/assignments", principalToken, assignReq).andExpect(status().isOk());
    }

    @Test
    @DisplayName("US-07013: Department-wise staff attendance summaries on principal dashboard")
    void testAttendanceSummary() throws Exception {
        String principalToken = token("SCHOOL_PRINCIPAL");

        JsonNode summary = data(getAs("/attendance/summary", principalToken, "date", LocalDate.now().toString()).andExpect(status().isOk()));
        assertThat(summary.get("totalStaff").asLong()).isGreaterThanOrEqualTo(2L);
        assertThat(summary.has("attendancePercentage")).isTrue();
    }

    @Test
    @DisplayName("US-07015: Flag biometric punch anomalies and regularise with corrected timestamps")
    void testAnomalyRegularisation() throws Exception {
        String hrToken = token("HR_MANAGER");

        // Flag anomaly
        JsonNode anomalyNode = data(postJson("/attendance/anomalies/" + staffKavya.getStaffId() +
                "?anomalyDate=" + LocalDate.now() + "&type=MISSING_OUT_PUNCH", hrToken, null).andExpect(status().isCreated()));
        UUID anomalyId = UUID.fromString(anomalyNode.get("anomalyId").asText());
        assertThat(anomalyNode.get("status").asText()).isEqualTo("PENDING");

        // Regularise anomaly
        AnomalyRegularisationRequest regReq = AnomalyRegularisationRequest.builder()
                .regularisationReason("Forgot punch out due to lab practical examination duties")
                .correctedInTime(LocalTime.of(8, 0))
                .correctedOutTime(LocalTime.of(16, 30))
                .build();

        JsonNode regNode = data(putJson("/attendance/anomalies/" + anomalyId + "/regularise", hrToken, regReq).andExpect(status().isOk()));
        assertThat(regNode.get("status").asText()).isEqualTo("APPROVED");
        assertThat(regNode.get("regularisationReason").asText()).contains("Forgot punch out");
    }
}
