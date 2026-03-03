package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.exception.GlobalExceptionHandler;
import com.example.ordermgmt.service.AdminAnalyticsService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAnalyticsService adminAnalyticsService;

    @InjectMocks
    private AdminAnalyticsController adminAnalyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAnalyticsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetMonthlyReport_Success() throws Exception {
        MonthlySalesLogDTO mockReport = new MonthlySalesLogDTO();
        mockReport.setTotalSoldItems(100L);
        mockReport.setTotalRevenue(new BigDecimal("5000.00"));

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
}
