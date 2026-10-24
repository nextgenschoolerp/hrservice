package com.kvs.hr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.service.JwtTokenService;
import com.kvs.hr.entity.StaffProfile;
import com.kvs.hr.enums.EmploymentStatus;
import com.kvs.hr.enums.ProbationStatus;
import com.kvs.hr.repo.StaffProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class HrTestSupport {

    static final String BASE = "/api/v1/hr";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JwtTokenService jwtTokenService;
    @Autowired protected StaffProfileRepository staffRepository;

    protected final String run = UUID.randomUUID().toString().substring(0, 8);
    protected final String tenantId = "T-HR-" + run;
    protected final String tenant = tenantId;
    protected final String branchId = "TEST-NORTH-" + run;
    /** userId of the FACULTY_MEMBER test token, linked to staffKavya's profile */
    protected final String kavyaUserId = "faculty_member-" + run;

    protected StaffProfile staffRajesh;
    protected StaffProfile staffKavya;

    @BeforeEach
    void baseSetUp() {
        staffRajesh = staffRepository.save(StaffProfile.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .employeeCode("EMP-RAJESH-" + run)
                .firstName("Rajesh")
                .lastName("Sharma")
                .department("Academics")
                .designation("Senior Teacher")
                .basicSalary(new BigDecimal("55000.00"))
                .joiningDate(LocalDate.of(2018, 6, 1))
                .isActive(true)
                .email("rajesh." + run + "@kvs.local")
                .phone("9876543210")
                .panNumber("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .pfUan("100904567890")
                .esiNumber("3100456789")
                .bankName("State Bank of India")
                .bankAccountNumber("30123456789")
                .bankIfsc("SBIN0001234")
                .probationStatus(ProbationStatus.CONFIRMED)
                .probationEndDate(LocalDate.of(2018, 12, 1))
                .employmentStatus(EmploymentStatus.ACTIVE)
                .qualifications("M.Sc, B.Ed")
                .contractTerms("Permanent Full-Time")
                .build());

        staffKavya = staffRepository.save(StaffProfile.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .userId(kavyaUserId)
                .employeeCode("EMP-KAVYA-" + run)
                .firstName("Kavya")
                .lastName("Nair")
                .department("Science")
                .designation("Assistant Teacher")
                .basicSalary(new BigDecimal("42000.00"))
                .joiningDate(LocalDate.of(2023, 7, 15))
                .isActive(true)
                .email("kavya." + run + "@kvs.local")
                .phone("9876543211")
                .panNumber("FGHIJ5678K")
                .aadhaarNumber("234567890123")
                .pfUan("100904567891")
                .esiNumber("3100456790")
                .bankName("HDFC Bank")
                .bankAccountNumber("50123456789")
                .bankIfsc("HDFC0001234")
                .probationStatus(ProbationStatus.ON_PROBATION)
                .probationEndDate(LocalDate.of(2024, 1, 15))
                .employmentStatus(EmploymentStatus.ACTIVE)
                .qualifications("M.Sc Physics")
                .contractTerms("Probationary Contract")
                .build());
    }

    protected String token(String role) {
        return token(role, role.toLowerCase(Locale.ROOT) + "-" + run);
    }

    protected String token(String role, String userId) {
        return token(role, userId, tenant);
    }

    protected String token(String role, String userId, String tenantStr) {
        return "Bearer " + jwtTokenService.generateToken(userId + "@kvs.local",
                Map.of("roles", List.of("ROLE_" + role), "tenantId", tenantStr, "userId", userId));
    }

    protected ResultActions postJson(String path, String token, Object body) throws Exception {
        return mockMvc.perform(withBody(post(BASE + path), token, body));
    }

    protected ResultActions putJson(String path, String token, Object body) throws Exception {
        return mockMvc.perform(withBody(put(BASE + path), token, body));
    }

    protected ResultActions getAs(String path, String token, String... params) throws Exception {
        MockHttpServletRequestBuilder req = get(BASE + path);
        if (token != null) {
            req.header("Authorization", token);
        }
        for (int i = 0; i < params.length; i += 2) {
            req.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(req);
    }

    protected MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        if (token != null) {
            builder.header("Authorization", token);
        }
        builder.contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            builder.content(objectMapper.writeValueAsString(body));
        }
        return builder;
    }

    protected JsonNode data(ResultActions actions) throws Exception {
        byte[] bytes = actions.andReturn().getResponse().getContentAsByteArray();
        JsonNode root = objectMapper.readTree(bytes);
        return root.has("data") ? root.get("data") : root;
    }
}
