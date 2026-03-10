package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.CreateAdminRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import com.example.ordermgmt.dto.analytics.MonthlyReportRequestDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsItemDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsResponseDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsSaleDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportItemDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportResponseDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportSaleDTO;
import com.example.ordermgmt.exception.GlobalExceptionHandler;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.service.AdminAnalyticsService;
import com.example.ordermgmt.service.OrgAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter = new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                                objectMapper);

                mockMvc = MockMvcBuilders.standaloneSetup(orgAdminController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .setMessageConverters(converter)
                                .build();
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

                verify(orgAdminService, times(1)).createAdmin(eq("org-admin@example.com"),
                                any(CreateAdminRequestDTO.class));
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
                doNothing().when(orgAdminService).updateAdminStatus(eq("org-admin@example.com"), eq(adminUserId),
                                any(UpdateUserStatusRequestDTO.class));

                mockMvc.perform(patch("/api/org-admin/admins/{id}/status", adminUserId)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Admin status updated successfully."));

                verify(orgAdminService, times(1))
                                .updateAdminStatus(eq("org-admin@example.com"), eq(adminUserId),
                                                any(UpdateUserStatusRequestDTO.class));
        }

        @Test
        void testSendReportEmail_Success() throws Exception {
                MonthlyReportRequestDTO requestDTO = new MonthlyReportRequestDTO("JANUARY", 2025);
                Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
                when(authentication.getName()).thenReturn("org-admin@example.com");
                doNothing().when(adminAnalyticsService).sendMonthlyReportEmail("JANUARY", 2025,
                                "org-admin@example.com");

                mockMvc.perform(post("/api/org-admin/analytics/sendreportemail")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("Report email request submitted for org-admin@example.com"));

                verify(adminAnalyticsService, times(1))
                                .sendMonthlyReportEmail("JANUARY", 2025, "org-admin@example.com");
        }

        @Test
        void testGetRevenueReport_Success() throws Exception {
                LocalDate startDate = LocalDate.parse("2026-03-01");
                LocalDate endDate = LocalDate.parse("2026-03-09");

                RevenueReportItemDTO laptop = new RevenueReportItemDTO(
                                UUID.fromString("c0a8085e-9c9f-1727-819c-9f3aae230001"),
                                "Laptop",
                                new BigDecimal("200.00"),
                                List.of(
                                                new RevenueReportSaleDTO(1L, "2026-03-02T14:30:00Z"),
                                                new RevenueReportSaleDTO(1L, "2026-03-05T10:00:00Z")));

                RevenueReportResponseDTO reportDTO = new RevenueReportResponseDTO(
                                startDate,
                                endDate,
                                2L,
                                4L,
                                new BigDecimal("400.00"),
                                List.of(laptop));

                when(adminAnalyticsService.getRevenueReport(startDate, endDate, null, null))
                                .thenReturn(reportDTO);

                mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                                .param("startdate", "2026-03-01")
                                .param("enddate", "2026-03-09"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                                .andExpect(jsonPath("$.endDate").value("2026-03-09"))
                                .andExpect(jsonPath("$.totalSoldItems").value(2))
                                .andExpect(jsonPath("$.totalSoldQty").value(4))
                                .andExpect(jsonPath("$.totalRevenue").value(400.00))
                                .andExpect(jsonPath("$.items[0].itemName").value("Laptop"))
                                .andExpect(jsonPath("$.items[0].sales[0].soldQty").value(1))
                                .andExpect(jsonPath("$.items[0].sales[0].soldOn").value("2026-03-02T14:30:00Z"));

                verify(adminAnalyticsService, times(1)).getRevenueReport(startDate, endDate, null, null);
        }

        @Test
        void testGetOrderAnalytics_Success() throws Exception {
                LocalDate startDate = LocalDate.parse("2026-02-01");
                LocalDate endDate = LocalDate.parse("2026-03-09");

                OrderAnalyticsItemDTO laptop = new OrderAnalyticsItemDTO(
                                UUID.fromString("c0a8085e-9c98-1b65-819c-988b78320003"),
                                "Laptop",
                                List.of(
                                                new OrderAnalyticsSaleDTO("DELIVERED", 1L,
                                                                "2026-02-27T18:58:41.298783Z"),
                                                new OrderAnalyticsSaleDTO("CANCELED", 1L,
                                                                "2026-03-09T16:55:51.009018Z")));

                OrderAnalyticsResponseDTO reportDTO = new OrderAnalyticsResponseDTO(
                                startDate,
                                endDate,
                                1L,
                                2L,
                                List.of(laptop));

                when(adminAnalyticsService.getOrderAnalytics(startDate, endDate, "laptop", "delivered, canceled", null))
                                .thenReturn(reportDTO);

                mockMvc.perform(get("/api/org-admin/analytics/order-analytics")
                                .param("startdate", "2026-02-01")
                                .param("enddate", "2026-03-09")
                                .param("itemname", "laptop")
                                .param("orderstatus", "delivered, canceled"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startDate").value("2026-02-01"))
                                .andExpect(jsonPath("$.endDate").value("2026-03-09"))
                                .andExpect(jsonPath("$.totalSoldItems").value(1))
                                .andExpect(jsonPath("$.totalSoldQty").value(2))
                                .andExpect(jsonPath("$.items[0].itemName").value("Laptop"))
                                .andExpect(jsonPath("$.items[0].sales[0].orderStatus").value("DELIVERED"))
                                .andExpect(jsonPath("$.items[0].sales[0].soldQty").value(1))
                                .andExpect(jsonPath("$.items[0].sales[0].soldOn").value("2026-02-27T18:58:41.298783Z"));

                verify(adminAnalyticsService, times(1))
                                .getOrderAnalytics(startDate, endDate, "laptop", "delivered, canceled", null);
        }

        @Test
        void testGetRevenueReport_BadRequest_MissingEndDate() throws Exception {
                mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                                .param("startdate", "2026-03-01"))
                                .andExpect(status().isBadRequest());

                verify(adminAnalyticsService, never()).getRevenueReport(any(), any(), any(), any());
        }

        @Test
        void testGetRevenueReport_BadRequest_InvalidRange() throws Exception {
                LocalDate startDate = LocalDate.parse("2026-03-09");
                LocalDate endDate = LocalDate.parse("2026-03-01");
                when(adminAnalyticsService.getRevenueReport(eq(startDate), eq(endDate), eq("Laptop"), any()))
                                .thenThrow(new InvalidOperationException("startDate must be before endDate"));

                mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                                .param("startdate", "2026-03-09")
                                .param("enddate", "2026-03-01")
                                .param("itemname", "Laptop")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("startDate must be before endDate"));

                verify(adminAnalyticsService, times(1)).getRevenueReport(eq(startDate), eq(endDate), eq("Laptop"),
                                any());
        }

        @Test
        void testCreateAdmin_InternalServerError() throws Exception {
                CreateAdminRequestDTO requestDTO = new CreateAdminRequestDTO("new-admin@example.com", "password123");
                Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
                when(authentication.getName()).thenReturn("org-admin@example.com");
                doThrow(new RuntimeException("Service failure"))
                                .when(orgAdminService)
                                .createAdmin(eq("org-admin@example.com"), any(CreateAdminRequestDTO.class));

                mockMvc.perform(post("/api/org-admin/admins")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isInternalServerError());
        }
}
