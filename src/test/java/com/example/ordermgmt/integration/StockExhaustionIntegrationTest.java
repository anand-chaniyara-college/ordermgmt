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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stock Exhaustion & Oversell Integration Test Suite
 *
 * Covers:
 * - Customer orders consuming all remaining stock of a SKU
 * - Subsequent order for same SKU fails with insufficient stock error
 * - Admin restocks the item
 * - Customer orders again with small qty to confirm recovery
 *
 * Prerequisites: AuthIntegrationTest must have run first.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(4)
public class StockExhaustionIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        // Shared state
        private static String adminAccessToken;
        private static String adminRefreshToken;
        private static String customerAccessToken;
        private static String customerRefreshToken;
        private static UUID itemIdC;

        // ────────────────────────────────────────────────────────────
        // SETUP: ADMIN creates a low-stock item (stock=10), sets price
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(1)
        @DisplayName("0.1 Setup — Login ADMIN and create Item C (stock=10)")
        void setupAdminAndInventory() throws Exception {
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

                // Create inventory item C with limited stock
                String inventoryBody = "{\"inventory\": [{\"itemName\": \"Scarce Item C\", \"availableStock\": 10, \"reservedStock\": 0}]}";
                MvcResult invResult = mockMvc.perform(post("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(inventoryBody))
                                .andExpect(status().isCreated())
                                .andReturn();

                JsonNode items = objectMapper.readTree(invResult.getResponse().getContentAsString()).get("items");
                itemIdC = UUID.fromString(items.get(0).asText());

                // Set price for Item C
                AdminPricingDTO pricing = new AdminPricingDTO(itemIdC, new BigDecimal("100.00"), null);
                AdminPricingWrapperDTO wrapper = new AdminPricingWrapperDTO(List.of(pricing));
                mockMvc.perform(post("/api/admin/prices")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isCreated());

                System.out.println("Item C created: " + itemIdC + " (stock=10, price=100.00)");
        }

        @Test
        @Order(2)
        @DisplayName("0.2 Setup — Login CUSTOMER and complete profile")
        void setupCustomer() throws Exception {
                LoginRequestDTO login = new LoginRequestDTO("enterprise", "anandchaniyara007storage@gmail.com",
                                "customerpassword");
                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andReturn();

                LoginResponseDTO resp = objectMapper.readValue(
                                loginResult.getResponse().getContentAsString(), LoginResponseDTO.class);
                customerAccessToken = resp.getAccessToken();
                customerRefreshToken = resp.getRefreshToken();

                // Ensure profile is complete
                CustomerProfileDTO profile = new CustomerProfileDTO(
                                "Test", "Customer", "9876543210", "123 Enterprise Blvd",
                                "anandchaniyara007storage@gmail.com");
                mockMvc.perform(put("/api/customer/profile")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(profile)))
                                .andExpect(status().isOk());

                System.out.println("CUSTOMER logged in and profile ensured.");
        }

        // ────────────────────────────────────────────────────────────
        // 1. CUSTOMER consumes all remaining stock of Item C
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(3)
        @DisplayName("1. CUSTOMER places order consuming all stock of Item C (qty=10)")
        void customerConsumeAllStock() throws Exception {
                OrderItemDTO item = new OrderItemDTO(itemIdC, null, 10, null, null);
                OrderDTO orderReq = new OrderDTO(null, null, null, null, null, List.of(item), null);

                mockMvc.perform(post("/api/customer/orders")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderReq)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status", is("PENDING")))
                                .andExpect(jsonPath("$.items[0].quantity", is(10)));

                System.out.println("Customer ordered 10x Item C — all stock now reserved.");
        }

        // ────────────────────────────────────────────────────────────
        // 2. Attempt another order for same SKU → insufficient stock
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(4)
        @DisplayName("2. CUSTOMER orders Item C again → insufficient stock error (400)")
        void customerOversellAttempt() throws Exception {
                OrderItemDTO item = new OrderItemDTO(itemIdC, null, 1, null, null);
                OrderDTO orderReq = new OrderDTO(null, null, null, null, null, List.of(item), null);

                MvcResult result = mockMvc.perform(post("/api/customer/orders")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderReq)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Insufficient Stock")))
                                .andExpect(jsonPath("$.message", containsString("Insufficient stock")))
                                .andReturn();

                System.out.println("Oversell correctly blocked: " + result.getResponse().getContentAsString());
        }

        // ────────────────────────────────────────────────────────────
        // 3. Logout CUSTOMER
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(5)
        @DisplayName("3. Logout CUSTOMER")
        void logoutCustomerFirst() throws Exception {
                RefreshTokenRequestDTO logoutReq = new RefreshTokenRequestDTO(customerRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutReq)))
                                .andExpect(status().isOk());

                System.out.println("CUSTOMER logged out.");
        }

        // ────────────────────────────────────────────────────────────
        // 4. ADMIN adds stock to Item C, then logs out
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(6)
        @DisplayName("4. ADMIN restocks Item C (add 20 units)")
        void adminAddStock() throws Exception {
                AddStockRequestDTO addStock = new AddStockRequestDTO(itemIdC, 20);
                AddStockWrapperDTO wrapper = new AddStockWrapperDTO(List.of(addStock));

                mockMvc.perform(post("/api/admin/inventory/addstock")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items", hasSize(1)));

                // Verify new stock level
                MvcResult invResult = mockMvc.perform(get("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("itemId", itemIdC.toString()))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode inv = objectMapper.readTree(invResult.getResponse().getContentAsString());
                System.out.println("After restock: available=" + inv.get("availableStock") +
                                ", reserved=" + inv.get("reservedStock"));
        }

        @Test
        @Order(7)
        @DisplayName("4.1 Logout ADMIN")
        void logoutAdmin() throws Exception {
                RefreshTokenRequestDTO logoutReq = new RefreshTokenRequestDTO(adminRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutReq)))
                                .andExpect(status().isOk());

                System.out.println("ADMIN logged out.");
        }

        // ────────────────────────────────────────────────────────────
        // 5. CUSTOMER logs in again, places small order to confirm recovery
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(8)
        @DisplayName("5. CUSTOMER logs in again and orders small qty to confirm stock recovery")
        void customerRecoveryOrder() throws Exception {
                // Login again
                LoginRequestDTO login = new LoginRequestDTO("enterprise", "anandchaniyara007storage@gmail.com",
                                "customerpassword");
                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andReturn();

                LoginResponseDTO resp = objectMapper.readValue(
                                loginResult.getResponse().getContentAsString(), LoginResponseDTO.class);
                customerAccessToken = resp.getAccessToken();
                customerRefreshToken = resp.getRefreshToken();

                // Place small order (qty=2) — should succeed now
                OrderItemDTO item = new OrderItemDTO(itemIdC, null, 2, null, null);
                OrderDTO orderReq = new OrderDTO(null, null, null, null, null, List.of(item), null);

                mockMvc.perform(post("/api/customer/orders")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderReq)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status", is("PENDING")))
                                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                                .andExpect(jsonPath("$.totalAmount", notNullValue()));

                System.out.println("Stock recovery confirmed — small order placed successfully.");
        }

        @Test
        @Order(9)
        @DisplayName("5.1 Logout CUSTOMER")
        void logoutCustomerFinal() throws Exception {
                RefreshTokenRequestDTO logoutReq = new RefreshTokenRequestDTO(customerRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutReq)))
                                .andExpect(status().isOk());

                System.out.println("CUSTOMER logged out. Stock exhaustion test suite complete.");
        }
}