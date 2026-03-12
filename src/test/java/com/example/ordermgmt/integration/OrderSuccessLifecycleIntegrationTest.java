package com.example.ordermgmt.integration;

import com.example.ordermgmt.dto.BulkOrderStatusUpdateDTO;
import com.example.ordermgmt.dto.BulkOrderStatusUpdateWrapperDTO;
import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.OrderItem;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class OrderSuccessLifecycleIntegrationTest {

    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String CUSTOMER_EMAIL = "customer@example.com";
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
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private PricingCatalogRepository pricingCatalogRepository;

    @Autowired
    private PricingHistoryRepository pricingHistoryRepository;

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
    void sq33_sq34_createOrder_persistsPendingOrderItemsAndPublishesReceiptEvent() throws Exception {
        seedCustomer(CUSTOMER_EMAIL);
        InventoryItem laptop = seedInventoryWithPricing("Laptop", 10, new BigDecimal("999.99"));

        OrderDTO createdOrder = createOrder(CUSTOMER_EMAIL, List.of(new OrderItemDTO(laptop.getItemId(), null, 2, null, null)));

        Orders savedOrder = ordersRepository.findById(createdOrder.getOrderId()).orElseThrow();
        List<OrderItem> savedItems = orderItemRepository.findByOrderOrderId(createdOrder.getOrderId());
        InventoryItem unchangedInventory = inventoryItemRepository.findById(laptop.getItemId()).orElseThrow();

        assertThat(savedOrder.getStatus().getStatusName()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(savedItems).hasSize(1);
        assertThat(savedItems.get(0).getQuantity()).isEqualTo(2);
        assertThat(savedItems.get(0).getUnitPrice()).isEqualByComparingTo("999.99");
        assertThat(createdOrder.getTotalAmount()).isEqualByComparingTo("1999.98");
        assertThat(unchangedInventory.getAvailableStock()).isEqualTo(10);
        assertThat(unchangedInventory.getReservedStock()).isEqualTo(0);

        EmailDispatchEvent receiptEvent = applicationEvents.stream(EmailDispatchEvent.class)
                .filter(event -> "order-receipt".equals(event.templateName()))
                .reduce((first, second) -> second)
                .orElseThrow();

        assertThat(receiptEvent.recipientEmail()).isEqualTo(CUSTOMER_EMAIL);
        assertThat(receiptEvent.subject()).contains(createdOrder.getOrderId().toString());
        assertThat(receiptEvent.templateData()).containsEntry("summary", "Your order has been placed successfully.");
    }

    @Test
    void sq35_sq36_customerCanListOwnOrders_byListByIdAndPage() throws Exception {
        seedCustomer(CUSTOMER_EMAIL);
        InventoryItem laptop = seedInventoryWithPricing("Laptop", 10, new BigDecimal("999.99"));
        OrderDTO createdOrder = createOrder(CUSTOMER_EMAIL, List.of(new OrderItemDTO(laptop.getItemId(), null, 1, null, null)));

        mockMvc.perform(get("/api/customer/orders")
                        .with(customerUser(CUSTOMER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].orderId").value(createdOrder.getOrderId().toString()))
                .andExpect(jsonPath("$.orders[0].status").value(OrderStatus.PENDING.name()))
                .andExpect(jsonPath("$.orders[0].items[0].itemName").value("Laptop"));

        mockMvc.perform(get("/api/customer/orders")
                        .with(customerUser(CUSTOMER_EMAIL))
                        .param("orderId", createdOrder.getOrderId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].orderId").value(createdOrder.getOrderId().toString()))
                .andExpect(jsonPath("$.orders[0].status").value(OrderStatus.PENDING.name()));

        mockMvc.perform(get("/api/customer/orders")
                        .with(customerUser(CUSTOMER_EMAIL))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(createdOrder.getOrderId().toString()))
                .andExpect(jsonPath("$.content[0].status").value(OrderStatus.PENDING.name()));
    }

    @Test
    void sq37_adminCanListAllOrders_byListByIdAndPage() throws Exception {
        seedCustomer(CUSTOMER_EMAIL);
        InventoryItem laptop = seedInventoryWithPricing("Laptop", 10, new BigDecimal("999.99"));
        OrderDTO createdOrder = createOrder(CUSTOMER_EMAIL, List.of(new OrderItemDTO(laptop.getItemId(), null, 1, null, null)));

        mockMvc.perform(get("/api/admin/orders")
                        .with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].orderId").value(createdOrder.getOrderId().toString()))
                .andExpect(jsonPath("$.orders[0].status").value(OrderStatus.PENDING.name()));

        mockMvc.perform(get("/api/admin/orders")
                        .with(adminUser())
                        .param("orderId", createdOrder.getOrderId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].orderId").value(createdOrder.getOrderId().toString()))
                .andExpect(jsonPath("$.orders[0].status").value(OrderStatus.PENDING.name()));

        mockMvc.perform(get("/api/admin/orders")
                        .with(adminUser())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(createdOrder.getOrderId().toString()))
                .andExpect(jsonPath("$.content[0].status").value(OrderStatus.PENDING.name()));
    }

    @Test
    void sq38_sq41_bulkAdminTransitions_moveOrderFromPendingToDeliveredAndAdjustInventory() throws Exception {
        seedCustomer(CUSTOMER_EMAIL);
        InventoryItem laptop = seedInventoryWithPricing("Laptop", 10, new BigDecimal("999.99"));
        OrderDTO createdOrder = createOrder(CUSTOMER_EMAIL, List.of(new OrderItemDTO(laptop.getItemId(), null, 3, null, null)));

        updateOrderStatus(createdOrder.getOrderId(), OrderStatus.CONFIRMED.name());
        assertOrderAndInventoryState(createdOrder.getOrderId(), laptop.getItemId(), OrderStatus.CONFIRMED, 7, 3);

        updateOrderStatus(createdOrder.getOrderId(), OrderStatus.PROCESSING.name());
        assertOrderAndInventoryState(createdOrder.getOrderId(), laptop.getItemId(), OrderStatus.PROCESSING, 7, 3);

        updateOrderStatus(createdOrder.getOrderId(), OrderStatus.SHIPPED.name());
        assertOrderAndInventoryState(createdOrder.getOrderId(), laptop.getItemId(), OrderStatus.SHIPPED, 7, 3);

        updateOrderStatus(createdOrder.getOrderId(), OrderStatus.DELIVERED.name());
        assertOrderAndInventoryState(createdOrder.getOrderId(), laptop.getItemId(), OrderStatus.DELIVERED, 7, 0);

        List<EmailDispatchEvent> statusEvents = applicationEvents.stream(EmailDispatchEvent.class)
                .filter(event -> "order-status".equals(event.templateName()))
                .toList();

        assertThat(statusEvents).hasSize(4);
        assertThat(statusEvents)
                .extracting(EmailDispatchEvent::recipientEmail)
                .containsOnly(CUSTOMER_EMAIL);
        assertThat(statusEvents.stream()
                .map(event -> (String) event.templateData().get("status"))
                .toList()).containsExactly("CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED");
    }

    private void assertOrderAndInventoryState(
            UUID orderId,
            UUID itemId,
            OrderStatus expectedStatus,
            int expectedAvailableStock,
            int expectedReservedStock) {
        Orders order = ordersRepository.findById(orderId).orElseThrow();
        InventoryItem inventoryItem = inventoryItemRepository.findById(itemId).orElseThrow();

        assertThat(order.getStatus().getStatusName()).isEqualTo(expectedStatus.name());
        assertThat(inventoryItem.getAvailableStock()).isEqualTo(expectedAvailableStock);
        assertThat(inventoryItem.getReservedStock()).isEqualTo(expectedReservedStock);
    }

    private void updateOrderStatus(UUID orderId, String newStatus) throws Exception {
        BulkOrderStatusUpdateWrapperDTO request = new BulkOrderStatusUpdateWrapperDTO(List.of(
                new BulkOrderStatusUpdateDTO(orderId, newStatus)));

        mockMvc.perform(put("/api/admin/orders/status")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successes[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.successes[0].status").value(newStatus))
                .andExpect(jsonPath("$.failures").isArray());
    }

    private OrderDTO createOrder(String customerEmail, List<OrderItemDTO> items) throws Exception {
        OrderDTO request = new OrderDTO(null, null, null, null, null, items, null);

        MvcResult result = mockMvc.perform(post("/api/customer/orders")
                        .with(customerUser(customerEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.name()))
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), OrderDTO.class);
    }

    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor customerUser(String email) {
        return SecurityMockMvcRequestPostProcessors.user(email)
                .authorities(new SimpleGrantedAuthority(ROLE_CUSTOMER));
    }

    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor adminUser() {
        return SecurityMockMvcRequestPostProcessors.user("admin@example.com")
                .authorities(new SimpleGrantedAuthority("ADMIN"));
    }

    private void seedCustomer(String email) {
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
            customer.setAddress("123 Integration Street");
            customer.setContactNo("9999999999");
            customerRepository.save(customer);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private InventoryItem seedInventoryWithPricing(String itemName, int availableStock, BigDecimal unitPrice) {
        TenantContextHolder.setTenantId(ROOT_ORG_ID);
        try {
            InventoryItem inventoryItem = new InventoryItem();
            inventoryItem.setItemName(itemName);
            inventoryItem.setAvailableStock(availableStock);
            inventoryItem.setReservedStock(0);

            PricingCatalog pricingCatalog = new PricingCatalog();
            pricingCatalog.setInventoryItem(inventoryItem);
            pricingCatalog.setUnitPrice(unitPrice);
            inventoryItem.setPricingCatalog(pricingCatalog);

            PricingHistory pricingHistory = new PricingHistory();
            pricingHistory.setInventoryItem(inventoryItem);
            pricingHistory.setOldPrice(null);
            pricingHistory.setNewPrice(unitPrice);
            inventoryItem.setPricingHistoryLogs(List.of(pricingHistory));

            InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);
            return inventoryItemRepository.findById(savedItem.getItemId()).orElseThrow();
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
        organization.setDescription("Seeded for order success integration tests");
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
    static class OrderSuccessTestConfig {

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
