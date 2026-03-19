package com.example.ordermgmt.integration;

import com.example.ordermgmt.dto.*;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.repository.OrdersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h3>Concurrency Problem: Order Status Race (Admin vs. Customer)</h3>
 * <p>
 * <b>Scenario:</b> An Order is in 'PENDING' status. A Customer attempts to
 * CANCEL the order
 * via the Customer API, while an Admin simultaneously attempts to update the
 * status to
 * 'CONFIRMED' via the Admin API.
 * </p>
 * <p>
 * <b>The Risk (The Bug):</b> If the system lacks Optimistic Locking (@Version)
 * or
 * Pessimistic Locking on the 'Orders' entity, both threads can read the
 * 'PENDING' state.
 * Thread A (Admin) updates it to 'CONFIRMED' and commits. Thread B (Customer)
 * updates it
 * to 'CANCELLED' immediately after, overwriting the Admin's fulfillment. This
 * results
 * in a "Cancelled" order that may have already triggered physical shipping or
 * inventory deductions.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
public class OrderStatusRaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrdersRepository ordersRepository;

    private String adminToken;
    private String customerToken;
    private UUID itemId;

    @BeforeEach
    void setupData() throws Exception {
        // Step 1: Login Admin
        LoginRequestDTO adminLogin = new LoginRequestDTO("enterprise", "anandchaniyara007@gmail.com", "adminpassword");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString()).get("accessToken").asText();

        // Step 2: Create a fresh inventory item
        String inventoryBody = "{\"inventory\": [{\"itemName\": \"Status Race Widget\", \"availableStock\": 1000, \"reservedStock\": 0}]}";
        MvcResult invResult = mockMvc.perform(post("/api/admin/inventory")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inventoryBody))
                .andExpect(status().isCreated())
                .andReturn();
        itemId = UUID.fromString(
                objectMapper.readTree(invResult.getResponse().getContentAsString()).get("items").get(0).asText());

        // Step 3: Set initial price
        AdminPricingDTO pricing = new AdminPricingDTO(itemId, new BigDecimal("100.00"), null);
        mockMvc.perform(post("/api/admin/prices")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminPricingWrapperDTO(List.of(pricing)))))
                .andExpect(status().isCreated());

        // Step 4: Login Customer and Setup Profile
        LoginRequestDTO customerLogin = new LoginRequestDTO("enterprise", "anandchaniyara007storage@gmail.com",
                "customerpassword");
        MvcResult customerResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerLogin)))
                .andExpect(status().isOk())
                .andReturn();
        customerToken = objectMapper.readTree(customerResult.getResponse().getContentAsString()).get("accessToken")
                .asText();

        CustomerProfileDTO profile = new CustomerProfileDTO("Status", "Race", "1234567890", "Race Track",
                "anandchaniyara007storage@gmail.com");
        mockMvc.perform(put("/api/customer/profile")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk());
    }

    /**
     * Test Case: Simulate simultaneous CANCEL (Customer) and CONFIRM (Admin)
     * requests.
     * Expected: One request should succeed (200 OK) and the other should fail with
     * a Conflict (409) or Invalid Transition (400) error. The final database state
     * must remain consistent.
     */
    @Test
    void testSimultaneousStatusUpdates_ShouldMaintainConsistency() throws Exception {
        int numberOfOrders = 30; // High count to increase race probability
        List<UUID> orderIds = new ArrayList<>();

        // 1. Create multiple PENDING orders to race on
        for (int i = 0; i < numberOfOrders; i++) {
            OrderItemDTO item = new OrderItemDTO(itemId, null, 1, null, null);
            OrderDTO orderRequest = new OrderDTO(null, null, null, null, null, List.of(item), null);

            MvcResult result = mockMvc.perform(post("/api/customer/orders")
                    .header("Authorization", "Bearer " + customerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            OrderDTO response = objectMapper.readValue(result.getResponse().getContentAsString(),
                    OrderDTO.class);
            orderIds.add(response.getOrderId());
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfOrders * 2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 2. Launch the Race
        for (UUID orderId : orderIds) {
            // Task: Customer Cancellation
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Interleave with very tiny random sleep to shift the race window
                    Thread.sleep(ThreadLocalRandom.current().nextInt(0, 5));

                    mockMvc.perform(put("/api/customer/orders/" + orderId + "/cancel")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(result -> {
                                int status = result.getResponse().getStatus();
                                if (status == 200) {
                                    successCount.incrementAndGet();
                                } else if (status == 400 || status == 409) {
                                    failureCount.incrementAndGet();
                                }
                            });
                } catch (Exception e) {
                    // Failures in MockMvc perform expectations don't throw InterruptedException
                    // here
                } finally {
                    finishLatch.countDown();
                }
            });

            // Task: Admin Confirmation
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Interleave with very tiny random sleep to shift the race window
                    Thread.sleep(ThreadLocalRandom.current().nextInt(0, 5));

                    BulkOrderStatusUpdateDTO update = new BulkOrderStatusUpdateDTO(orderId,
                            OrderStatus.CONFIRMED.name());
                    BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO(List.of(update));

                    mockMvc.perform(put("/api/admin/orders/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrapper)))
                            .andExpect(result -> {
                                int status = result.getResponse().getStatus();
                                // Note: Admin bulk update return 200 even if some fail,
                                // so we'd need to check the result body for actual success.
                                if (status == 200) {
                                    String content = result.getResponse().getContentAsString();
                                    BulkOrderUpdateResultDTO resultDto = objectMapper.readValue(content,
                                            BulkOrderUpdateResultDTO.class);
                                    if (!resultDto.getSuccesses().isEmpty()) {
                                        successCount.incrementAndGet();
                                    } else if (!resultDto.getFailures().isEmpty()) {
                                        failureCount.incrementAndGet();
                                    }
                                }
                            });
                } catch (Exception e) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        long startTime = System.currentTimeMillis();
        startLatch.countDown(); // Race!

        boolean completed = finishLatch.await(1, TimeUnit.MINUTES);
        long duration = System.currentTimeMillis() - startTime;

        executor.shutdown();
        if (!completed || !executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        // ────────────────────────────────────────────────────────────
        // ASSERTIONS AND VERIFICATION
        // ────────────────────────────────────────────────────────────

        Assertions.assertTrue(completed, "Test timed out");

        System.out.println("Status Race simulation finished in " + duration + "ms.");
        System.out.println("Total Requests: " + (numberOfOrders * 2));
        System.out.println("Successes: " + successCount.get());
        System.out.println("Failures (Conflicts): " + failureCount.get());

        int customerWins = 0;
        int adminWins = 0;

        for (UUID orderId : orderIds) {
            Orders order = ordersRepository.findById(orderId).orElseThrow();
            String finalStatus = order.getStatus().getStatusName();

            // CRITICAL CHECK: Binary Status Outcome
            // The order must be either CONFIRMED or CANCELLED.
            Assertions.assertTrue(
                    finalStatus.equals(OrderStatus.CONFIRMED.name()) ||
                            finalStatus.equals(OrderStatus.CANCELLED.name()),
                    "Order " + orderId + " in invalid state: " + finalStatus);

            if (finalStatus.equals(OrderStatus.CONFIRMED.name())) {
                adminWins++;
            } else {
                customerWins++;
            }
        }

        System.out
                .println("Race Distribution: Admin(Confirmed)=" + adminWins + ", Customer(Cancelled)=" + customerWins);

        // Each order must have exactly ONE winner.
        Assertions.assertEquals(numberOfOrders, adminWins + customerWins,
                "Concurrency leak! Some orders didn't process correctly.");
        Assertions.assertEquals(numberOfOrders, successCount.get(), "Total successes should match number of orders");
        Assertions.assertEquals(numberOfOrders, failureCount.get(), "Total failures should match number of orders");

        System.out.println("Verified " + numberOfOrders + " orders for state consistency under high concurrency.");
    }
}
/*
 * I have verified the results of your
 * 
 * OrderStatusRaceIntegrationTest
 * by querying the database directly for the orders linked to the
 * "Status Race Widget".
 * 
 * Here is the breakdown of what actually occurred during the race:
 * 
 * 1. Order Status Final Counts
 * I found exactly 30 orders created for this test item. The final distribution
 * is:
 * 
 * Final Status Count Who Won?
 * CANCELLED 21 The Customer was faster/successful in cancelling.
 * CONFIRMED 8 The Admin was successful in confirming.
 * PENDING 1 The Race Condition Bug (Collision).
 * 2. Inventory Side-Effect (The "Stock Leak")
 * This is where the bug becomes financially visible. I checked the
 * inventory_item table for your widget:
 * 
 * Target: reservedstock should be exactly 8 (matching the 8 Confirmed orders).
 * Actual: reservedstock is 9.
 * The Logic: The 1 orphaned PENDING order is holding 1 unit of stock hostage.
 * Since the status never moved to a terminal state, the inventory was never
 * released (for cancellation) nor deducted (for confirmation).
 * 
 * 3. Conclusion: Expected vs. Received
 * Expected: 30/30 orders reach a terminal state (0 Pending).
 * Received: 29/30 terminal.
 * The "Why": For order c0a8085e-9d04-1016-819d-04d04a230009, both the Admin and
 * Customer requests likely collided so perfectly at the database lock level
 * that the transaction rolled back. Without a Retry Mechanism or Optimistic
 * Locking (@Version) handling, the system simply "gave up" on that order to be
 * safe, leaving it in an inconsistent business state.
 * 
 * -------------------------
 * Implementation Plan: Resolving Order Status Race Conditions
 * Problem Statement
 * The
 * OrderStatusRaceIntegrationTest
 * and
 * CronVsHumanRaceIntegrationTest
 * confirmed a Lost Update Race Condition and Orphaned Status Bug.
 * 
 * Issue 1: The "Customer" path (
 * cancelOrder
 * ) lacks pessimistic locking, while the "Admin" path uses it. This creates an
 * asymmetric race where both actors read the same state and potentially
 * deadlock.
 * Issue 2: When a database-level collision occurs, the lack of a retry
 * mechanism results in a "Pending" order being stuck, causing inventory leaks.
 * Proposed Architecture: The "Dual-Locking + Retry" Strategy
 * To ensure 100% data consistency and reliability under high concurrency, we
 * will implement three layers of protection:
 * 
 * Phase 1: Data Integrity Layer (Optimistic Locking)
 * Introducing a version field to the
 * Orders
 * entity acts as a fail-safe at the database level.
 * 
 * Database Migration: Add a version column to the ORDERS table.
 * sql
 * ALTER TABLE itest_ordermgmt.orders ADD COLUMN version BIGINT DEFAULT 0;
 * Entity Modification: Update
 * Orders.java
 * to include the @Version annotation.
 * java
 * 
 * @Version
 * 
 * @Column(name = "version")
 * private Long version;
 * Phase 2: Locking Protocol Synchronization
 * Currently, only the Admin path locks the order. We must synchronize the
 * protocol so both actors respect the same lock.
 * 
 * Refactor OrderServiceImpl.cancelOrder: Change the read method from
 * findById()
 * to
 * findByIdWithLock()
 * .
 * java
 * // Before
 * Orders order = getOrderOrThrow(orderId);
 * // After
 * Orders order = ordersRepository.findByIdWithLock(orderId)
 * .orElseThrow(() -> new OrderNotFoundException("Order not found: " +
 * orderId));
 * Phase 3: Reliability Layer (Transaction Retries)
 * We will introduce Spring Retry to handle the rare nanosecond collisions where
 * a LockTimeout or an OptimisticLockingFailureException might occur.
 * 
 * Dependency Requirement: Ensure spring-retry and spring-boot-starter-aop are
 * in
 * pom.xml
 * .
 * Service-Level Retry: Annotate
 * updateOrdersStatus
 * and
 * cancelOrder
 * with @Retryable.
 * java
 * 
 * @Retryable(
 * value = {OptimisticLockingFailureException.class,
 * CannotAcquireLockException.class},
 * maxAttempts = 3,
 * backoff = @Backoff(delay = 100, multiplier = 2)
 * )
 * Success Criteria (Verification)
 * After implementation, re-running the
 * OrderStatusRaceIntegrationTest
 * should yield:
 * 
 * Zero "PENDING" Orders: All 30 orders must reach a terminal state (CONFIRMED
 * or CANCELLED).
 * Total Count Consistency: Successes + Conflicts must exactly equal 60 total
 * attempts (30 per actor).
 * Correct Inventory Reservation: reservedstock in the database must exactly
 * match the number of CONFIRMED orders (no stock leaks).
 */
