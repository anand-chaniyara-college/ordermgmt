package com.example.ordermgmt;

import com.example.ordermgmt.dto.analytics.ItemSalesReportDTO;
import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.entity.*;
import com.example.ordermgmt.repository.*;
import com.example.ordermgmt.service.AdminAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminAnalyticsIntegrationTest {

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderStatusLookupRepository statusRepository;

    @Autowired
    private AppUserRepository userRepository;

    private OrderStatusLookup deliveredStatus;
    private OrderStatusLookup pendingStatus;
    private InventoryItem item;
    private Customer customer;

    @BeforeEach
    void setUp() {
        deliveredStatus = statusRepository.findByStatusName("DELIVERED")
                .orElseGet(() -> {
                    OrderStatusLookup s = new OrderStatusLookup();
                    s.setStatusId(5);
                    s.setStatusName("DELIVERED");
                    return statusRepository.save(s);
                });

        pendingStatus = statusRepository.findByStatusName("PENDING")
                .orElseGet(() -> {
                    OrderStatusLookup s = new OrderStatusLookup();
                    s.setStatusId(1);
                    s.setStatusName("PENDING");
                    return statusRepository.save(s);
                });

        AppUser user = new AppUser();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail("test" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("hash");
        user.setIsActive(true);
        user = userRepository.save(user);

        customer = new Customer();
        customer.setCustomerId(UUID.randomUUID().toString());
        customer.setAppUser(user);
        customer.setFirstName("Test");
        customer.setLastName("User");
        customer = customerRepository.save(customer);

        item = new InventoryItem();
        item.setItemId("TEST-ITEM-" + UUID.randomUUID());
        item.setItemName("Test Item");
        item.setAvailableStock(100);
        item.setReservedStock(0);
        item = inventoryItemRepository.save(item);
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void testRevenueCalculationFiltersOnlyDeliveredOrders() {
        LocalDateTime now = LocalDateTime.now();
        String monthName = now.getMonth().name();
        int year = now.getYear();

        // 1. Delivered order - should be included
        Orders deliveredOrder = createOrder(deliveredStatus, now);
        createOrderItem(deliveredOrder, item, 2, new BigDecimal("100.00"));

        // 2. Pending order - should be excluded
        Orders pendingOrder = createOrder(pendingStatus, now);
        createOrderItem(pendingOrder, item, 5, new BigDecimal("50.00"));

        // Execute
        MonthlySalesLogDTO report = adminAnalyticsService.getMonthlyReport(monthName, year);

        // Verify
        assertNotNull(report);
        assertEquals(2L, report.getTotalSoldItems(), "Should only count items from delivered orders");
        assertTrue(new BigDecimal("200.00").compareTo(report.getTotalRevenue()) == 0,
                "Expected 200.00 but got " + report.getTotalRevenue());

        assertNotNull(report.getItems());
        assertFalse(report.getItems().isEmpty(), "Item-wise report should not be empty");
        ItemSalesReportDTO itemReport = report.getItems().stream()
                .filter(i -> i.getItemId().equals(item.getItemId()))
                .findFirst()
                .orElseThrow();

        assertEquals(2L, itemReport.getTotalSoldItems());
        assertTrue(new BigDecimal("200.00").compareTo(itemReport.getTotalRevenue()) == 0);
    }

    private Orders createOrder(OrderStatusLookup status, LocalDateTime timestamp) {
        Orders order = new Orders();
        order.setOrderId(UUID.randomUUID().toString());
        order.setCustomer(customer);
        order.setStatus(status);
        order.setCreatedTimestamp(timestamp);
        return ordersRepository.save(order);
    }

    private void createOrderItem(Orders order, InventoryItem item, Integer quantity, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(new OrderItem.OrderItemId(order.getOrderId(), item.getItemId()));
        orderItem.setOrder(order);
        orderItem.setInventoryItem(item);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(price);
        orderItemRepository.save(orderItem);
    }
}
