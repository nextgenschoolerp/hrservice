package com.kvs.hr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kvs.hr.dto.*;
import com.kvs.hr.enums.LeaveStatus;
import com.kvs.hr.enums.LeaveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeaveControllerIntegrationTest extends HrTestSupport {

    @Test
    @DisplayName("US-07003, US-07004, US-07017: Leave application, review, and statutory quota lifecycle")
    void testLeaveLifecycle() throws Exception {
        String teacherToken = token("FACULTY_MEMBER");
        String hrToken = token("HR_MANAGER");

        // US-07017: Configure annual casual and maternity leave quotas
        LeaveQuotaRequest clReq = LeaveQuotaRequest.builder()
                .staffId(staffKavya.getStaffId())
                .quotaYear(LocalDate.now().getYear())
                .leaveType(LeaveType.CASUAL)
                .totalEntitled(new BigDecimal("12.0"))
                .build();
        postJson("/leaves/quotas", hrToken, clReq).andExpect(status().isCreated());

        LeaveQuotaRequest mlReq = LeaveQuotaRequest.builder()
                .staffId(staffKavya.getStaffId())
                .quotaYear(LocalDate.now().getYear())
                .leaveType(LeaveType.MATERNITY)
                .totalEntitled(new BigDecimal("180.0"))
                .build();
        postJson("/leaves/quotas", hrToken, mlReq).andExpect(status().isCreated());

        // US-07003: Staff submits leave application
        LeaveApplicationRequest applyReq = LeaveApplicationRequest.builder()
                .staffId(staffKavya.getStaffId())
                .leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(3))
                .reason("Attending family wedding ceremony")
                .proxyNotes("Syllabus coverage assigned to Mr. Rajesh Sharma")
                .build();

        JsonNode leaveNode = data(postJson("/leaves", teacherToken, applyReq).andExpect(status().isCreated()));
        UUID appId = UUID.fromString(leaveNode.get("applicationId").asText());
        assertThat(leaveNode.get("status").asText()).isEqualTo("PENDING");
        assertThat(leaveNode.get("totalDays").asDouble()).isEqualTo(2.0);

        // US-07004: HR reviews and approves leave
        LeaveReviewRequest reviewReq = LeaveReviewRequest.builder()
                .status(LeaveStatus.APPROVED)
                .reviewComments("Approved with proxy coverage confirmed")
                .build();

        JsonNode reviewed = data(putJson("/leaves/" + appId + "/review", hrToken, reviewReq).andExpect(status().isOk()));
        assertThat(reviewed.get("status").asText()).isEqualTo("APPROVED");

        // Verify quota balance deducted (12 - 2 = 10)
        JsonNode quotas = data(getAs("/leaves/quotas", teacherToken, "staffId", staffKavya.getStaffId().toString()).andExpect(status().isOk()));
        boolean verified = false;
        for (JsonNode q : quotas) {
            if (q.get("leaveType").asText().equals("CASUAL")) {
                assertThat(q.get("balanceDays").asDouble()).isEqualTo(10.0);
                verified = true;
            }
        }
        assertThat(verified).isTrue();
    }
}
