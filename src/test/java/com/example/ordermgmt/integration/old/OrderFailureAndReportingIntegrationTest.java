package com.example.ordermgmt.integration.old;

import com.example.ordermgmt.dto.BulkOrderStatusUpdateDTO;
import com.example.ordermgmt.dto.BulkOrderStatusUpdateWrapperDTO;
import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.entity.PricingHistory;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.event.EmailDispatchEvent;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.OrderItemRepository;
import com.example.ordermgmt.repository.OrderStatusLookupRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
import com.example.ordermgmt.repository.OrdersRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
import com.example.ordermgmt.repository.PricingHistoryRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.security.TenantContextHolder;
import com.example.ordermgmt.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.CommonCachesTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@RecordApplicationEvents
@TestExecutionListeners({
        ServletTestExecutionListener.class,
        DirtiesContextBeforeModesTestExecutionListener.class,
        ApplicationEventsTestExecutionListener.class,
        BeanOverrideTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        WithSecurityContextTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        CommonCachesTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        SqlScriptsTestExecutionListener.class,
        EventPublishingTestExecutionListener.class
})
class OrderFailureAndReportingIntegrationTest {

    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final UUID ROOT_ORG_ID = TenantContextHolder.ROOT_TENANT_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PricingHistoryRepository pricingHistoryRepository;

    @Autowired
    private PricingCatalogRepository pricingCatalogRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private OrderStatusLookupRepository orderStatusLookupRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        orderItemRepository.deleteAll();
        ordersRepository.deleteAll();
        pricingHistoryRepository.deleteAll();
        pricingCatalogRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        customerRepository.deleteAll();
        appUserRepository.deleteAll();
        orderStatusLookupRepository.deleteAll();
        userRoleRepository.deleteAll();
        organizationRepository.deleteAll();

        ensureRootTenantOrganizationExists();
        seedCustomerRole();
        seedOrderStatuses();
    }

    @Test
    void sq43_insufficientStockOrder_isRejectedAndRolledBack() throws Exception {
        seedCustomer("low-stock@example.com", true);
        InventoryItem laptop = seedInventory("Laptop", 1, new BigDecimal("999.99"), true);

        createOrderExpectingBadRequest(
                "low-stock@example.com",
                List.of(new OrderItemDTO(laptop.getItemId(), null, 2, null, null)),
                "Insufficient stock for item: Laptop");

        assertThat(ordersRepository.count()).isZero();
        assertThat(orderItemRepository.count()).isZero();
        InventoryItem inventoryItem = inventoryItemRepository.findById(laptop.getItemId()).orElseThrow();
        assertThat(inventoryItem.getAvailableStock()).isEqualTo(1);
        assertThat(inventoryItem.getReservedStock()).isZero();
    }

    @Test
    void sq44_incompleteProfileOrder_isRejectedAndRolledBack() throws Exception {
        seedCustomer("incomplete@example.com", false);
        InventoryItem laptop = seedInventory("Laptop", 5, new BigDecimal("999.99"), true);

        createOrderExpectingBadRequest(
                "incomplete@example.com",
                List.of(new OrderItemDTO(laptop.getItemId(), null, 1, null, null)),
                "Customer profile incomplete");

        assertThat(ordersRepository.count()).isZero();
        assertThat(orderItemRepository.count()).isZero();
    }

    @Test
    void sq45_missingPriceOrder_isRejectedAndRolledBack() throws Exception {
        seedCustomer("missing-price@example.com", true);
        InventoryItem laptop = seedInventory("Laptop", 5, null, false);

        createOrderExpectingBadRequest(
                "missing-price@example.com",
                List.of(new OrderItemDTO(laptop.getItemId(), null, 1, null, null)),
                "Price not found for item ID");

        assertThat(ordersRepository.count()).isZero();
        assertThat(orderItemRepository.count()).isZero();
    }

    @Test
    void sq46_cancelPendingOrder_succeedsWithoutInventoryMutation() throws Exception {
        seedCustomer("cancel-pending@example.com", true);
        InventoryItem laptop = seedInventory("Laptop", 5, new BigDecimal("999.99"), true);
        OrderDTO order = createOrder(
                "cancel-pending@example.com",
                List.of(new OrderItemDTO(laptop.getItemId(), null, 1, null, null)));

        mockMvc.perform(put("/api/customer/orders/{orderId}/cancel", order.getOrderId())
                .with(customerUser("cancel-pending@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getOrderId().toString()))
                .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name()));

        Orders cancelled = ordersRepository.findById(order.getOrderId()).orElseThrow();
        InventoryItem inventoryItem = inventoryItemRepository.findById(laptop.getItemId()).orElseThrow();

        assertThat(cancelled.getStatus().getStatusName()).isEqualTo(OrderStatus.CANCELLED.name());
        assertThat(inventoryItem.getAvailableStock()).isEqualTo(5);
        assertThat(inventoryItem.getReservedStock()).isZero();

        EmailDispatchEvent statusEvent = applicationEvents.stream(EmailDispatchEvent.class)
                .filter(event -> "order-status".equals(event.templateName()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(statusEvent.recipientEmail()).isEqualTo("cancel-pending@example.com");
        assertThat(statusEvent.templateData()).containsEntry("status", OrderStatus.CANCELLED.name());
    }

    @Test
    void sq47_cancelNonPendingOrder_isRejectedAndLeavesReservationUntouched() throws Exception {
        seedCustomer("cancel-confirmed@example.com", true);
        InventoryItem laptop = seedInventory("Laptop", 5, new BigDecimal("999.99"), true);
        OrderDTO order = createOrder(
                "cancel-confirmed@example.com",
                List.of(new OrderItemDTO(laptop.getItemId(), null, 2, null, null)));

        updateOrderStatus(order.getOrderId(), OrderStatus.CONFIRMED.name());

        mockMvc.perform(put("/api/customer/orders/{orderId}/cancel", order.getOrderId())
                .with(customerUser("cancel-confirmed@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cancel order " + order.getOrderId()
                        + ". Current status: " + OrderStatus.CONFIRMED.name()));

        Orders confirmed = ordersRepository.findById(order.getOrderId()).orElseThrow();
        InventoryItem inventoryItem = inventoryItemRepository.findById(laptop.getItemId()).orElseThrow();

        assertThat(confirmed.getStatus().getStatusName()).isEqualTo(OrderStatus.CONFIRMED.name());
        assertThat(inventoryItem.getAvailableStock()).isEqualTo(3);
        assertThat(inventoryItem.getReservedStock()).isEqualTo(2);
    }

    @Test
    void sq48_bulkUpdateWithMixedValidity_returnsPartialSuccess() throws Exception {
        seedCustomer("bulk@example.com", true);
        InventoryItem laptop = seedInventory("Laptop", 5, new BigDecimal("999.99"), true);
        InventoryItem mouse = seedInventory("Mouse", 1, new BigDecimal("49.99"), true);

        OrderDTO successOrder = createOrder(
                "bulk@example.com",
                List.of(new OrderItemDTO(laptop.getItemId(), null, 1, null, null)));
        OrderDTO failureOrder = createOrder(
                "bulk@example.com",
                List.of(new OrderItemDTO(mouse.getItemId(), null, 1, null, null)));
        InventoryItem depleted = inventoryItemRepository.findById(mouse.getItemId()).orElseThrow();
        depleted.setAvailableStock(0);
        inventoryItemRepository.save(depleted);

        BulkOrderStatusUpdateWrapperDTO request = new BulkOrderStatusUpdateWrapperDTO(List.of(
                new BulkOrderStatusUpdateDTO(successOrder.getOrderId(), OrderStatus.CONFIRMED.name()),
                new BulkOrderStatusUpdateDTO(failureOrder.getOrderId(), OrderStatus.CONFIRMED.name())));

        mockMvc.perform(put("/api/admin/orders/status")
                .with(adminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successes[0].orderId").value(successOrder.getOrderId().toString()))
                .andExpect(jsonPath("$.successes[0].status").value(OrderStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.failures[0].orderId").value(failureOrder.getOrderId().toString()))
                .andExpect(jsonPath("$.failures[0].error")
                        .value(org.hamcrest.Matchers.containsString("Insufficient stock for item: Mouse")));

        assertThat(ordersRepository.findById(successOrder.getOrderId()).orElseThrow().getStatus().getStatusName())
                .isEqualTo(OrderStatus.CONFIRMED.name());
        assertThat(ordersRepository.findById(failureOrder.getOrderId()).orElseThrow().getStatus().getStatusName())
                .isEqualTo(OrderStatus.PENDING.name());
        assertThat(inventoryItemRepository.findById(laptop.getItemId()).orElseThrow().getReservedStock()).isEqualTo(1);
        assertThat(inventoryItemRepository.findById(mouse.getItemId()).orElseThrow().getReservedStock()).isZero();
    }

    @Test
    void sq50_revenueReport_returnsDeliveredSummaryWithPaginationAndEmailEvent() throws Exception {
        seedCustomer("report@example.com", true);
        InventoryItem laptop = seedInventory("Laptop", 10, new BigDecimal("999.99"), true);
        InventoryItem mouse = seedInventory("Mouse", 10, new BigDecimal("49.99"), true);

        OrderDTO laptopOrder = createDeliveredOrder("report@example.com", laptop.getItemId(), 2);
        OrderDTO mouseOrder = createDeliveredOrder("report@example.com", mouse.getItemId(), 1);
        setOrderCreatedTimestamp(laptopOrder.getOrderId(), LocalDateTime.of(2026, 3, 2, 14, 30));
        setOrderCreatedTimestamp(mouseOrder.getOrderId(), LocalDateTime.of(2026, 3, 5, 10, 0));

        mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                .with(orgAdminUser())
                .param("startdate", "2026-03-01")
                .param("enddate", "2026-03-09")
                .param("page", "0")
                .param("size", "1")
                .param("sendEmail", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.endDate").value("2026-03-09"))
                .andExpect(jsonPath("$.totalSoldItems").value(2))
                .andExpect(jsonPath("$.totalSoldQty").value(3))
                .andExpect(jsonPath("$.totalRevenue").value(2049.97))
                .andExpect(jsonPath("$.items[0].itemName").value("Laptop"))
                .andExpect(jsonPath("$.items[0].totalRevenue").value(1999.98))
                .andExpect(jsonPath("$.items[0].sales[0].soldQty").value(2));

        EmailDispatchEvent reportEvent = applicationEvents.stream(EmailDispatchEvent.class)
                .filter(event -> "report-email".equals(event.templateName()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(reportEvent.subject()).isEqualTo("Revenue Report");
        assertThat(reportEvent.recipientEmail()).isEqualTo("org-admin@example.com");
    }

    @Test
    void sq52_orderAnalytics_returnsFilteredStatusesAndItemMatches() throws Exception {
        seedCustomer("analytics@example.com", true);
        InventoryItem laptop = seedInventory("Laptop", 10, new BigDecimal("999.99"), true);

        OrderDTO deliveredOrder = createDeliveredOrder("analytics@example.com", laptop.getItemId(), 2);
        OrderDTO cancelledOrder = createOrder(
                "analytics@example.com",
                List.of(new OrderItemDTO(laptop.getItemId(), null, 1, null, null)));
        mockMvc.perform(put("/api/customer/orders/{orderId}/cancel", cancelledOrder.getOrderId())
                .with(customerUser("analytics@example.com")))
                .andExpect(status().isOk());

        setOrderCreatedTimestamp(deliveredOrder.getOrderId(), LocalDateTime.of(2026, 3, 2, 11, 0));
        setOrderCreatedTimestamp(cancelledOrder.getOrderId(), LocalDateTime.of(2026, 3, 9, 16, 55));

        mockMvc.perform(get("/api/org-admin/analytics/order-analytics")
                .with(orgAdminUser())
                .param("startdate", "2026-03-01")
                .param("enddate", "2026-03-10")
                .param("itemname", "laptop")
                .param("orderstatus", "delivered, canceled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.endDate").value("2026-03-10"))
                .andExpect(jsonPath("$.totalSoldItems").value(1))
                .andExpect(jsonPath("$.totalSoldQty").value(3))
                .andExpect(jsonPath("$.items[0].itemName").value("Laptop"))
                .andExpect(jsonPath("$.items[0].sales[0].orderStatus").value("DELIVERED"))
                .andExpect(jsonPath("$.items[0].sales[1].orderStatus").value("CANCELLED"));
    }

    @Test
    void sq53_invalidAnalyticsFilters_areRejected() throws Exception {
        mockMvc.perform(get("/api/org-admin/analytics/revenue-report")
                .with(orgAdminUser())
                .param("startdate", "2026-03-09")
                .param("enddate", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startDate must be before endDate"));

        mockMvc.perform(get("/api/org-admin/analytics/order-analytics")
                .with(orgAdminUser())
                .param("startdate", "2026-03-01")
                .param("enddate", "2026-03-10")
                .param("orderstatus", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid orderStatus: BOGUS"));
    }

    private void createOrderExpectingBadRequest(String email, List<OrderItemDTO> items, String expectedMessagePart)
            throws Exception {
        OrderDTO request = new OrderDTO(null, null, null, null, null, items, null);

        mockMvc.perform(post("/api/customer/orders")
                .with(customerUser(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(expectedMessagePart)));
    }

    private OrderDTO createOrder(String email, List<OrderItemDTO> items) throws Exception {
        OrderDTO request = new OrderDTO(null, null, null, null, null, items, null);

        MvcResult result = mockMvc.perform(post("/api/customer/orders")
                .with(customerUser(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), OrderDTO.class);
    }

    private OrderDTO createDeliveredOrder(String email, UUID itemId, int quantity) throws Exception {
        OrderDTO order = createOrder(email, List.of(new OrderItemDTO(itemId, null, quantity, null, null)));
        updateOrderStatus(order.getOrderId(), OrderStatus.CONFIRMED.name());
        updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING.name());
        updateOrderStatus(order.getOrderId(), OrderStatus.SHIPPED.name());
        updateOrderStatus(order.getOrderId(), OrderStatus.DELIVERED.name());
        return order;
    }

    private void updateOrderStatus(UUID orderId, String newStatus) throws Exception {
        BulkOrderStatusUpdateWrapperDTO request = new BulkOrderStatusUpdateWrapperDTO(
                List.of(new BulkOrderStatusUpdateDTO(orderId, newStatus)));

        mockMvc.perform(put("/api/admin/orders/status")
                .with(adminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void setOrderCreatedTimestamp(UUID orderId, LocalDateTime createdTimestamp) {
        jdbcTemplate.update(
                "UPDATE ordermgmt.orders SET createdtimestamp = ? WHERE orderid = ?",
                Timestamp.valueOf(createdTimestamp),
                orderId);
    }

    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor customerUser(String email) {
        return SecurityMockMvcRequestPostProcessors.user(email)
                .authorities(new SimpleGrantedAuthority(ROLE_CUSTOMER));
    }

    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor adminUser() {
        return SecurityMockMvcRequestPostProcessors.user("admin@example.com")
                .authorities(new SimpleGrantedAuthority("ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor orgAdminUser() {
        return SecurityMockMvcRequestPostProcessors.user("org-admin@example.com")
                .authorities(new SimpleGrantedAuthority("ORG_ADMIN"));
    }

    private void seedCustomer(String email, boolean completeProfile) {
        UserRole role = userRoleRepository.findByRoleName(ROLE_CUSTOMER).orElseThrow();

        TenantContextHolder.setTenantId(ROOT_ORG_ID);
        try {
            AppUser user = new AppUser();
            user.setEmail(email);
            user.setPasswordHash("noop");
            user.setRole(role);
            user.setOrgId(ROOT_ORG_ID);
            user.setIsActive(true);
            user.setIsPasswordChanged(true);
            AppUser savedUser = appUserRepository.save(user);

            Customer customer = new Customer();
            customer.setAppUser(savedUser);
            customer.setOrgId(ROOT_ORG_ID);
            customer.setFirstName("Jane");
            customer.setLastName("Customer");
            customer.setContactNo("9999999999");
            if (completeProfile) {
                customer.setAddress("123 Integration Street");
            }
            customerRepository.save(customer);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private InventoryItem seedInventory(String itemName, int availableStock, BigDecimal unitPrice,
            boolean withPricing) {
        TenantContextHolder.setTenantId(ROOT_ORG_ID);
        try {
            InventoryItem inventoryItem = new InventoryItem();
            inventoryItem.setItemName(itemName);
            inventoryItem.setAvailableStock(availableStock);
            inventoryItem.setReservedStock(0);

            if (withPricing && unitPrice != null) {
                PricingCatalog pricingCatalog = new PricingCatalog();
                pricingCatalog.setInventoryItem(inventoryItem);
                pricingCatalog.setUnitPrice(unitPrice);
                inventoryItem.setPricingCatalog(pricingCatalog);

                PricingHistory pricingHistory = new PricingHistory();
                pricingHistory.setInventoryItem(inventoryItem);
                pricingHistory.setOldPrice(null);
                pricingHistory.setNewPrice(unitPrice);
                inventoryItem.setPricingHistoryLogs(List.of(pricingHistory));
            }

            return inventoryItemRepository.save(inventoryItem);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void ensureRootTenantOrganizationExists() {
        if (organizationRepository.existsById(ROOT_ORG_ID)) {
            return;
        }

        Organization organization = new Organization();
        organization.setOrgId(ROOT_ORG_ID);
        organization.setName("Root Test Org");
        organization.setSubdomain("root-test");
        organization.setDescription("Seeded for order failure and reporting integration tests");
        organization.setIsActive(true);
        organizationRepository.save(organization);
    }

    private void seedCustomerRole() {
        UserRole role = new UserRole();
        role.setRoleId(1);
        role.setRoleName(ROLE_CUSTOMER);
        userRoleRepository.save(role);
    }

    private void seedOrderStatuses() {
        List<Map.Entry<Integer, OrderStatus>> statuses = List.of(
                Map.entry(1, OrderStatus.PENDING),
                Map.entry(2, OrderStatus.CONFIRMED),
                Map.entry(3, OrderStatus.PROCESSING),
                Map.entry(4, OrderStatus.SHIPPED),
                Map.entry(5, OrderStatus.DELIVERED),
                Map.entry(6, OrderStatus.CANCELLED));

        for (Map.Entry<Integer, OrderStatus> entry : statuses) {
            OrderStatusLookup status = new OrderStatusLookup();
            status.setStatusId(entry.getKey());
            status.setStatusName(entry.getValue().name());
            orderStatusLookupRepository.save(status);
        }
    }

    @TestConfiguration
    static class OrderFailureAndReportingTestConfig {

        @Bean(name = "taskExecutor")
        Executor taskExecutor() {
            return Runnable::run;
        }

        @Bean
        @Primary
        EmailService emailService() {
            return (to, subject, body) -> {
            };
        }
    }
}
