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
 * Cancellation & Wrong-State Integration Test Suite
 *
 * Covers:
 * - Customer cancelling a DELIVERED order (wrong-state error)
 * - Customer cancelling a PENDING order (success + reserved stock released)
 * - Admin attempting to cancel a DELIVERED order via bulk update (rejected)
 *
 * Prerequisites: AuthIntegrationTest must have run first.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(3)
public class OrderCancellationIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        // Shared state
        private static String adminAccessToken;
        private static String adminRefreshToken;
        private static String customerAccessToken;
        private static String customerRefreshToken;
        private static UUID itemIdB;
        private static UUID deliveredOrderId;
        private static UUID pendingOrderId;
        private static int reservedStockBeforeCancel;

        // ────────────────────────────────────────────────────────────
        // SETUP: Create inventory, price, place order, advance to DELIVERED
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(1)
        @DisplayName("0.1 Setup — Login ADMIN and create inventory Item B (stock=30)")
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

                // Create inventory item B
                String inventoryBody = "{\"inventory\": [{\"itemName\": \"Gadget B\", \"availableStock\": 30, \"reservedStock\": 0}]}";
                MvcResult invResult = mockMvc.perform(post("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(inventoryBody))
                                .andExpect(status().isCreated())
                                .andReturn();

                JsonNode items = objectMapper.readTree(invResult.getResponse().getContentAsString()).get("items");
                itemIdB = UUID.fromString(items.get(0).asText());

                // Set price for Item B
                AdminPricingDTO pricing = new AdminPricingDTO(itemIdB, new BigDecimal("15.00"), null);
                AdminPricingWrapperDTO priceWrapper = new AdminPricingWrapperDTO(List.of(pricing));
                mockMvc.perform(post("/api/admin/prices")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(priceWrapper)))
                                .andExpect(status().isCreated());

                System.out.println("ADMIN logged in. Item B created: " + itemIdB + " with price 15.00");
        }

        @Test
        @Order(2)
        @DisplayName("0.2 Setup — Login CUSTOMER, place order, drive to DELIVERED")
        void setupCustomerAndDeliveredOrder() throws Exception {
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

                // Place order for 3x Item B
                OrderItemDTO item = new OrderItemDTO(itemIdB, null, 3, null, null);
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

                // Drive order through full lifecycle:
                // PENDING→CONFIRMED→PROCESSING→SHIPPED→DELIVERED
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

                System.out.println("Order " + deliveredOrderId + " driven to DELIVERED state.");
        }

        // ────────────────────────────────────────────────────────────
        // 1. CUSTOMER attempts to cancel a DELIVERED order → wrong-state error
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(3)
        @DisplayName("1. CUSTOMER cancel DELIVERED order — expect wrong-state error (400)")
        void customerCancelDeliveredOrder() throws Exception {
                MvcResult result = mockMvc.perform(put("/api/customer/orders/" + deliveredOrderId + "/cancel")
                                .header("Authorization", "Bearer " + customerAccessToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid Status Transition")))
                                .andExpect(jsonPath("$.message", containsString("Cannot cancel")))
                                .andReturn();

                System.out.println("CUSTOMER cancel DELIVERED → correctly rejected: " +
                                result.getResponse().getContentAsString());
        }

        // ────────────────────────────────────────────────────────────
        // 2. CUSTOMER places new order, cancels while PENDING → success + stock
        // released
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(4)
        @DisplayName("2.1 CUSTOMER places new order (PENDING) for 4x Item B")
        void customerPlaceNewOrder() throws Exception {
                // Capture reserved stock before
                MvcResult invBefore = mockMvc.perform(get("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("itemId", itemIdB.toString()))
                                .andExpect(status().isOk())
                                .andReturn();
                reservedStockBeforeCancel = objectMapper.readTree(
                                invBefore.getResponse().getContentAsString()).get("reservedStock").asInt();

                // Place order
                OrderItemDTO item = new OrderItemDTO(itemIdB, null, 4, null, null);
                OrderDTO orderReq = new OrderDTO(null, null, null, null, null, List.of(item), null);
                MvcResult orderResult = mockMvc.perform(post("/api/customer/orders")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderReq)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status", is("PENDING")))
                                .andReturn();

                OrderDTO resp = objectMapper.readValue(
                                orderResult.getResponse().getContentAsString(), OrderDTO.class);
                pendingOrderId = resp.getOrderId();

                System.out.println("New PENDING order placed: " + pendingOrderId);
        }

        @Test
        @Order(5)
        @DisplayName("2.2 CUSTOMER cancels PENDING order — success")
        void customerCancelPendingOrder() throws Exception {
                mockMvc.perform(put("/api/customer/orders/" + pendingOrderId + "/cancel")
                                .header("Authorization", "Bearer " + customerAccessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("CANCELLED")))
                                .andExpect(jsonPath("$.orderId", is(pendingOrderId.toString())));

                System.out.println("PENDING order " + pendingOrderId + " cancelled successfully.");
        }

        @Test
        @Order(6)
        @DisplayName("2.3 Verify reserved stock released after cancellation")
        void verifyReservedStockReleased() throws Exception {
                MvcResult invAfter = mockMvc.perform(get("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("itemId", itemIdB.toString()))
                                .andExpect(status().isOk())
                                .andReturn();

                int reservedStockAfter = objectMapper.readTree(
                                invAfter.getResponse().getContentAsString()).get("reservedStock").asInt();

                // Reserved stock should be back to what it was before the cancelled order
                Assertions.assertEquals(reservedStockBeforeCancel, reservedStockAfter,
                                "Reserved stock should be released after cancellation");

                System.out.println("Reserved stock restored: before=" + reservedStockBeforeCancel +
                                ", after=" + reservedStockAfter);
        }

        // ────────────────────────────────────────────────────────────
        // 3. Logout CUSTOMER
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(7)
        @DisplayName("3. Logout CUSTOMER")
        void logoutCustomer() throws Exception {
                RefreshTokenRequestDTO logoutReq = new RefreshTokenRequestDTO(customerRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutReq)))
                                .andExpect(status().isOk());

                System.out.println("CUSTOMER logged out.");
        }

        // ────────────────────────────────────────────────────────────
        // 4. ADMIN attempts to cancel a DELIVERED order → rejection
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(8)
        @DisplayName("4. ADMIN cancel DELIVERED order via bulk — expect rejection")
        void adminCancelDeliveredOrder() throws Exception {
                BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(deliveredOrderId, "CANCELLED");
                BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                MvcResult result = mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid Status Transition")))
                                .andExpect(jsonPath("$.message", containsString("Invalid transition")))
                                .andReturn();

                System.out.println("ADMIN cancel DELIVERED → correctly rejected: " +
                                result.getResponse().getContentAsString());
        }

        // ────────────────────────────────────────────────────────────
        // 5. Logout ADMIN
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(9)
        @DisplayName("5. Logout ADMIN")
        void logoutAdmin() throws Exception {
                RefreshTokenRequestDTO logoutReq = new RefreshTokenRequestDTO(adminRefreshToken);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutReq)))
                                .andExpect(status().isOk());

                System.out.println("ADMIN logged out.");
        }
}