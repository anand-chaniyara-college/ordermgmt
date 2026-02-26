package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.analytics.MonthlyReportRequestDTO;
import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.service.AdminAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAnalyticsService adminAnalyticsService;

    @InjectMocks
    private AdminAnalyticsController adminAnalyticsController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAnalyticsController)
                .setControllerAdvice(new com.example.ordermgmt.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetMonthlyReport_Success() throws Exception {
        MonthlySalesLogDTO mockReport = new MonthlySalesLogDTO();
        mockReport.setTotalSoldItems(100L);
        mockReport.setTotalRevenue(new java.math.BigDecimal("5000.00"));

        when(adminAnalyticsService.getMonthlyReport("JANUARY", 2025)).thenReturn(mockReport);

        mockMvc.perform(get("/api/admin/analytics/monthlyreport")
                .param("month", "JANUARY")
                .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSoldItems").value(100))
                .andExpect(jsonPath("$.totalRevenue").value(5000.00));

        verify(adminAnalyticsService, times(1)).getMonthlyReport("JANUARY", 2025);
    }

    @Test
    void testGetMonthlyReport_MissingMonth_BadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/monthlyreport")
                // missing month param
                .param("year", "2025"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetMonthlyReport_ServiceThrowsException() throws Exception {
        when(adminAnalyticsService.getMonthlyReport("JANUARY", 2025))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/admin/analytics/monthlyreport")
                .param("month", "JANUARY")
                .param("year", "2025"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testSendReportEmail_Success() throws Exception {
        MonthlyReportRequestDTO requestDTO = new MonthlyReportRequestDTO();
        requestDTO.setMonth("FEBRUARY");
        requestDTO.setYear(2025);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("admin@example.com");

        doNothing().when(adminAnalyticsService).sendMonthlyReportEmail("FEBRUARY", 2025, "admin@example.com");

        mockMvc.perform(post("/api/admin/analytics/sendreportemail")
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Report email request submitted for admin@example.com"));

        verify(adminAnalyticsService, times(1)).sendMonthlyReportEmail("FEBRUARY", 2025, "admin@example.com");
    }

    @Test
    void testSendReportEmail_InvalidRequest() throws Exception {
        MonthlyReportRequestDTO requestDTO = new MonthlyReportRequestDTO(); // missing fields
        Principal principal = mock(Principal.class);

        mockMvc.perform(post("/api/admin/analytics/sendreportemail")
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSendReportEmail_InternalServerError() throws Exception {
        MonthlyReportRequestDTO requestDTO = new MonthlyReportRequestDTO();
        requestDTO.setMonth("MARCH");
        requestDTO.setYear(2025);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("admin2@test.com");

        doThrow(new RuntimeException("Email service down")).when(adminAnalyticsService)
                .sendMonthlyReportEmail("MARCH", 2025, "admin2@test.com");

        mockMvc.perform(post("/api/admin/analytics/sendreportemail")
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError());
    }
}
