package com.example.ordermgmt.integration;

import com.example.ordermgmt.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security & Access Control Integration Test Suite
 *
 * Covers:
 * 1. No-token access on protected endpoints → 403
 * 2. CUSTOMER hitting /api/admin/orders → 403
 * 3. ADMIN hitting /api/super-admin/organizations → 403
 * 4. Invalid UUID in inventory/order lookup → 400
 * 5. Reuse of logged-out tokens → 401
 *
 * Prerequisites: AuthIntegrationTest must have run first.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(5)
public class SecurityAccessControlIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        // Shared state
        private static String customerAccessToken;
        private static String customerRefreshToken;
        private static String adminAccessToken;
        private static String adminRefreshToken;
        private static String blacklistedCustomerToken;
        private static String blacklistedAdminToken;

        // ────────────────────────────────────────────────────────────
        // 1. No-token access on protected endpoints → 403
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(1)
        @DisplayName("1.1 No token — GET /api/admin/orders → 403")
        void noTokenAdminOrders() throws Exception {
                mockMvc.perform(get("/api/admin/orders"))
                                .andExpect(status().isForbidden());

                System.out.println("No token → /api/admin/orders → 403 confirmed.");
        }

        @Test
        @Order(2)
        @DisplayName("1.2 No token — GET /api/customer/orders → 403")
        void noTokenCustomerOrders() throws Exception {
                mockMvc.perform(get("/api/customer/orders"))
                                .andExpect(status().isForbidden());

                System.out.println("No token → /api/customer/orders → 403 confirmed.");
        }

        @Test
        @Order(3)
        @DisplayName("1.3 No token — GET /api/org-admin/analytics/revenue-report → 403")
        void noTokenAnalytics() throws Exception {
                mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                                .param("startdate", "2026-01-01")
                                .param("enddate", "2026-12-31"))
                                .andExpect(status().isForbidden());

                System.out.println("No token → /api/org-admin/analytics/revenue-report → 403 confirmed.");
        }

        @Test
        @Order(4)
        @DisplayName("1.4 No token — POST /api/customer/orders → 403")
        void noTokenPlaceOrder() throws Exception {
                mockMvc.perform(post("/api/customer/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"items\": [{\"itemId\": \"00000000-0000-0000-0000-000000000001\", \"quantity\": 1}]}"))
                                .andExpect(status().isForbidden());

                System.out.println("No token → POST /api/customer/orders → 403 confirmed.");
        }

        // ────────────────────────────────────────────────────────────
        // 2. CUSTOMER hitting admin APIs → 403
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(5)
        @DisplayName("2.0 Setup — Login CUSTOMER")
        void loginCustomer() throws Exception {
                LoginRequestDTO login = new LoginRequestDTO("enterprise", "anandchaniyara007storage@gmail.com",
                                "customerpassword");
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andReturn();

                LoginResponseDTO resp = objectMapper.readValue(
                                result.getResponse().getContentAsString(), LoginResponseDTO.class);
                customerAccessToken = resp.getAccessToken();
                customerRefreshToken = resp.getRefreshToken();

                System.out.println("CUSTOMER logged in.");
        }

        @Test
        @Order(6)
        @DisplayName("2.1 CUSTOMER → GET /api/admin/orders → 403 Forbidden")
        void customerAccessAdminOrders() throws Exception {
                mockMvc.perform(get("/api/admin/orders")
                                .header("Authorization", "Bearer " + customerAccessToken))
                                .andExpect(status().isForbidden());

                System.out.println("CUSTOMER → /api/admin/orders → 403 confirmed.");
        }

        @Test
        @Order(7)
        @DisplayName("2.2 CUSTOMER → PUT /api/admin/orders/status → 403 Forbidden")
        void customerAccessAdminBulkUpdate() throws Exception {
                String body = "{\"orders\": [{\"orderId\": \"00000000-0000-0000-0000-000000000001\", \"newStatus\": \"CONFIRMED\"}]}";
                mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isForbidden());

                System.out.println("CUSTOMER → PUT /api/admin/orders/status → 403 confirmed.");
        }

        @Test
        @Order(8)
        @DisplayName("2.3 CUSTOMER → GET /api/admin/inventory → 403 Forbidden")
        void customerAccessInventory() throws Exception {
                mockMvc.perform(get("/api/admin/inventory")
                                .header("Authorization", "Bearer " + customerAccessToken))
                                .andExpect(status().isForbidden());

                System.out.println("CUSTOMER → /api/admin/inventory → 403 confirmed.");
        }

        @Test
        @Order(9)
        @DisplayName("2.4 CUSTOMER → GET /api/org-admin/analytics/revenue-report → 403 Forbidden")
        void customerAccessAnalytics() throws Exception {
                mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .param("startdate", "2026-01-01")
                                .param("enddate", "2026-12-31"))
                                .andExpect(status().isForbidden());

                System.out.println("CUSTOMER → /api/org-admin/analytics/revenue-report → 403 confirmed.");
        }

        // ────────────────────────────────────────────────────────────
        // 3. ADMIN hitting super-admin APIs → 403
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(10)
        @DisplayName("3.0 Setup — Login ADMIN")
        void loginAdmin() throws Exception {
                LoginRequestDTO login = new LoginRequestDTO("enterprise", "anandchaniyara007@gmail.com",
                                "adminpassword");
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andReturn();

                LoginResponseDTO resp = objectMapper.readValue(
                                result.getResponse().getContentAsString(), LoginResponseDTO.class);
                adminAccessToken = resp.getAccessToken();
                adminRefreshToken = resp.getRefreshToken();

                System.out.println("ADMIN logged in.");
        }

        @Test
        @Order(11)
        @DisplayName("3.1 ADMIN → GET /api/super-admin/organizations → 403 Forbidden")
        void adminAccessSuperAdminOrgs() throws Exception {
                mockMvc.perform(get("/api/super-admin/organizations")
                                .header("Authorization", "Bearer " + adminAccessToken))
                                .andExpect(status().isForbidden());

                System.out.println("ADMIN → /api/super-admin/organizations → 403 confirmed.");
        }

        @Test
        @Order(12)
        @DisplayName("3.2 ADMIN → POST /api/super-admin/org-admins → 403 Forbidden")
        void adminAccessSuperAdminCreateOrgAdmin() throws Exception {
                CreateOrgAdminRequestDTO req = new CreateOrgAdminRequestDTO(
                                "hacker@evil.com", "badpass", UUID.randomUUID());
                mockMvc.perform(post("/api/super-admin/org-admins")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isForbidden());

                System.out.println("ADMIN → POST /api/super-admin/org-admins → 403 confirmed.");
        }

        // ────────────────────────────────────────────────────────────
        // 4. Invalid UUID in lookup → 400 (type mismatch)
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(13)
        @DisplayName("4.1 Invalid UUID format in inventory lookup → 400 Bad Request")
        void invalidUuidInventory() throws Exception {
                MvcResult result = mockMvc.perform(get("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("itemId", "not-a-valid-uuid"))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                System.out.println("Invalid UUID in inventory → 400: " + result.getResponse().getContentAsString());
        }

        @Test
        @Order(14)
        @DisplayName("4.2 Invalid UUID format in order lookup → 400 Bad Request")
        void invalidUuidOrder() throws Exception {
                MvcResult result = mockMvc.perform(get("/api/admin/orders")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("orderId", "definitely-not-uuid"))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                System.out.println("Invalid UUID in order → 400: " + result.getResponse().getContentAsString());
        }

        @Test
        @Order(15)
        @DisplayName("4.3 Non-existent valid UUID in order lookup → 404 Not Found")
        void nonExistentOrderId() throws Exception {
                UUID fakeId = UUID.randomUUID();
                MvcResult result = mockMvc.perform(get("/api/admin/orders")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("orderId", fakeId.toString()))
                                .andExpect(status().isNotFound())
                                .andReturn();

                System.out.println("Non-existent order UUID → 404: " + result.getResponse().getContentAsString());
        }

        // ────────────────────────────────────────────────────────────
        // 5. Reuse logged-out/expired tokens → 401
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(16)
        @DisplayName("5.1 Logout CUSTOMER and capture blacklisted token")
        void logoutCustomerAndCaptureToken() throws Exception {
                blacklistedCustomerToken = customerAccessToken;

                RefreshTokenRequestDTO logoutReq = new RefreshTokenRequestDTO(customerRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutReq)))
                                .andExpect(status().isOk());

                System.out.println("CUSTOMER logged out. Token captured for reuse test.");
        }

        @Test
        @Order(17)
        @DisplayName("5.2 Reuse blacklisted CUSTOMER token → 401")
        void reuseBlacklistedCustomerToken() throws Exception {
                mockMvc.perform(get("/api/customer/orders")
                                .header("Authorization", "Bearer " + blacklistedCustomerToken))
                                .andExpect(status().isUnauthorized());

                System.out.println("Blacklisted CUSTOMER token → 401 confirmed.");
        }

        @Test
        @Order(18)
        @DisplayName("5.3 Logout ADMIN and capture blacklisted token")
        void logoutAdminAndCaptureToken() throws Exception {
                blacklistedAdminToken = adminAccessToken;

                RefreshTokenRequestDTO logoutReq = new RefreshTokenRequestDTO(adminRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutReq)))
                                .andExpect(status().isOk());

                System.out.println("ADMIN logged out. Token captured for reuse test.");
        }

        @Test
        @Order(19)
        @DisplayName("5.4 Reuse blacklisted ADMIN token → 401")
        void reuseBlacklistedAdminToken() throws Exception {
                mockMvc.perform(get("/api/admin/orders")
                                .header("Authorization", "Bearer " + blacklistedAdminToken))
                                .andExpect(status().isUnauthorized());

                System.out.println("Blacklisted ADMIN token → 401 confirmed.");
        }

        @Test
        @Order(20)
        @DisplayName("5.5 Fabricated/expired token → 403")
        void fabricatedToken() throws Exception {
                String fakeToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlQHVzZXIuY29tIn0.fakeSignature";

                mockMvc.perform(get("/api/customer/orders")
                                .header("Authorization", "Bearer " + fakeToken))
                                .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/admin/orders")
                                .header("Authorization", "Bearer " + fakeToken))
                                .andExpect(status().isForbidden());

                System.out.println("Fabricated token → 403 confirmed on both customer and admin endpoints.");
        }
}