package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.CreateAdminRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import com.example.ordermgmt.dto.analytics.MonthlyReportRequestDTO;
import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.exception.GlobalExceptionHandler;
import com.example.ordermgmt.service.AdminAnalyticsService;
import com.example.ordermgmt.service.OrgAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrgAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrgAdminService orgAdminService;

    @Mock
    private AdminAnalyticsService adminAnalyticsService;

    @InjectMocks
    private OrgAdminController orgAdminController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orgAdminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateAdmin_Success() throws Exception {
        CreateAdminRequestDTO requestDTO = new CreateAdminRequestDTO("new-admin@example.com", "password123");
        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setUserId(UUID.randomUUID());
        responseDTO.setEmail("new-admin@example.com");
        responseDTO.setRole("ADMIN");
        responseDTO.setMessage("Admin created successfully");

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("org-admin@example.com");
        when(orgAdminService.createAdmin(eq("org-admin@example.com"), any(CreateAdminRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/org-admin/admins")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new-admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(orgAdminService, times(1)).createAdmin(eq("org-admin@example.com"), any(CreateAdminRequestDTO.class));
    }

    @Test
    void testCreateAdmin_BadRequest_ValidationFailure() throws Exception {
        CreateAdminRequestDTO requestDTO = new CreateAdminRequestDTO("", "123");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("org-admin@example.com");

        mockMvc.perform(post("/api/org-admin/admins")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(orgAdminService, never()).createAdmin(any(), any());
    }

    @Test
    void testListAdmins_Success() throws Exception {
        UserResponseDTO adminDTO = new UserResponseDTO();
        adminDTO.setUserId(UUID.randomUUID());
        adminDTO.setEmail("admin1@example.com");
        adminDTO.setRole("ADMIN");

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("org-admin@example.com");
        when(orgAdminService.listAdmins("org-admin@example.com"))
                .thenReturn(Collections.singletonList(adminDTO));

        mockMvc.perform(get("/api/org-admin/admins")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admins").isArray())
                .andExpect(jsonPath("$.admins[0].email").value("admin1@example.com"));

        verify(orgAdminService, times(1)).listAdmins("org-admin@example.com");
    }

    @Test
    void testUpdateAdminStatus_Success() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        UpdateUserStatusRequestDTO requestDTO = new UpdateUserStatusRequestDTO(true);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("org-admin@example.com");
        doNothing().when(orgAdminService).updateAdminStatus(eq("org-admin@example.com"), eq(adminUserId), any(UpdateUserStatusRequestDTO.class));

        mockMvc.perform(patch("/api/org-admin/admins/{id}/status", adminUserId)
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin status updated successfully."));

        verify(orgAdminService, times(1))
                .updateAdminStatus(eq("org-admin@example.com"), eq(adminUserId), any(UpdateUserStatusRequestDTO.class));
    }

    @Test
    void testSendReportEmail_Success() throws Exception {
        MonthlyReportRequestDTO requestDTO = new MonthlyReportRequestDTO("JANUARY", 2025);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("org-admin@example.com");
        doNothing().when(adminAnalyticsService).sendMonthlyReportEmail("JANUARY", 2025, "org-admin@example.com");

        mockMvc.perform(post("/api/org-admin/analytics/sendreportemail")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Report email request submitted for org-admin@example.com"));

        verify(adminAnalyticsService, times(1))
                .sendMonthlyReportEmail("JANUARY", 2025, "org-admin@example.com");
    }

    @Test
    void testGetMonthlyReport_Success() throws Exception {
        MonthlySalesLogDTO reportDTO = new MonthlySalesLogDTO();
        reportDTO.setTotalSoldItems(120L);
        reportDTO.setTotalRevenue(new BigDecimal("7650.50"));

        when(adminAnalyticsService.getMonthlyReport("JANUARY", 2025)).thenReturn(reportDTO);

        mockMvc.perform(get("/api/org-admin/analytics/monthlyreport")
                        .param("month", "JANUARY")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSoldItems").value(120))
                .andExpect(jsonPath("$.totalRevenue").value(7650.50));

        verify(adminAnalyticsService, times(1)).getMonthlyReport("JANUARY", 2025);
    }

    @Test
    void testGetMonthlyReport_BadRequest_MissingYear() throws Exception {
        mockMvc.perform(get("/api/org-admin/analytics/monthlyreport")
                        .param("month", "JANUARY"))
                .andExpect(status().isBadRequest());

        verify(adminAnalyticsService, never()).getMonthlyReport(anyString(), anyInt());
    }

    @Test
    void testCreateAdmin_InternalServerError() throws Exception {
        CreateAdminRequestDTO requestDTO = new CreateAdminRequestDTO("new-admin@example.com", "password123");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("org-admin@example.com");
        doThrow(new RuntimeException("Service failure"))
                .when(orgAdminService).createAdmin(eq("org-admin@example.com"), any(CreateAdminRequestDTO.class));

        mockMvc.perform(post("/api/org-admin/admins")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError());
    }
}
