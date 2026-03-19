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
 * Order Lifecycle Integration Test Suite
 *
 * Covers the full admin-driven order lifecycle:
 * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 * Including negative paths for illegal status transitions,
 * timestamp verification, and stock reservation/finalization behavior.
 *
 * Prerequisites: AuthIntegrationTest must have run first to create
 * the 'enterprise' org, ORG_ADMIN, ADMIN, and CUSTOMER users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(2)
public class OrderLifecycleIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        // Shared state across ordered tests
        private static String adminAccessToken;
        private static String adminRefreshToken;
        private static String customerAccessToken;
        private static String customerRefreshToken;
        private static UUID itemIdA;
        private static UUID orderId;
        private static int initialAvailableStock;

        // ────────────────────────────────────────────────────────────
        // SETUP: Login ADMIN, create inventory + pricing, login CUSTOMER,
        // set up profile, place an order
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(1)
        @DisplayName("0.1 Setup — Login ADMIN")
        void setupLoginAdmin() throws Exception {
                LoginRequestDTO login = new LoginRequestDTO("enterprise", "anandchaniyara007@gmail.com",
                                "adminpassword");

                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken", notNullValue()))
                                .andExpect(jsonPath("$.role", is("ADMIN")))
                                .andReturn();

                LoginResponseDTO response = objectMapper.readValue(
                                result.getResponse().getContentAsString(), LoginResponseDTO.class);
                adminAccessToken = response.getAccessToken();
                adminRefreshToken = response.getRefreshToken();

                System.out.println("ADMIN logged in successfully.");
        }

        @Test
        @Order(2)
        @DisplayName("0.2 Setup — Create Inventory Item A with stock=50")
        void setupCreateInventory() throws Exception {
                // Add inventory item
                String inventoryBody = "{\"inventory\": [{\"itemName\": \"Widget A\", \"availableStock\": 50, \"reservedStock\": 0}]}";

                MvcResult result = mockMvc.perform(post("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(inventoryBody))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.items", hasSize(1)))
                                .andReturn();

                JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("items");
                itemIdA = UUID.fromString(items.get(0).asText());
                initialAvailableStock = 50;

                System.out.println("Inventory Item A created: " + itemIdA);
        }

        @Test
        @Order(3)
        @DisplayName("0.3 Setup — Set price for Item A (unit price = 25.00)")
        void setupSetPrice() throws Exception {
                AdminPricingDTO pricing = new AdminPricingDTO(itemIdA, new BigDecimal("25.00"), null);
                AdminPricingWrapperDTO wrapper = new AdminPricingWrapperDTO(List.of(pricing));

                mockMvc.perform(post("/api/admin/prices")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message", containsString("successfully")));

                System.out.println("Price set for Item A.");
        }

        @Test
        @Order(4)
        @DisplayName("0.4 Setup — Login CUSTOMER and complete profile")
        void setupLoginCustomerAndProfile() throws Exception {
                LoginRequestDTO login = new LoginRequestDTO("enterprise", "anandchaniyara007storage@gmail.com",
                                "customerpassword");

                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken", notNullValue()))
                                .andReturn();

                LoginResponseDTO response = objectMapper.readValue(
                                result.getResponse().getContentAsString(), LoginResponseDTO.class);
                customerAccessToken = response.getAccessToken();
                customerRefreshToken = response.getRefreshToken();

                // Complete customer profile (required to place orders)
                CustomerProfileDTO profile = new CustomerProfileDTO(
                                "Test", "Customer", "9876543210", "123 Enterprise Blvd",
                                "anandchaniyara007storage@gmail.com");

                mockMvc.perform(put("/api/customer/profile")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(profile)))
                                .andExpect(status().isOk());

                System.out.println("CUSTOMER logged in and profile completed.");
        }

        @Test
        @Order(5)
        @DisplayName("0.5 Setup — Customer places order for 5x Item A")
        void setupPlaceOrder() throws Exception {
                OrderItemDTO item = new OrderItemDTO(itemIdA, null, 5, null, null);
                OrderDTO orderRequest = new OrderDTO(null, null, null, null, null, List.of(item), null);

                MvcResult result = mockMvc.perform(post("/api/customer/orders")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.orderId", notNullValue()))
                                .andExpect(jsonPath("$.status", is("PENDING")))
                                .andExpect(jsonPath("$.items", hasSize(1)))
                                .andExpect(jsonPath("$.items[0].itemId", is(itemIdA.toString())))
                                .andExpect(jsonPath("$.items[0].quantity", is(5)))
                                .andExpect(jsonPath("$.totalAmount", notNullValue()))
                                .andExpect(jsonPath("$.createdTimestamp", notNullValue()))
                                .andReturn();

                OrderDTO response = objectMapper.readValue(
                                result.getResponse().getContentAsString(), OrderDTO.class);
                orderId = response.getOrderId();

                System.out.println("Order placed: " + orderId + " | Total: " + response.getTotalAmount());
        }

        // ────────────────────────────────────────────────────────────
        // 1. ADMIN sees the new PENDING order
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(6)
        @DisplayName("1.1 ADMIN — GET orders contains new PENDING order")
        void adminGetOrdersContainsPending() throws Exception {
                MvcResult result = mockMvc.perform(get("/api/admin/orders")
                                .header("Authorization", "Bearer " + adminAccessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orders", not(empty())))
                                .andReturn();

                // Verify our specific order is present and PENDING
                JsonNode orders = objectMapper.readTree(result.getResponse().getContentAsString()).get("orders");
                boolean found = false;
                for (JsonNode o : orders) {
                        if (o.get("orderId").asText().equals(orderId.toString())) {
                                Assertions.assertEquals("PENDING", o.get("status").asText());
                                found = true;
                                break;
                        }
                }
                Assertions.assertTrue(found, "Order " + orderId + " should be present in admin order list");

                System.out.println("ADMIN confirmed order " + orderId + " is PENDING.");
        }

        @Test
        @Order(7)
        @DisplayName("1.2 Verify stock moved from available to reserved after order placement")
        void verifyReservedStockAfterOrder() throws Exception {
                MvcResult result = mockMvc.perform(get("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("itemId", itemIdA.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reservedStock", greaterThanOrEqualTo(5)))
                                .andReturn();

                JsonNode inv = objectMapper.readTree(result.getResponse().getContentAsString());
                Assertions.assertEquals(initialAvailableStock - 5, inv.get("availableStock").asInt(),
                                "Available stock should be reduced when the order is placed");
                System.out.println("Stock after PENDING: available=" + inv.get("availableStock") +
                                ", reserved=" + inv.get("reservedStock"));
        }

        // ────────────────────────────────────────────────────────────
        // 2. Bulk status transitions: PENDING→CONFIRMED→PROCESSING→SHIPPED→DELIVERED
        // Plus illegal transition attempts
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(8)
        @DisplayName("2.1 Bulk transition PENDING → CONFIRMED (success)")
        void bulkTransitionPendingToConfirmed() throws Exception {
                BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(orderId, "CONFIRMED");
                BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.successes", hasSize(1)))
                                .andExpect(jsonPath("$.successes[0].status", is("CONFIRMED")))
                                .andExpect(jsonPath("$.successes[0].updatedTimestamp", notNullValue()))
                                .andExpect(jsonPath("$.failures", empty()));

                System.out.println("Transition PENDING → CONFIRMED succeeded.");
        }

        @Test
        @Order(9)
        @DisplayName("2.2 Illegal: CONFIRMED → DELIVERED (skip steps) — expect failure in bulk result")
        void bulkTransitionIllegalConfirmedToDelivered() throws Exception {
                BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(orderId, "DELIVERED");
                BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid Status Transition")))
                                .andExpect(jsonPath("$.message", containsString("Invalid transition")));

                System.out.println("Illegal transition CONFIRMED → DELIVERED correctly rejected in bulk.");
        }

        @Test
        @Order(10)
        @DisplayName("2.3 Bulk transition CONFIRMED → PROCESSING (success)")
        void bulkTransitionConfirmedToProcessing() throws Exception {
                BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(orderId, "PROCESSING");
                BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.successes", hasSize(1)))
                                .andExpect(jsonPath("$.successes[0].status", is("PROCESSING")))
                                .andExpect(jsonPath("$.failures", empty()));

                System.out.println("Transition CONFIRMED → PROCESSING succeeded.");
        }

        @Test
        @Order(11)
        @DisplayName("2.4 Illegal: PROCESSING → PENDING (backward) — expect failure")
        void bulkTransitionIllegalProcessingToPending() throws Exception {
                BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(orderId, "PENDING");
                BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid Status Transition")))
                                .andExpect(jsonPath("$.message", containsString("Invalid transition")));

                System.out.println("Illegal backward transition PROCESSING → PENDING correctly rejected.");
        }

        @Test
        @Order(12)
        @DisplayName("2.5 Bulk transition PROCESSING → SHIPPED (success)")
        void bulkTransitionProcessingToShipped() throws Exception {
                BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(orderId, "SHIPPED");
                BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.successes", hasSize(1)))
                                .andExpect(jsonPath("$.successes[0].status", is("SHIPPED")))
                                .andExpect(jsonPath("$.failures", empty()));

                System.out.println("Transition PROCESSING → SHIPPED succeeded.");
        }

        @Test
        @Order(13)
        @DisplayName("2.6 Illegal: SHIPPED → CANCELLED — expect failure")
        void bulkTransitionIllegalShippedToCancelled() throws Exception {
                BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(orderId, "CANCELLED");
                BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid Status Transition")))
                                .andExpect(jsonPath("$.message", containsString("Invalid transition")));

                System.out.println("Illegal transition SHIPPED → CANCELLED correctly rejected.");
        }

        @Test
        @Order(14)
        @DisplayName("2.7 Bulk transition SHIPPED → DELIVERED (success)")
        void bulkTransitionShippedToDelivered() throws Exception {
                BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(orderId, "DELIVERED");
                BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                MvcResult result = mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.successes", hasSize(1)))
                                .andExpect(jsonPath("$.successes[0].status", is("DELIVERED")))
                                .andExpect(jsonPath("$.successes[0].updatedTimestamp", notNullValue()))
                                .andExpect(jsonPath("$.failures", empty()))
                                .andReturn();

                System.out.println("Transition SHIPPED → DELIVERED succeeded. Full lifecycle complete.");
                System.out.println("Response: " + result.getResponse().getContentAsString());
        }

        @Test
        @Order(15)
        @DisplayName("2.8 Illegal: DELIVERED → any status — expect failure (terminal state)")
        void bulkTransitionIllegalDeliveredToConfirmed() throws Exception {
                BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(orderId, "CONFIRMED");
                BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                mockMvc.perform(put("/api/admin/orders/status")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrapper)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid Status Transition")))
                                .andExpect(jsonPath("$.message", containsString("Invalid transition")));

                System.out.println("DELIVERED is terminal — further transitions correctly rejected.");
        }

        // ────────────────────────────────────────────────────────────
        // 3. Verify timestamps and stock finalization after DELIVERED
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(16)
        @DisplayName("3.1 Confirm timestamps updated on final order state")
        void confirmTimestampsUpdated() throws Exception {
                mockMvc.perform(get("/api/admin/orders")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("orderId", orderId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orders[0].status", is("DELIVERED")))
                                .andExpect(jsonPath("$.orders[0].createdTimestamp", notNullValue()))
                                .andExpect(jsonPath("$.orders[0].updatedTimestamp", notNullValue()));

                System.out.println("Timestamps verified on DELIVERED order.");
        }

        @Test
        @Order(17)
        @DisplayName("3.2 Confirm delivery releases reserved stock without changing available stock again")
        void confirmStockDeductedAfterDelivery() throws Exception {
                MvcResult result = mockMvc.perform(get("/api/admin/inventory")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("itemId", itemIdA.toString()))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode inv = objectMapper.readTree(result.getResponse().getContentAsString());
                int currentAvailable = inv.get("availableStock").asInt();
                int currentReserved = inv.get("reservedStock").asInt();

                // Under the reservation model, available stock was already reduced at order placement.
                Assertions.assertEquals(initialAvailableStock - 5, currentAvailable,
                                "Available stock should remain at the post-reservation level after delivery");
                Assertions.assertEquals(0, currentReserved,
                                "Reserved stock should be released after delivery");

                System.out.println("Stock after DELIVERED: available=" + currentAvailable +
                                ", reserved=" + currentReserved +
                                " (initial available was " + initialAvailableStock + ")");
        }

        // ────────────────────────────────────────────────────────────
        // 4. Logout ADMIN
        // ────────────────────────────────────────────────────────────

        @Test
        @Order(18)
        @DisplayName("4.1 Logout CUSTOMER")
        void logoutCustomer() throws Exception {
                RefreshTokenRequestDTO logoutReq = new RefreshTokenRequestDTO(customerRefreshToken);

                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + customerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutReq)))
                                .andExpect(status().isOk());

                System.out.println("CUSTOMER logged out.");
        }

        @Test
        @Order(19)
        @DisplayName("4.2 Logout ADMIN")
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
