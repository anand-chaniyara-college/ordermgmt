package com.example.ordermgmt.integration;

import com.example.ordermgmt.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Analytics Suite Integration Test (ORG_ADMIN)
 *
 * Covers:
 * 1. Login ORG_ADMIN
 * 2. Revenue report with valid date range covering created orders
 * 3. Order analytics with itemName and orderStatus filters + pagination
 * 4. Revenue report with sendEmail=true and emailTo
 * 5. Revenue report with startDate after endDate → validation error
 * 6. Logout ORG_ADMIN
 *
 * Prerequisites: AuthIntegrationTest must have run first.
 * Orders should exist from OrderLifecycleIntegrationTest or similar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(6)
public class AnalyticsIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        // Shared state
        private static String orgAdminAccessToken;
        private static String orgAdminRefreshToken;
        private static String adminAccessToken;
        private static String adminRefreshToken;
        private static String customerAccessToken;
        private static String customerRefreshToken;
        private static UUID itemIdD;
        private static UUID deliveredOrderId;

        // ────────────────────────────────────────────────────────────
        // SETUP: Ensure there is at least one DELIVERED order for analytics
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(1)
        @DisplayName("0.1 Setup — Login ADMIN and create Item D for analytics data")
        void setupAdminAndData() throws Exception {
                LoginRequestDTO login = new LoginRequestDTO("enterprise", "anandchaniyara007@gmail.com",
                                "adminpassword");
                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andReturn();

                LoginResponseDTO resp = objectMapper.readValue(
                                loginResult.getResponse().getContentAsString(), LoginResponseDTO.class);
                adminAccessToken = resp.getAccessToken();
                adminRefreshToken = resp.getRefreshToken();

                // Create inventory item D
                String inventoryBody = "{\"inventory\": [{\"itemName\": \"Analytics Item D\", \"availableStock\": 100, \"reservedStock\": 0}]}";
                MvcResult invResult = mockMvc.perform(post("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(inventoryBody))
                                .andExpect(status().isCreated())
                                .andReturn();

                JsonNode items = objectMapper.readTree(invResult.getResponse().getContentAsString()).get("items");
                itemIdD = UUID.fromString(items.get(0).asText());

                // Set price
                AdminPricingDTO pricing = new AdminPricingDTO(itemIdD, new BigDecimal("50.00"), null);
                AdminPricingWrapperDTO wrapper = new AdminPricingWrapperDTO(List.of(pricing));
                mockMvc.perform(post("/api/admin/prices")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isCreated());

                System.out.println("Item D created: " + itemIdD + " (stock=100, price=50.00)");
        }

        @Test
        @Order(2)
        @DisplayName("0.2 Setup — CUSTOMER places order and ADMIN delivers it")
        void setupDeliveredOrder() throws Exception {
                // Login CUSTOMER
                LoginRequestDTO custLogin = new LoginRequestDTO("enterprise", "anandchaniyara007storage@gmail.com",
                                "customerpassword");
                MvcResult custResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(custLogin)))
                                .andExpect(status().isOk())
                                .andReturn();

                LoginResponseDTO custResp = objectMapper.readValue(
                                custResult.getResponse().getContentAsString(), LoginResponseDTO.class);
                customerAccessToken = custResp.getAccessToken();
                customerRefreshToken = custResp.getRefreshToken();

                // Ensure profile is complete
                CustomerProfileDTO profile = new CustomerProfileDTO(
                                "Test", "Customer", "9876543210", "123 Enterprise Blvd",
                                "anandchaniyara007storage@gmail.com");
                mockMvc.perform(put("/api/customer/profile")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(profile)))
                                .andExpect(status().isOk());

                // Place order for 3x Item D
                OrderItemDTO item = new OrderItemDTO(itemIdD, null, 3, null, null);
                OrderDTO orderReq = new OrderDTO(null, null, null, null, null, List.of(item), null);
                MvcResult orderResult = mockMvc.perform(post("/api/customer/orders")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderReq)))
                                .andExpect(status().isCreated())
                                .andReturn();

                OrderDTO orderResp = objectMapper.readValue(
                                orderResult.getResponse().getContentAsString(), OrderDTO.class);
                deliveredOrderId = orderResp.getOrderId();

                // Drive to DELIVERED
                for (String nextStatus : List.of("CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED")) {
                        BulkOrderStatusUpdateDTO upd = new BulkOrderStatusUpdateDTO(deliveredOrderId, nextStatus);
                        BulkOrderStatusUpdateWrapperDTO w = new BulkOrderStatusUpdateWrapperDTO(List.of(upd));
                        mockMvc.perform(put("/api/admin/orders/status")
                                        .header("Authorization", "Bearer " + adminAccessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(w)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.successes", hasSize(1)));
                }

                // Logout CUSTOMER and ADMIN (cleanup before ORG_ADMIN tests)
                RefreshTokenRequestDTO custLogout = new RefreshTokenRequestDTO(customerRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(custLogout)))
                                .andExpect(status().isOk());

                RefreshTokenRequestDTO adminLogout = new RefreshTokenRequestDTO(adminRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(adminLogout)))
                                .andExpect(status().isOk());

                System.out.println("Setup complete. Order " + deliveredOrderId
                                + " is DELIVERED. CUSTOMER and ADMIN logged out.");
        }

        // ────────────────────────────────────────────────────────────
        // 1. Login ORG_ADMIN
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(3)
        @DisplayName("1. Login ORG_ADMIN")
        void loginOrgAdmin() throws Exception {
                LoginRequestDTO login = new LoginRequestDTO("enterprise", "vpsciencememories@gmail.com",
                                "orgadminpassword");

                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken", notNullValue()))
                                .andExpect(jsonPath("$.role", is("ORG_ADMIN")))
                                .andReturn();

                LoginResponseDTO resp = objectMapper.readValue(
                                result.getResponse().getContentAsString(), LoginResponseDTO.class);
                orgAdminAccessToken = resp.getAccessToken();
                orgAdminRefreshToken = resp.getRefreshToken();

                System.out.println("ORG_ADMIN logged in.");
        }

        // ────────────────────────────────────────────────────────────
        // 2. Revenue Report — valid date range covering created orders
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(4)
        @DisplayName("2. Revenue report with valid date range")
        void revenueReportValidRange() throws Exception {
                String today = LocalDate.now().toString();
                String startDate = LocalDate.now().minusDays(30).toString();

                MvcResult result = mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .param("startdate", startDate)
                                .param("enddate", today))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startDate", notNullValue()))
                                .andExpect(jsonPath("$.endDate", notNullValue()))
                                .andExpect(jsonPath("$.totalRevenue", notNullValue()))
                                .andExpect(jsonPath("$.totalSoldItems", notNullValue()))
                                .andExpect(jsonPath("$.totalSoldQty", notNullValue()))
                                .andReturn();

                JsonNode report = objectMapper.readTree(result.getResponse().getContentAsString());
                System.out.println("Revenue Report: totalRevenue=" + report.get("totalRevenue") +
                                ", totalSoldItems=" + report.get("totalSoldItems") +
                                ", totalSoldQty=" + report.get("totalSoldQty"));
        }

        // ────────────────────────────────────────────────────────────
        // 3. Order Analytics — with itemName filter, orderStatus filter, pagination
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(5)
        @DisplayName("3.1 Order analytics with itemName filter")
        void orderAnalyticsWithItemNameFilter() throws Exception {
                String today = LocalDate.now().toString();
                String startDate = LocalDate.now().minusDays(30).toString();

                MvcResult result = mockMvc.perform(get("/api/org-admin/analytics/order-analytics")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .param("startdate", startDate)
                                .param("enddate", today)
                                .param("itemname", "Analytics Item D"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startDate", notNullValue()))
                                .andExpect(jsonPath("$.endDate", notNullValue()))
                                .andReturn();

                System.out.println("Order analytics (itemName filter): " + result.getResponse().getContentAsString());
        }

        @Test
        @Order(6)
        @DisplayName("3.2 Order analytics with orderStatus filter (DELIVERED)")
        void orderAnalyticsWithStatusFilter() throws Exception {
                String today = LocalDate.now().toString();
                String startDate = LocalDate.now().minusDays(30).toString();

                MvcResult result = mockMvc.perform(get("/api/org-admin/analytics/order-analytics")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .param("startdate", startDate)
                                .param("enddate", today)
                                .param("orderstatus", "DELIVERED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startDate", notNullValue()))
                                .andExpect(jsonPath("$.endDate", notNullValue()))
                                .andReturn();

                System.out.println("Order analytics (status=DELIVERED): " + result.getResponse().getContentAsString());
        }

        @Test
        @Order(7)
        @DisplayName("3.3 Order analytics with pagination (page=0, size=5)")
        void orderAnalyticsWithPagination() throws Exception {
                String today = LocalDate.now().toString();
                String startDate = LocalDate.now().minusDays(30).toString();

                MvcResult result = mockMvc.perform(get("/api/org-admin/analytics/order-analytics")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .param("startdate", startDate)
                                .param("enddate", today)
                                .param("page", "0")
                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startDate", notNullValue()))
                                .andExpect(jsonPath("$.endDate", notNullValue()))
                                .andReturn();

                System.out.println("Order analytics (paginated): " + result.getResponse().getContentAsString());
        }

        // ────────────────────────────────────────────────────────────
        // 4. Revenue Report with sendEmail=true and emailTo
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(8)
        @DisplayName("4. Revenue report with sendEmail=true and emailTo — assert dispatch or success")
        void revenueReportWithEmail() throws Exception {
                String today = LocalDate.now().toString();
                String startDate = LocalDate.now().minusDays(30).toString();

                MvcResult result = mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .param("startdate", startDate)
                                .param("enddate", today)
                                .param("sendEmail", "true")
                                .param("emailTo", "testreport@enterprise.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalRevenue", notNullValue()))
                                .andReturn();

                System.out.println("Revenue report with sendEmail=true dispatched successfully.");
                System.out.println("Response: " + result.getResponse().getContentAsString());
        }

        // ────────────────────────────────────────────────────────────
        // 5. Revenue Report with startDate after endDate → validation error
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(9)
        @DisplayName("5. Revenue report with startDate after endDate — expect 400 validation error")
        void revenueReportInvalidDateRange() throws Exception {
                MvcResult result = mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .param("startdate", "2026-12-31")
                                .param("enddate", "2026-01-01"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid Operation")))
                                .andExpect(jsonPath("$.message", containsString("startDate must be before endDate")))
                                .andReturn();

                System.out.println("Inverted date range → HTTP 400: " + result.getResponse().getContentAsString());
        }

        // ────────────────────────────────────────────────────────────
        // 6. Logout ORG_ADMIN
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(10)
        @DisplayName("6. Logout ORG_ADMIN")
        void logoutOrgAdmin() throws Exception {
                RefreshTokenRequestDTO logoutReq = new RefreshTokenRequestDTO(orgAdminRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutReq)))
                                .andExpect(status().isOk());

                System.out.println("ORG_ADMIN logged out. Analytics test suite complete.");
        }
}