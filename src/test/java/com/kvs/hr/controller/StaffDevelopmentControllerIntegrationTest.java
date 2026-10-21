package com.kvs.hr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kvs.hr.dto.*;
import com.kvs.hr.enums.LeaveType;
import com.kvs.hr.repo.LeaveQuotaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaffDevelopmentControllerIntegrationTest extends HrTestSupport {

    @Autowired
    private LeaveQuotaRepository leaveQuotaRepository;

    @Test
    @DisplayName("US-07009: Log staff CPD training credits and hours")
    void testCpdCreditTracker() throws Exception {
        String teacherToken = token("FACULTY_MEMBER");

        CpdRecordRequest req = CpdRecordRequest.builder()
                .staffId(staffRajesh.getStaffId())
                .courseTitle("NEP 2020 Pedagogical Innovations & AI in Classroom")
                .conductingAgency("NCERT & CBSE Board")
                .completionDate(LocalDate.now().minusDays(5))
                .trainingHours(new BigDecimal("25.0"))
                .creditsEarned(new BigDecimal("4.0"))
                .certificateReference("CERT-NCERT-2024-9988")
                .build();

        JsonNode cpdNode = data(postJson("/development/cpd", teacherToken, req).andExpect(status().isCreated()));
        assertThat(cpdNode.get("courseTitle").asText()).contains("NEP 2020");
        assertThat(cpdNode.get("creditsEarned").asDouble()).isEqualTo(4.0);

        // List CPD
        JsonNode list = data(getAs("/development/cpd/" + staffRajesh.getStaffId(), teacherToken).andExpect(status().isOk()));
        assertThat(list.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("US-07011: Calculate gratuity and leave encashment values during staff offboarding")
    void testStaffSettlement() throws Exception {
        String accountantToken = token("PAYROLL_ACCOUNTANT");

        // Seed 15 days earned leave for staffRajesh (joined in 2018 -> >= 5 years -> eligible for gratuity)
        postJson("/leaves/quotas", token("HR_MANAGER"), LeaveQuotaRequest.builder()
                .staffId(staffRajesh.getStaffId())
                .quotaYear(2024)
                .leaveType(LeaveType.EARNED)
                .totalEntitled(new BigDecimal("15.0"))
                .build()).andExpect(status().isCreated());

        StaffSettlementRequest req = StaffSettlementRequest.builder()
                .staffId(staffRajesh.getStaffId())
                .lastWorkingDay(LocalDate.of(2024, 6, 30))
                .noticePayAdjustment(BigDecimal.ZERO)
                .otherAdditions(new BigDecimal("2000.00")) // performance bonus
                .otherDeductions(new BigDecimal("500.00"))  // uniform recovery
                .remarks("Full and Final Settlement on superannuation")
                .build();

        JsonNode settlement = data(postJson("/development/settlement", accountantToken, req).andExpect(status().isCreated()));
        assertThat(settlement.get("completedYearsService").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(settlement.get("gratuityAmount").asDouble()).isGreaterThan(0.0);
        assertThat(settlement.get("leaveEncashmentAmount").asDouble()).isGreaterThan(0.0);
        assertThat(settlement.get("netSettlementAmount").asDouble()).isGreaterThan(0.0);
    }
}
